use crate::stream::{NdiStream, PtzAction, StreamChannelMessage};
use crate::ndi::*;
use axum::{
    body::Body,
    extract::{
        connect_info::ConnectInfo,
        ws::{Message, Utf8Bytes, WebSocket, WebSocketUpgrade},
        Json, Path, State,
    },
    http::StatusCode,
    response::IntoResponse,
    routing::{any, get},
    Router,
};
use axum_extra::TypedHeader;
use futures::{sink::SinkExt, stream::StreamExt};
use futures_util::stream::SplitSink;
use grafton_ndi::{Finder, FinderOptions, NDI, Receiver, ReceiverBandwidth, ReceiverColorFormat, ReceiverOptions, Source, VideoFrame};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::{
    net::SocketAddr,
    ops::ControlFlow,
    str::FromStr,
    sync::{
        Arc, Mutex, RwLock
    }, time::Duration,
};

/// Router
pub async fn ndi_router(frame_tx: crossbeam_channel::Sender<VideoFrame>) -> Router {
    log::info!("Initializing state");
    let shared_state: Arc<SharedState> = Arc::new(SharedState::new(frame_tx));

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
    let sources = get_sources_from_shared_state(shared_state);
    serde_json::json!({
        "sources": sources
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

async fn send_message(sender: &mut SplitSink<WebSocket, Message>, message: NdiSocketMessage) {
    if sender.send(Message::Text(
        Utf8Bytes::from(serde_json::to_string(&message).unwrap())
    )).await.is_err() {
        log::warn!("Unable to send NdiSocketMessage");
    }
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
            if process_message(msg, who, shared_state.stx.clone(), shared_state.clone()).await.is_break() {
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

    let cloned_shared_state = shared_state.clone();
    let mut ndi_bus = shared_state.new_ndi_message_bus_recv();
    let mut stream_bus = shared_state.new_stream_message_bus_recv();
    // Spawn a task that will push several messages to the client (does not matter what client does)
    let mut send_task = tokio::spawn(async move {
        let (sources, selected_source) = get_sources_and_selected_source_from_shared_state(cloned_shared_state.clone());
        send_message(&mut sender, NdiSocketMessage::Sources { sources }).await;
        send_message(&mut sender, NdiSocketMessage::SelectedSource { source: selected_source }).await;

        loop {
            while let Ok(action) = stream_bus.try_recv() {
                match action {
                    StreamChannelMessage::Running(is_running) => {
                        let message = NdiSocketMessage::Running { is_running };
                        send_message(&mut sender, message).await;
                    }
                    StreamChannelMessage::Ptz(is_supported) => {
                        let message = NdiSocketMessage::Ptz { is_supported };
                        send_message(&mut sender, message).await;
                    }
                    StreamChannelMessage::Frame(data) => {
                        let message = NdiSocketMessage::Frame { data };
                        send_message(&mut sender, message).await;
                    }
                    _ => {}
                }
            }
            while let Ok(action) = ndi_bus.try_recv() {
                match action.clone() {
                    NdiSocketMessage::Sources { .. } |
                    NdiSocketMessage::SelectedSource { .. } => {
                        send_message(&mut sender, action).await;
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

    let state = shared_state.clone();
    let stx = shared_state.stx.clone();
    // This second task will receive messages from client and print them on server console
    let mut recv_task = tokio::spawn(async move {
        let mut cnt = 0;
        while let Some(Ok(msg)) = receiver.next().await {
            cnt += 1;
            // print message and break if instructed to do so
            if process_message(msg, who, stx.clone(), state.clone()).await.is_break() {
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
async fn process_message(msg: Message, who: SocketAddr, tx: crossbeam_channel::Sender<StreamChannelMessage>, shared_state: Arc<SharedState>) -> ControlFlow<(), ()> {
    match msg {
        Message::Text(t) => {
            match serde_json::from_str::<NdiSocketMessage>(t.as_str()) {
                Ok(message) => match message {
                    NdiSocketMessage::Actions { actions } => {
                        for action in actions {
                            match tx.send(StreamChannelMessage::PtzAction(action)) {
                                Ok(_) => {
                                    log::trace!("PtzAction sent over channel: {}", action);
                                }
                                Err(e) => {
                                    log::warn!("SendError<PtzAction>: {}", e);
                                }
                            };
                        }
                    }
                    NdiSocketMessage::Refresh => {
                        get_sources_async(shared_state).await
                    }
                    NdiSocketMessage::SelectSource { source: name } => {
                        let mut data = shared_state
                            .ndi_data
                            .lock()
                            .expect("Unable to lock ndi_data");
                        for source in data.sources.clone().expect("No sources found") {
                            if source.name == name {
                                data.selected_source = Some(source);
                                log::info!("Selected {}", name);
                                tokio::spawn(source_selected(shared_state.clone()));
                            }
                        }
                    }
                    NdiSocketMessage::Stop => {
                        let stream = shared_state.stream.read().expect("Unable to read stream");
                        stream.stop();
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