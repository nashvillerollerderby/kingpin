use std::sync::{Arc, Mutex};

use crate::Result;
use crate::ndi::{NdiStreaming, NdiStreamingSharedState};
use crate::window;
use grafton_ndi::VideoFrame;
use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Clone)]
pub enum ApplicationStatus {
    Home,
    Sources { target: Option<String> },
    Streaming,
    Unset,
}

pub trait InvertAxisToMult {
    fn to_mult(self) -> f32;
}

pub type InvertAxis = bool;

impl InvertAxisToMult for InvertAxis {
    fn to_mult(self) -> f32 {
        if self { -1.0 } else { 1.0 }
    }
}

pub struct RwLockedApplicationState {
    pub status: ApplicationStatus,
    pub invert_rs_x: bool,
    pub invert_rs_y: bool,
    pub pan_sens: f32,
    pub tilt_sens: f32,
}

impl RwLockedApplicationState {
    pub fn new() -> Self {
        Self {
            status: ApplicationStatus::Home,
            invert_rs_x: true,
            invert_rs_y: true,
            pan_sens: 0.6,
            tilt_sens: 0.6,
        }
    }
}

pub struct SharedApplicationState {
    pub mutex: Arc<Mutex<RwLockedApplicationState>>,
    pub frame_rx: crossbeam_channel::Receiver<VideoFrame>,
    pub ndi_streaming_state: Arc<NdiStreamingSharedState>,
    pub fullscreen: bool,
}

impl SharedApplicationState {
    pub fn new(frame_rx: crossbeam_channel::Receiver<VideoFrame>, ndi: NdiStreaming, fullscreen: bool) -> Self {
        Self {
            mutex: Arc::new(Mutex::new(RwLockedApplicationState::new())),
            frame_rx,
            ndi_streaming_state: ndi.shared_state.clone(),
            fullscreen,
        }
    }
}

pub async fn launch(
    frame_rx: crossbeam_channel::Receiver<VideoFrame>,
    ndi: NdiStreaming,
    fullscreen: bool
) -> Result<()> {
    window::spawn_window(SharedApplicationState::new(frame_rx, ndi, fullscreen)).await
}
