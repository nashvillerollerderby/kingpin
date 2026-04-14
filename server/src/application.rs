use std::sync::{Arc, Mutex};

use crate::ndi::{NdiStreaming, NdiStreamingSharedState};
use crate::window;
use grafton_ndi::VideoFrame;

enum ApplicationStatus {
    Home
}

pub struct RwLockedApplicationState {
    pub status: ApplicationStatus
}

impl RwLockedApplicationState {
    pub fn new() -> Self {
        Self {
            status: ApplicationStatus::Home,
        }
    }
}

pub struct SharedApplicationState {
    pub mutex: Arc<Mutex<RwLockedApplicationState>>,
    pub frame_rx: crossbeam_channel::Receiver::<VideoFrame>,
    pub ndi_streaming_shared_state: Arc<NdiStreamingSharedState>
}

impl SharedApplicationState {
    pub fn new(frame_rx: crossbeam_channel::Receiver::<VideoFrame>, ndi: NdiStreaming) -> Self {
        Self {
            mutex: Arc::new(Mutex::new(RwLockedApplicationState::new())),
            frame_rx,
            ndi_streaming_shared_state: ndi.shared_state.clone(),
        }
    }
}

pub fn launch(
    frame_rx: crossbeam_channel::Receiver::<VideoFrame>,
    ndi: NdiStreaming,
) {
    window::spawn_window(SharedApplicationState::new(frame_rx, ndi));
}