use crate::stream::{NdiStream, PtzAction, StreamChannelMessage};
use std::sync::{Arc, Mutex};
use axum::body::Body;
use axum::extract::{Json, Path, State, };
use axum::extract::ws::{Message, Utf8Bytes, WebSocket, WebSocketUpgrade};
use axum::http::{StatusCode};
use axum::response::IntoResponse;
use axum::routing::{any, get, post};
use axum::Router;
use axum_extra::TypedHeader;
use ndi_bindings::{Find, Finder, Receiver, RecvBandwidth, RecvColorFormat, Source, NDI};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::str::FromStr;
use std::sync::RwLock;
use std::ops::ControlFlow;
use std::{net::SocketAddr, path::PathBuf};
use std::io::Bytes;
//allows to extract the IP of connecting user
use axum::extract::connect_info::ConnectInfo;
use axum::extract::ws::CloseFrame;

//allows to split the websocket stream into separate TX and RX branches
use futures::{sink::SinkExt, stream::StreamExt};

fn get_ndi_sources(
    _ndi: &NDI,
    timeout: u32,
    groups: Option<&str>,
    extra_ips: Option<&str>,
) -> Result<Vec<Source>, ndi_bindings::Error> {
    let settings = Finder::new(true, groups, extra_ips);
    log::info!("Finder instance created");
    let find = Find::new(_ndi, settings)?;
    log::info!("Find instance created");
    let sources = find.get_sources(timeout)?;
    log::info!("{} source(s) found", sources.len());
    sources.iter().for_each(|source| {
        log::info!(
            "{}: [{:?}, {:?}]",
            source.name,
            source.ip_address,
            source.url_address
        );
    });
    Ok(sources)
}

pub(crate) async fn get_sources_async(shared_state: Arc<SharedState>) {
    match NDI::new() {
        Ok(ndi) => {
            let state = shared_state.clone();
            let mut state = state.ndi_data.lock().expect("Unable to lock ndi_data");

            state.sources = match get_ndi_sources(&ndi, 5000, state.groups, state.extra_ips) {
                Ok(s) => Some(s),
                Err(e) => {
                    log::error!("{}", e);
                    None
                }
            }
        }
        Err(e) => {
            log::error!("{}", e);
        }
    }
}

async fn source_selected(shared_state: Arc<SharedState>) {
    let data = shared_state
        .ndi_data
        .lock()
        .expect("Unable to lock ndi_data");
    let mut receiver = shared_state
        .receiver
        .write()
        .expect("Unable to write to shared receiver");

    if let Some(source) = &data.selected_source {
        let mut stream = shared_state.stream.write().expect("Unable to write to shared stream");
        let receiver_instance = Receiver::new(
            source.clone(),
            RecvColorFormat::Best,
            RecvBandwidth::Highest,
            true,
            Some("Example receiver".to_string()),
        );
        stream.set_receiver(receiver_instance.clone());
        stream.start();
        *receiver = Some(receiver_instance);
    } else {
        *receiver = None;
    }
}

#[derive(Default)]
pub struct NDIData {
    pub sources: Option<Vec<Source>>,
    pub selected_source: Option<Source>,
    pub groups: Option<&'static str>,
    pub extra_ips: Option<&'static str>,
}

#[derive(Serialize, Deserialize)]
pub(crate) enum StreamMessage {
    NewVideoFrame(Vec<u8>),
}

unsafe impl Send for StreamMessage {}
unsafe impl Sync for StreamMessage {}

pub(crate) struct SharedState {
    pub ndi_data: Mutex<NDIData>,
    pub receiver: RwLock<Option<Receiver>>,
    pub stream: Arc<RwLock<NdiStream>>,
    pub stx: crossbeam_channel::Sender<StreamChannelMessage>,
    pub rx: crossbeam_channel::Receiver<StreamChannelMessage>,
}

unsafe impl Send for SharedState {}
unsafe impl Sync for SharedState {}

impl SharedState {
    fn new() -> Self {
        let groups = option_env!("NDI_GROUPS");
        log::info!("[env-provided] NDI Groups: {:?}", groups);
        let extra_ips = option_env!("NDI_EXTRA_IPS");
        log::info!("[env-provided] NDI Extra IPs: {:?}", extra_ips);

        let (stream, tx, rx) = NdiStream::new();

        Self {
            ndi_data: Mutex::new(NDIData {
                groups,
                extra_ips,
                ..Default::default()
            }),
            receiver: RwLock::new(None),
            stream: Arc::new(RwLock::new(stream)),
            stx: tx,
            rx,
        }
    }
}

/// Router
pub async fn ndi_router() -> Router {
    log::info!("Initializing state");
    let shared_state: Arc<SharedState> = Arc::new(SharedState::new());

    log::info!("Getting sources off-thread");
    tokio::spawn(get_sources_async(shared_state.clone()));

    Router::new()
        .route("/refresh", get(refresh_sources))
        .route("/sources", get(get_sources))
        .route("/running", get(is_running))
        .route("/select-source/{name}", get(select_source))
        .route("/ptz", get(ptz).post(ptz_control))
        .route("/stop", get(stop_stream))
        .route("/ws", any(ws_handler))
        .with_state(shared_state)
}

/// Route handlers
async fn refresh_sources(State(shared_state): State<Arc<SharedState>>) -> String {
    get_sources_async(shared_state).await;
    serde_json::json!(true).to_string()
}

async fn get_sources(State(shared_state): State<Arc<SharedState>>) -> String {
    let data = shared_state
        .ndi_data
        .lock()
        .expect("Unable to lock ndi_data");
    let names = data
        .sources
        .clone()
        .map(|srcs| srcs.iter().map(|s| s.name.clone()).collect::<Vec<String>>());
    serde_json::json!({
        "sources": names
    })
    .to_string()
}

async fn select_source(
    Path(name): Path<String>,
    State(shared_state): State<Arc<SharedState>>,
) -> String {
    let mut data = shared_state
        .ndi_data
        .lock()
        .expect("Unable to lock ndi_data");
    for source in data.sources.clone().expect("No sources found") {
        if source.name == name {
            data.selected_source = Some(source);
            log::info!("Selected {}", name);
            tokio::spawn(source_selected(shared_state.clone()));
            return format!("Selected source {}", name);
        }
    }
    format!("Could not find source {}", name)
}

async fn is_running(
    State(shared_state): State<Arc<SharedState>>,
) -> impl IntoResponse {
    let stream = shared_state.stream.read().expect("Unable to read stream");
    (StatusCode::OK, serde_json::json!(stream.running()).to_string())
}

async fn ptz(
    State(shared_state): State<Arc<SharedState>>,
) -> impl IntoResponse {
    let stream = shared_state.stream.read().expect("Unable to read stream");
    let state = stream.stream_state.read().expect("Unable to read stream state");
    (StatusCode::OK, serde_json::json!(state.ptz).to_string())
}

async fn ptz_control(
    State(shared_state): State<Arc<SharedState>>,
    Json(actions): Json<Vec<PtzAction>>,
) -> impl IntoResponse {
    let tx = shared_state.stx.clone();
    let mut result = serde_json::Map::new();
    for action in actions {
        match tx.send(StreamChannelMessage::PtzAction(action)) {
            Ok(_) => {},
            Err(e) => {
                result.insert(action.to_string(), Value::from_str(&e.to_string()).unwrap_or_default());
            }
        }
    }
    let result = serde_json::to_string(&result).unwrap_or("Failed to unwrap result JSON map".into());
    (StatusCode::OK, Body::from(result))
}

async fn stop_stream(State(shared_state): State<Arc<SharedState>>) {
    log::info!("Received request to stop stream");
    let stream = shared_state.stream.read().expect("Unable to write to stream");
    stream.stop();
}

/// The handler for the HTTP request (this gets called when the HTTP request lands at the start
/// of websocket negotiation). After this completes, the actual switching from HTTP to
/// websocket protocol will occur.
/// This is the last point where we can extract TCP/IP metadata such as IP address of the client
/// as well as things from HTTP headers such as user-agent of the browser etc.
async fn ws_handler(
    State(shared_state): State<Arc<SharedState>>,
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

#[derive(Debug, Deserialize, Serialize)]
enum NdiSocketMessage {
    Actions { actions: Vec<PtzAction> },
    Ptz { is_supported: bool },
    Running { is_running: bool },
}

/// Actual websocket statemachine (one will be spawned per connection)
async fn handle_socket(mut socket: WebSocket, who: SocketAddr, shared_state: Arc<SharedState>) {
    // send a ping (unsupported by some browsers) just to kick things off and get a response
    if socket
        .send(Message::Ping(axum::body::Bytes::from_static(&[1, 2, 3])))
        .await
        .is_ok()
    {
        log::debug!("Pinged {who}...");
    } else {
        log::warn!("Could not ping {who}!");
        // no Error here since the only thing we can do is to close the connection.
        // If we can not send messages, there is no way to salvage the statemachine anyway.
        return;
    }

    // receive single message from a client (we can either receive or send with socket).
    // this will likely be the Pong for our Ping or a hello message from client.
    // waiting for message from a client will block this task, but will not block other client's
    // connections.
    if let Some(msg) = socket.recv().await {
        if let Ok(msg) = msg {
            if process_message(msg, who, shared_state.stx.clone()).is_break() {
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

    let srx = shared_state.rx.clone();
    // Spawn a task that will push several messages to the client (does not matter what client does)
    let mut send_task = tokio::spawn(async move {
        loop {
            while let Ok(action) = srx.try_recv() {
                log::info!("{:?}", action);
                match action {
                    StreamChannelMessage::Running(is_running) => {
                        let message = NdiSocketMessage::Running { is_running };
                        if sender.send(Message::Text(
                            Utf8Bytes::from(serde_json::to_string(&message).unwrap())
                        )).await.is_err() {
                            log::warn!("Unable to send NdiSocketMessage::Ptz");
                        }
                    }
                    StreamChannelMessage::Ptz(is_supported) => {
                        let message = NdiSocketMessage::Ptz { is_supported };
                        if sender.send(Message::Text(
                            Utf8Bytes::from(serde_json::to_string(&message).unwrap())
                        )).await.is_err() {
                            log::warn!("Unable to send NdiSocketMessage::Ptz");
                        }
                    }
                    _ => {}
                }
            }

            // In case of any websocket error, we exit.
            log::trace!("Sending ping");
            if sender
                .send(Message::Ping(axum::body::Bytes::from_static(&[1, 2, 3])))
                // .send(Message::Text(format!("Server message {i} ...").into()))
                .await
                .is_err()
            {
                log::warn!("Error sending ping");
                return "Error sending ping";
            }

            tokio::time::sleep(std::time::Duration::from_millis(500)).await;
        }
    });

    let stx = shared_state.stx.clone();
    // This second task will receive messages from client and print them on server console
    let mut recv_task = tokio::spawn(async move {
        let mut cnt = 0;
        while let Some(Ok(msg)) = receiver.next().await {
            cnt += 1;
            // print message and break if instructed to do so
            if process_message(msg, who, stx.clone()).is_break() {
                break;
            }
        }
        cnt
    });

    if shared_state.stx.clone().send(StreamChannelMessage::NewListener).is_err() {
        log::info!("Could not send NewListener message");
    }

    // If any one of the tasks exit, abort the other.
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

    // returning from the handler closes the websocket connection
    log::debug!("Websocket context {who} destroyed");
}

/// helper to print contents of messages to stdout. Has special treatment for Close.
fn process_message(msg: Message, who: SocketAddr, tx: crossbeam_channel::Sender<StreamChannelMessage>) -> ControlFlow<(), ()> {
    match msg {
        Message::Text(t) => {
            match serde_json::from_str::<NdiSocketMessage>(t.as_str()) {
                Ok(message) => match message {
                    NdiSocketMessage::Actions { actions } => {
                        for action in actions {
                            match tx.send(StreamChannelMessage::PtzAction(action)) {
                                Ok(_) => {
                                    log::debug!("PtzAction sent over channel: {}", action);
                                }
                                Err(e) => {
                                    log::warn!("SendError<PtzAction>: {}", e);
                                }
                            };
                        }
                    }
                    _ => {}
                }
                Err(e) => {
                    log::warn!("Cannot convert from string {} :: {}", t, e);
                }
            }
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