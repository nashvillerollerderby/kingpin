use crate::stream::{NdiStream, PtzAction, StreamChannelMessage};
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

pub struct NdiStreaming {
    pub shared_state: Arc<NdiStreamingSharedState>,
}

impl NdiStreaming {
    pub fn new(frame_tx: crossbeam_channel::Sender<VideoFrame>) -> Self {
        Self {
            shared_state: Arc::new(NdiStreamingSharedState::new(frame_tx))
        }
    }
}

#[derive(Debug, Deserialize, Serialize, Clone)]
pub enum NdiSocketMessage {
    Actions { actions: Vec<PtzAction> },
    Ptz { is_supported: bool },
    Running { is_running: bool },
    Refresh,
    Sources { sources: Option<Vec<String>> },
    SelectSource { source: String },
    SelectedSource { source: Option<String> },
    Stop,
    Frame { data: String }
}

pub(crate) fn get_ndi_sources(
    _ndi: &NDI,
    timeout: u32,
    groups: Option<&str>,
    extra_ips: Option<&str>,
) -> Result<Vec<Source>, grafton_ndi::Error> {
    let options = FinderOptions::builder().groups::<String>(groups.unwrap_or_default().into()).extra_ips::<String>(extra_ips.unwrap_or_default().into()).build();
    let finder = Finder::new(_ndi, &options)?;
    log::info!("Finder instance created");
    let sources = finder.find_sources(Duration::from_millis(timeout as u64))?;
    log::info!("{} source(s) found", sources.len());
    sources.iter().for_each(|source| {
        log::info!(
            "{}: [{:?}, {:?}]",
            source.name,
            source.ip_address(),
            source.host()
        );
    });
    Ok(sources)
}

pub(crate) async fn get_sources_async(shared_state: Arc<NdiStreamingSharedState>) {
    match NDI::new() {
        Ok(ndi) => {
            let state = shared_state.clone();
            let mut state = state.ndi_data.lock().expect("Unable to lock ndi_data");

            let sources = match get_ndi_sources(&ndi, 5000, state.groups, state.extra_ips) {
                Ok(s) => Some(s),
                Err(e) => {
                    log::error!("{}", e);
                    None
                }
            };

            let source_list = sources.clone().map(|source_list|
                source_list.iter().map(|s| s.name.clone()
            ).collect());
            shared_state.broadcast_ndi_message(NdiSocketMessage::Sources { sources: source_list });
            state.sources = sources;
        }
        Err(e) => {
            log::error!("{}", e);
        }
    }
}

pub(crate) async fn source_selected(shared_state: Arc<NdiStreamingSharedState>) {
    match NDI::new() {
        Ok(ndi) => {
            if shared_state.stream.read().expect("").running() {
                log::info!("Cannot select source, stream is running. Stop the stream first.");
            }
            let data = shared_state
                .ndi_data
                .lock()
                .expect("Unable to lock ndi_data");

            let mut receiver_options = shared_state.receiver_options.write().expect("Cannot write to receiver_options");

            if let Some(source) = &data.selected_source {
                let mut stream = shared_state.stream.write().expect("Unable to write to shared stream");
                let options = ReceiverOptions::builder(source.clone())
                    .bandwidth(ReceiverBandwidth::Highest)
                    .color(ReceiverColorFormat::UYVY_RGBA)
                    .allow_video_fields(true)
                    .build();
                *receiver_options = Some(options.clone());
                stream.set_receiver_options(options);
                stream.start();
                shared_state.broadcast_ndi_message(NdiSocketMessage::SelectedSource { source: Some(source.to_string()) });
            } else {
                *receiver_options = None;
                shared_state.broadcast_ndi_message(NdiSocketMessage::SelectedSource { source: None });
            }
        }
        Err(e) => {
            log::error!("{}", e);
        }
    }
}

#[derive(Default)]
pub struct NDIData {
    pub sources: Option<Vec<Source>>,
    pub selected_source: Option<Source>,
    pub groups: Option<&'static str>,
    pub extra_ips: Option<&'static str>,
}

pub(crate) struct NdiStreamingSharedState {
    pub ndi_data: Mutex<NDIData>,
    pub receiver_options: RwLock<Option<ReceiverOptions>>,
    pub stream: Arc<RwLock<NdiStream>>,
    pub stx: crossbeam_channel::Sender<StreamChannelMessage>,
    pub ndi_message_bus: Arc<Mutex<bus::Bus<NdiSocketMessage>>>,
    pub stream_message_bus: Arc<Mutex<bus::Bus<StreamChannelMessage>>>,
}

unsafe impl Send for NdiStreamingSharedState {}
unsafe impl Sync for NdiStreamingSharedState {}

impl NdiStreamingSharedState {
    pub(crate) fn new(frame_tx: crossbeam_channel::Sender<VideoFrame>) -> Self {
        let groups = option_env!("NDI_GROUPS");
        log::info!("[env-provided] NDI Groups: {:?}", groups);
        let extra_ips = option_env!("NDI_EXTRA_IPS");
        log::info!("[env-provided] NDI Extra IPs: {:?}", extra_ips);

        let (stream, tx, stream_message_bus) = NdiStream::new(frame_tx);

        Self {
            ndi_data: Mutex::new(NDIData {
                groups,
                extra_ips,
                ..Default::default()
            }),
            receiver_options: RwLock::new(None),
            stream: Arc::new(RwLock::new(stream)),
            stx: tx,
            ndi_message_bus: Arc::new(Mutex::new(bus::Bus::new(10))),
            stream_message_bus,
        }
    }

    pub(crate) fn new_ndi_message_bus_recv(&self) -> bus::BusReader<NdiSocketMessage> {
        log::trace!("Locking NDI message bus for new receiver");
        let mut bus = self.ndi_message_bus.lock().expect("Could not write to ndi_message_bus");
        log::trace!("Locking NDI message bus for new receiver return");
        bus.add_rx()
    }

    pub(crate) fn new_stream_message_bus_recv(&self) -> bus::BusReader<StreamChannelMessage> {
        log::trace!("Locking stream message bus for new receiver start");
        let mut bus = self.stream_message_bus.lock().expect("Could not write to ndi_message_bus");
        log::trace!("Locking stream message bus for new receiver return");
        bus.add_rx()
    }

    pub(crate) fn broadcast_ndi_message(&self, message: NdiSocketMessage) {
        log::trace!("Locking NDI message bus to broadcast message start [{:?}]", message);
        let mut bus = self.ndi_message_bus.lock().expect("Could not write-lock the ndi message bus");
        bus.broadcast(message);
        log::trace!("Locking NDI message bus to broadcast message end");
    }

    pub(crate) fn broadcast_stream_message(&self, message: StreamChannelMessage) {
        log::trace!("Locking stream message bus to broadcast message start: [{:?}]", message);
        let mut bus = self.stream_message_bus.lock().expect("Could not write-lock the stream message bus");
        bus.broadcast(message);
        log::trace!("Locking stream message bus to broadcast message end");
    }
}

pub(crate) fn get_sources_and_selected_source_from_shared_state(shared_state: Arc<NdiStreamingSharedState>) -> (Option<Vec<String>>, Option<String>) {
    let data = shared_state
        .ndi_data
        .lock()
        .expect("Unable to lock ndi_data");
    (data
        .sources
        .clone()
        .map(|srcs| srcs.iter().map(|s| s.name.clone()).collect::<Vec<String>>()), data.selected_source.clone().map(|s| s.name))
}

pub(crate) fn get_sources_from_shared_state(shared_state: Arc<NdiStreamingSharedState>) -> Option<Vec<String>> {
    let data = shared_state
        .ndi_data
        .lock()
        .expect("Unable to lock ndi_data");
    data
        .sources
        .clone()
        .map(|srcs| srcs.iter().map(|s| s.name.clone()).collect::<Vec<String>>())
}


