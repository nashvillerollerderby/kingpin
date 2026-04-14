use std::ffi::CStr;
use grafton_ndi::{FrameSync, FrameType, NDI, Receiver, ReceiverOptions, ReceiverStatus, VideoFrame};
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
    Frame(String)
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
    receiver_options: Option<ReceiverOptions>,
    pub ptz: bool,
}

pub struct NdiStream {
    action_handle: Option<thread::JoinHandle<()>>,
    frame_handle: Option<thread::JoinHandle<()>>,
    stopped: Arc<RwLock<bool>>,
    pub stream_state: Arc<RwLock<StreamState>>,
    message_bus: Arc<Mutex<bus::Bus<StreamChannelMessage>>>,
    rx: crossbeam_channel::Receiver<StreamChannelMessage>,
    frame_tx: crossbeam_channel::Sender<VideoFrame>,
}

impl NdiStream {
    pub fn new(frame_tx: crossbeam_channel::Sender<VideoFrame>) -> (
        Self,
        crossbeam_channel::Sender<StreamChannelMessage>,
        Arc<Mutex<bus::Bus<StreamChannelMessage>>>,
    ) {
        let (tx, rx) = crossbeam_channel::bounded(300);
        let bus = Arc::new(Mutex::new(bus::Bus::new(300)));
        (
            NdiStream {
                action_handle: Default::default(),
                frame_handle: Default::default(),
                stopped: Arc::new(RwLock::new(true)),
                stream_state: Arc::new(RwLock::new(StreamState {
                    receiver_options: None,
                    ptz: true,
                })),
                message_bus: bus.clone(),
                rx,
                frame_tx,
            },
            tx,
            bus,
        )
    }

    pub fn running(&self) -> bool {
        !*self.stopped.read().expect("Unable to read stream stopped")
    }

    pub fn set_receiver_options(&self, options: ReceiverOptions) {
        match NDI::new() {
                Ok(ndi) => {
                let mut state = self
                    .stream_state
                    .write()
                    .expect("Unable to write to stream state");
                state.receiver_options = Some(options);
            }
            Err(e) => {
                log::error!("{}", e);
            }
        }
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

        #[cfg(feature = "with_gui")]
        let read_frames = true;
        #[cfg(not(feature = "with_gui"))]
        let read_frames = false;

        let frame_state: Arc<RwLock<StreamState>> = self.stream_state.clone();
        let frame_bus = self.message_bus.clone();
        let frame_tx = self.frame_tx.clone();
        let rx = self.rx.clone();
        let frame_stopped = self.stopped.clone();

        let action_state = self.stream_state.clone();
        let action_bus = self.message_bus.clone();
        let action_stopped = self.stopped.clone();

        if read_frames {
            self.frame_handle = Some(thread::spawn(move || {
            match NDI::new() {
                Ok(ndi) => {
                    let state = frame_state.read().expect("Unable to write to stream state");

                    let receiver = Receiver::new(&ndi, &state.receiver_options.as_ref().unwrap())
                        .expect("Unable to create receiver for source");
                    let fs = FrameSync::new(receiver).expect("Unable to framesync");

                    log::info!("Starting frame stream loop");
                    while !*frame_stopped.read().expect("Unable to read stream stopped") {
                        match fs.capture_video_owned(grafton_ndi::ScanType::Progressive) {
                            Some(Ok(frame)) => {
                                log::info!("Received frame in stream thread");
                                frame_tx.send(frame).ok();
                            }
                            Some(Err(e)) => {
                                log::error!("{}", e);
                            }
                            None => {
                                log::info!("No frame data");
                            }
                        }
                    }
                    log::info!("Ending stream loop");
                    {
                        let mut bus = frame_bus.lock().expect("Could not write to message bus");
                        bus.broadcast(StreamChannelMessage::Ptz(false));
                        bus.broadcast(StreamChannelMessage::Running(false));
                    }
                }
                Err(e) => {
                    log::error!("{}", e);
                }
            }
        }));
        }
        
        self.action_handle = Some(thread::spawn(move || {
            match NDI::new() {
                Ok(ndi) => {
                    let state = action_state.read().expect("Unable to write to stream state");
                    let mut receiver = Receiver::new(&ndi, &state.receiver_options.as_ref().unwrap())
                        .expect("Unable to create receiver for source");

                    log::info!("Starting action stream loop");
                    while !*action_stopped.read().expect("Unable to read stream stopped") {
                        log::trace!("stream::start::thread while running loop");
                        while let Ok(action) = rx.try_recv() {
                            match action {
                                StreamChannelMessage::NewListener => {
                                    {
                                        let mut bus = action_bus.lock().expect("Could not write to message bus");
                                        bus.broadcast(StreamChannelMessage::Ptz(state.ptz));
                                        bus.broadcast(StreamChannelMessage::Running(true));
                                    }
                                }
                                StreamChannelMessage::PtzAction(action) => {
                                    if state.ptz {
                                        handle_ptz_action(&ndi, &mut receiver, action);
                                    } else {
                                        log::warn!("PTZ is not enabled on device");
                                    }
                                }
                                _ => {
                                    log::info!("Other action received");
                                }
                            }
                        }
                    }
                    log::info!("Ending stream loop");
                    {
                        let mut bus = action_bus.lock().expect("Could not write to message bus");
                        bus.broadcast(StreamChannelMessage::Ptz(false));
                        bus.broadcast(StreamChannelMessage::Running(false));
                    }
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

fn handle_ptz_action(ndi: &NDI, recv: &mut Receiver, action: PtzAction) -> Result<(), grafton_ndi::Error> {
    log::trace!("Received PTZ action: {}", action);
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