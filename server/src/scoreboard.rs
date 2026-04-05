use crate::error::{Error, Result, ScoreboardError};
use axum::Router;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::net::SocketAddr;
use std::ops::ControlFlow;
use std::sync::Arc;
use tokio::sync::{Mutex, RwLock};
use axum::extract::{ConnectInfo, State, WebSocketUpgrade};
use axum::extract::ws::{Message, Utf8Bytes, WebSocket};
use axum::response::IntoResponse;
use axum::routing::any;
use axum_extra::TypedHeader;
use futures_util::{SinkExt, StreamExt};
use futures_util::stream::SplitSink;
use tungstenite::client::IntoClientRequest;
use crate::game::GameListener;

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "PascalCase")]
struct Game {
    abort_reason: String,
    direction: bool,
}

#[derive(Eq, PartialEq, Hash, Serialize, Deserialize)]
enum VersionKey {
    Release,
    Commit,
    Host,
    Time,
    User
}

impl From<VersionKey> for String {
    fn from(value: VersionKey) -> Self {
        match value {
            VersionKey::Release => "release",
            VersionKey::Commit => "release.commit",
            VersionKey::Host => "release.host",
            VersionKey::Time => "release.time",
            VersionKey::User => "release.user",
        }.into()
    }
}

impl TryFrom<String> for VersionKey {
    type Error = Error;

    fn try_from(value: String) -> Result<VersionKey> {
        Ok(match value.as_str() {
            "release" => VersionKey::Release,
            "release.commit" => VersionKey::Commit,
            "release.host" => VersionKey::Host,
            "release.time" => VersionKey::Time,
            "release.user" => VersionKey::User,
            _ => return Err(Error::Scoreboard(ScoreboardError::InvalidVersionKey(value))),
        })
    }
}



#[derive(Default, Serialize, Deserialize)]
struct ScoreboardState {
    game: HashMap<String, Game>,
    version: HashMap<VersionKey, String>,
}

impl ScoreboardState {
    pub fn new() -> Self {
        Self {
            ..Default::default()
        }
    }
}

pub struct ScoreboardRouterState {
    pub host: Arc<RwLock<Option<String>>>,
    pub scoreboard: Arc<Mutex<ScoreboardState>>,
}

impl ScoreboardRouterState {
    pub fn new() -> Self {
        Self {
            host: Default::default(),
            scoreboard: Default::default(),
        }
    }
}

pub async fn scoreboard() -> Router {
    let state = Arc::new(ScoreboardRouterState::new());

    let (listener, tx, bus) = GameListener::new(state.clone());

    Router::new()
        .route("/ws", any(ws_handler))
        .with_state(state)
}

async fn ws_handler(
    State(shared_state): State<Arc<ScoreboardRouterState>>,
    ws: WebSocketUpgrade,
    user_agent: Option<TypedHeader<headers::UserAgent>>,
    ConnectInfo(addr): ConnectInfo<SocketAddr>,
) -> impl IntoResponse {
    let user_agent = if let Some(TypedHeader(user_agent)) = user_agent {
        user_agent.to_string()
    } else {
        String::from("Unknown browser")
    };
    log::info!("`{}` at {} connected.", user_agent, addr);
    // finalize the upgrade process by returning upgrade callback.
    // we can customize the callback by sending additional info such as address.
    ws.on_upgrade(move |socket| handle_socket(socket, addr, shared_state.clone()))
}

#[derive(Debug, Deserialize, Serialize, Clone)]
enum ScoreboardSocketMessage {
    NewListener
}

async fn send_message(sender: &mut SplitSink<WebSocket, Message>, message: ScoreboardSocketMessage) {
    if sender.send(Message::Text(
        Utf8Bytes::from(serde_json::to_string(&message).unwrap())
    )).await.is_err() {
        log::warn!("Unable to send ScoreboardSocketMessage");
    }
}

async fn handle_socket(mut socket: WebSocket, who: SocketAddr, router_state: Arc<ScoreboardRouterState>) {
    if socket
        .send(Message::Ping(axum::body::Bytes::from_static(&[1, 2, 3])))
        .await
        .is_ok()
    {
        log::debug!("Pinged {who}...");
    } else {
        log::warn!("Could not ping {who}!");
        return;
    }

    if let Some(msg) = socket.recv().await {
        if let Ok(msg) = msg {
            if process_message(msg, who).await.is_break() {
                return;
            }
        } else {
            log::warn!("client {who} abruptly disconnected");
            return;
        }
    }

    // By splitting socket we can send and receive at the same time. In this example we will send
    // unsolicited messages to client based on some sort of server's internal event (i.e .timer).
    let (mut sender, mut receiver) = socket.split();

    let cloned_shared_state = router_state.clone();
    // let mut ndi_bus = router_state.new_ndi_message_bus_recv();
    // let mut stream_bus = router_state.new_stream_message_bus_recv();
    // Spawn a task that will push several messages to the client (does not matter what client does)
    let mut send_task = tokio::spawn(async move {
        loop {
            // while let Ok(action) = stream_bus.try_recv() {
            //     log::info!("{:?}", action);
            //     match action {
            //         StreamChannelMessage::Running(is_running) => {
            //             let message = NdiSocketMessage::Running { is_running };
            //             send_message(&mut sender, message).await;
            //         }
            //         StreamChannelMessage::Ptz(is_supported) => {
            //             let message = NdiSocketMessage::Ptz { is_supported };
            //             send_message(&mut sender, message).await;
            //         }
            //         _ => {}
            //     }
            // }
            // while let Ok(action) = ndi_bus.try_recv() {
            //     match action.clone() {
            //         NdiSocketMessage::Sources { .. } |
            //         NdiSocketMessage::SelectedSource { .. } => {
            //             send_message(&mut sender, action).await;
            //         }
            //         _ => {}
            //     }
            // }

            log::trace!("Sending ping");
            if sender
                .send(Message::Ping(axum::body::Bytes::from_static(&[1, 2, 3])))
                .await
                .is_err()
            {
                log::warn!("Error sending ping");
                return "Error sending ping";
            }

            tokio::time::sleep(std::time::Duration::from_millis(500)).await;
        }
    });

    let state = router_state.clone();
    // let stx = router_state.stx.clone();
    let mut recv_task = tokio::spawn(async move {
        let mut cnt = 0;
        while let Some(Ok(msg)) = receiver.next().await {
            cnt += 1;
            if process_message(msg, who).await.is_break() {
                break;
            }
        }
        cnt
    });

    // if router_state.stx.clone().send(ScoreboardSocketMessage::NewListener).is_err() {
    //     log::info!("Could not send NewListener message");
    // }

    tokio::select! {
        rv_a = (&mut send_task) => {
            match rv_a {
                Ok(a) => log::debug!("{a} messages sent to {who}"),
                Err(a) => log::warn!("Error sending messages {a:?}")
            }
            recv_task.abort();
        },
        rv_b = (&mut recv_task) => {
            match rv_b {
                Ok(b) => log::debug!("Received {b} messages"),
                Err(b) => log::warn!("Error receiving messages {b:?}")
            }
            send_task.abort();
        }
    }

    log::debug!("Websocket context {who} destroyed");
}

async fn process_message(msg: Message, who: SocketAddr) -> ControlFlow<(), ()> {
    match msg {
        Message::Text(t) => {
            // match serde_json::from_str::<ScoreboardSocketMessage>(t.as_str()) {
                // Ok(message) => match message {
                //     
                // }
                // Err(e) => {
                //     log::warn!("Cannot convert from string {} :: {}", t, e);
                // }
            // }
        }
        Message::Binary(d) => {
            log::info!(">>> {} sent {} bytes: {:?}", who, d.len(), d);
        }
        Message::Close(c) => {
            if let Some(cf) = c {
                log::info!(
                    ">>> {} sent close with code {} and reason `{}`",
                    who, cf.code, cf.reason
                );
            } else {
                log::info!(">>> {who} somehow sent close message without CloseFrame");
            }
            return ControlFlow::Break(());
        }

        Message::Pong(v) => {
            log::trace!(">>> {who} sent pong with {v:?}");
        }
        // You should never need to manually handle Message::Ping, as axum's websocket library
        // will do so for you automagically by replying with Pong and copying the v according to
        // spec. But if you need the contents of the pings you can see them here.
        Message::Ping(v) => {
            log::info!(">>> {who} sent ping with {v:?}");
        }
    }
    ControlFlow::Continue(())
}

fn init_thread(state: Arc<ScoreboardRouterState>, ) {
    let state = state.clone();
    tokio::spawn(async move {
        // thread_state;
        let mut request = "ws://".into_client_request().unwrap();
        match tokio_tungstenite::connect_async(request).await {
            Ok(r) => {

            },
            Err(e) => {
                log::error!("{}", e);
            }
        }
    });
}
