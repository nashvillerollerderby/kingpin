use std::ffi::CStr;
use crate::error::Result;
use ndi_bindings::{FrameType, NDI, Receiver, Recv};
use serde::{Deserialize, Serialize};
use std::fmt::Formatter;
use std::sync::Arc;
use std::sync::{Mutex, RwLock};
use std::thread;
use std::time::Duration;

#[derive(Debug, Deserialize, Serialize, Clone)]
pub enum StreamChannelMessage {
    NewListener,
    Running(bool),
    Ptz(bool),
    PtzAction(PtzAction),
}

/// An exhaustive list of actions that can be applied to PTZ-enabled NDI devices
#[derive(Debug, Deserialize, Serialize, Copy, Clone)]
pub enum PtzAction {
    RecallPreset {
        preset: u32,
        speed: f32,
    },
    Zoom {
        value: f32,
    },
    ZoomSpeed {
        speed: f32,
    },
    PanTilt {
        pan: f32,
        tilt: f32,
    },
    PanTiltSpeed {
        pan_speed: f32,
        tilt_speed: f32,
    },
    StorePreset {
        preset_no: i32,
    },
    AutoFocus,
    Focus {
        value: f32,
    },
    FocusSpeed {
        speed: f32,
    },
    WhiteBalanceAuto,
    WhiteBalanceIndoor,
    WhiteBalanceOutdoor,
    WhiteBalanceOneshot,
    WhiteBalanceManual {
        red: f32,
        blue: f32,
    },
    ExposureAuto,
    ExposureManual {
        level: f32,
    },
    ExposureManualV2 {
        iris: f32,
        gain: f32,
        shutter_speed: f32,
    },
}

impl std::fmt::Display for PtzAction {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}

pub struct StreamState {
    receiver: Option<Receiver>,
    pub ptz: bool,
}

pub struct NdiStream {
    thread: Option<thread::JoinHandle<()>>,
    stopped: Arc<RwLock<bool>>,
    pub stream_state: Arc<RwLock<StreamState>>,
    tx: crossbeam_channel::Sender<StreamChannelMessage>,
    rx: crossbeam_channel::Receiver<StreamChannelMessage>,
}

impl NdiStream {
    pub fn new() -> (
        Self,
        crossbeam_channel::Sender<StreamChannelMessage>,
        crossbeam_channel::Receiver<StreamChannelMessage>,
    ) {
        let (tx, rx) = crossbeam_channel::bounded(300);
        let (ptx, prx) = crossbeam_channel::bounded(300);
        (
            NdiStream {
                thread: Default::default(),
                stopped: Arc::new(RwLock::new(true)),
                stream_state: Arc::new(RwLock::new(StreamState {
                    receiver: None,
                    ptz: false,
                })),
                tx: ptx,
                rx,
            },
            tx,
            prx,
        )
    }

    pub fn running(&self) -> bool {
        !*self.stopped.read().expect("Unable to read stream stopped")
    }

    pub fn set_receiver(&self, receiver: Receiver) {
        let mut state = self
            .stream_state
            .write()
            .expect("Unable to write to stream state");
        state.receiver = Some(receiver);
    }

    pub fn stop(&self) {
        *self
            .stopped
            .write()
            .expect("Unable to write to stream stopped") = true;
        log::info!("Stream stopped written as 'true'");
    }

    pub fn start(&mut self) {
        {
            *self
                .stopped
                .write()
                .expect("Unable to write to stream stopped") = false;
        }
        let state = self.stream_state.clone();
        let tx = self.tx.clone();
        let rx = self.rx.clone();
        let stopped = self.stopped.clone();
        self.thread = Some(thread::spawn(move || {
            match NDI::new() {
                Ok(ndi) => {
                    let mut state = state.write().expect("Unable to write to stream state");

                    let mut recv = Recv::new(&ndi, state.receiver.clone().unwrap())
                        .expect("Unable to create recv");

                    log::info!("Starting stream loop");
                    while !*stopped.read().expect("Unable to read stream stopped") {
                        log::trace!("stream::start::thread while running loop");
                        while let Ok(action) = rx.try_recv() {
                            match action {
                                StreamChannelMessage::NewListener => {
                                    tx.send(StreamChannelMessage::Ptz(state.ptz)).expect("Could not send Ptz message");
                                    tx.send(StreamChannelMessage::Running(true)).expect("Could not send Running message");
                                }
                                StreamChannelMessage::PtzAction(action) => {
                                    if state.ptz {
                                        if !handle_ptz_action(&ndi, &mut recv, action) { 
                                            log::warn!("Failed to apply action: {}", action);
                                            recv = Recv::new(&ndi, state.receiver.clone().unwrap())
                                                .expect("Unable to create recv");
                                            handle_ptz_action(&ndi, &mut recv, action);
                                        }
                                    } else {
                                        log::warn!("PTZ is not enabled on device");
                                    }
                                }
                                _ => {}
                            }
                            thread::sleep(Duration::from_millis(100));
                        }

                        match recv.capture(1000) {
                            Ok(FrameType::StatusChange) => {
                                log::debug!("StatusChange frame received");
                                state.ptz = recv.ptz_is_supported();
                                tx.send(StreamChannelMessage::Ptz(state.ptz)).expect("Could not send Ptz message");
                            }
                            Ok(FrameType::Metadata(metadata)) => {
                                log::info!("{:?}", metadata);
                                let meta = unsafe {
                                    let c_str = CStr::from_ptr(metadata.p_data);
                                    String::from_utf8_lossy(c_str.to_bytes())
                                };
                                log::info!("c_str {}", meta);
                            }
                            Err(e) => {
                                log::error!("Error on capture: {}", e);
                            }
                            _ => {}
                        }
                    }
                    log::info!("Ending stream loop");
                    tx.send(StreamChannelMessage::Ptz(false)).expect("Could not send Ptz message");
                    tx.send(StreamChannelMessage::Running(false)).expect("Could not send Running message");
                }
                Err(e) => {
                    log::error!("{}", e);
                }
            }
        }));
    }

    // pub fn apply_actions(&self, actions: Vec<PtzAction>) -> Result<()> {
    //     for action in actions {
    //         self.tx.send(action)?;
    //     }
    //     Ok(())
    // }
}

fn handle_ptz_action(ndi: &NDI, recv: &mut Recv, action: PtzAction) -> bool {
    log::info!("Received PTZ action: {}", action);
    match action {
        PtzAction::RecallPreset { preset, speed } => {
            recv.ptz_recall_preset(preset, speed)
        }
        PtzAction::Zoom { value } => recv.ptz_zoom(value),
        PtzAction::ZoomSpeed { speed } => recv.ptz_zoom_speed(speed),
        PtzAction::PanTilt { pan, tilt } => {
            recv.ptz_pan_tilt(pan, tilt)
        }
        PtzAction::PanTiltSpeed {
            pan_speed,
            tilt_speed,
        } => recv.ptz_pan_tilt_speed(pan_speed, tilt_speed),
        PtzAction::StorePreset { preset_no } => {
            recv.ptz_store_preset(preset_no)
        }
        PtzAction::AutoFocus => recv.ptz_auto_focus(),
        PtzAction::Focus { value } => recv.ptz_focus(value),
        PtzAction::FocusSpeed { speed } => recv.ptz_focus_speed(speed),
        PtzAction::WhiteBalanceAuto => recv.ptz_white_balance_auto(),
        PtzAction::WhiteBalanceIndoor => {
            recv.ptz_white_balance_indoor()
        }
        PtzAction::WhiteBalanceOutdoor => {
            recv.ptz_white_balance_outdoor()
        }
        PtzAction::WhiteBalanceOneshot => {
            recv.ptz_white_balance_oneshot()
        }
        PtzAction::WhiteBalanceManual { red, blue } => {
            recv.ptz_white_balance_manual(red, blue)
        }
        PtzAction::ExposureAuto => recv.ptz_exposure_auto(),
        PtzAction::ExposureManual { level } => {
            recv.ptz_exposure_manual(level)
        }
        PtzAction::ExposureManualV2 {
            iris,
            gain,
            shutter_speed,
        } => recv.ptz_exposure_manual_v2(iris, gain, shutter_speed),
    }
}

// let vf = ndi_lib::NDIlib_video_frame_v2_t::default();
// let mut frame_sync = FrameSync::new(&recv)
//     .with_video_frame(vf);
// log::info!("Format type: {}", vf.xres);
// frame_sync.capture_video(FrameFormatType::Progressive);
