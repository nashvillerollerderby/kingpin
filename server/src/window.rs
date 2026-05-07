use crate::application::ApplicationStatus;
use crate::application::InvertAxisToMult;
use crate::application::RwLockedApplicationState;
use crate::application::SharedApplicationState;
use crate::ndi::{get_sources_async, get_sources_from_shared_state, source_selected};
use crate::stream::PtzAction;
use crate::stream::PtzAction::{Focus, WhiteBalanceOneshot};
use crate::stream::StreamChannelMessage;
use chrono::Utc;
use grafton_ndi::{LineStrideOrSize, PixelFormat, VideoFrame};
use log::kv::ToValue;
use sdl2::controller::{Axis, Button, GameController};
use sdl2::event::Event;
use sdl2::keyboard::Keycode;
use sdl2::pixels::Color;
use sdl2::render::Texture;
use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Mutex;
use std::sync::{Arc, MutexGuard};
use std::time::Duration;

trait ToSdl2PixelFormat {
    fn to_sdl2_pixel_format(&self) -> Option<sdl2::pixels::PixelFormatEnum>;
}

impl ToSdl2PixelFormat for grafton_ndi::PixelFormat {
    fn to_sdl2_pixel_format(&self) -> Option<sdl2::pixels::PixelFormatEnum> {
        match self {
            grafton_ndi::PixelFormat::UYVY => Some(sdl2::pixels::PixelFormatEnum::UYVY),
            grafton_ndi::PixelFormat::BGRA => Some(sdl2::pixels::PixelFormatEnum::BGRA4444),
            _ => None,
        }
    }
}

pub async fn spawn_window(
    shared_application_state: SharedApplicationState,
) -> crate::error::Result<()> {
    sdl2::hint::set("SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS", "1");
    let sdl_context = sdl2::init()?;
    let video_subsystem = sdl_context.video()?;
    let controller_subsystem = sdl_context.game_controller()?;
    controller_subsystem.set_event_state(true);
    controller_subsystem
        .load_mappings(PathBuf::from("./resources/gamecontrollerdb.txt"))
        .expect("Could not load gamecontrollerdb.txt into SDL2");

    let window = video_subsystem
        .window("Kingpin PTZ Controller", 1280, 800)
        .vulkan()
        .position_centered()
        // .fullscreen()
        .resizable()
        .build()
        .unwrap();

    let mut canvas = window.into_canvas().build().unwrap();
    let texture_creator = canvas.texture_creator();

    let mut status = ApplicationStatus::Unset;

    let frame_rx = shared_application_state.frame_rx.clone();
    let mut frame: Option<VideoFrame> = None;
    let mut texture: Option<Texture> = None;

    canvas.set_draw_color(Color::RGB(0, 0, 0));
    canvas.clear();
    canvas.present();
    let mut controllers = HashMap::new();
    let mut event_pump = sdl_context.event_pump()?;
    let app_state: Arc<Mutex<RwLockedApplicationState>> = shared_application_state.mutex.clone();
    let ndi_streaming_state = shared_application_state.ndi_streaming_state.clone();
    let stream_channel = ndi_streaming_state.stx.clone();

    let mut pan_tilt_zeroed = true;
    let mut focus_zoom_zeroed = true;
    let mut focus = 0.5;

    let mut rb_pressed = false;
    let mut lb_pressed = false;

    stream_channel
        .send(StreamChannelMessage::PtzAction(PtzAction::Focus {
            value: focus,
        }))
        .unwrap();

    get_sources_async(ndi_streaming_state.clone()).await;

    'running: loop {
        // Set current application status
        {
            let lock = app_state
                .lock()
                .expect("Unable to lock shared application state");
            status = lock.status.clone();
        }

        canvas.clear();
        for event in event_pump.poll_iter() {
            match (status.clone(), event) {
                // Handle quit events
                (_, Event::Quit { .. })
                | (
                    _,
                    Event::KeyDown {
                        keycode: Some(Keycode::Escape),
                        ..
                    },
                )
                | (
                    ApplicationStatus::Home,
                    Event::ControllerButtonDown {
                        button: Button::B, ..
                    },
                ) => break 'running,

                // Handle controllers
                (_, Event::ControllerDeviceAdded { which, .. }) => {
                    if controller_subsystem.is_game_controller(which) {
                        let c = controller_subsystem.open(which)?;
                        log::info!("Gamepad {:?} opened", c.name());
                        controllers.insert(which, c);
                    }
                }
                (_, Event::ControllerDeviceRemoved { which, .. }) => {
                    controllers.remove(&which);
                }

                // Navigate to Sources page from Home
                (
                    ApplicationStatus::Home,
                    Event::ControllerButtonDown {
                        button: Button::A, ..
                    },
                ) => {
                    let mut lock = shared_application_state
                        .mutex
                        .lock()
                        .expect("Unable to lock application status");
                    let sources = get_sources_from_shared_state(ndi_streaming_state.clone());
                    lock.status = ApplicationStatus::Sources {
                        target: if let Some(ref sources) = sources
                            && !sources.is_empty()
                        {
                            Some(sources[0].clone())
                        } else {
                            None
                        },
                    }
                }

                // Select target source and start stream
                (
                    ApplicationStatus::Sources {
                        target: Some(target),
                    },
                    Event::ControllerButtonDown {
                        button: Button::A, ..
                    },
                ) => {
                    let state = ndi_streaming_state.clone();
                    let mut data = state.ndi_data.lock().expect("Unable to lock ndi_data");
                    for source in data.sources.clone().expect("No sources found") {
                        if source.name == target {
                            data.selected_source = Some(source);
                            log::info!("Selected {}", target);
                            tokio::spawn(source_selected(ndi_streaming_state.clone()));

                            let mut lock = app_state.lock().expect("Unable to lock app state");
                            lock.status = ApplicationStatus::Streaming;
                        }
                    }
                }

                // While streaming
                (
                    ApplicationStatus::Streaming,
                    Event::ControllerButtonDown {
                        button: Button::B, ..
                    },
                ) => {
                    let lock = ndi_streaming_state
                        .stream
                        .read()
                        .expect("Unable to lock NDI Stream");
                    lock.stop();
                    texture = None;
                    frame = None;

                    let mut lock = app_state.lock().expect("Unable to lock app state");
                    lock.status = ApplicationStatus::Home;
                }

                // Handle right stick axes
                (
                    ApplicationStatus::Streaming,
                    Event::ControllerAxisMotion {
                        axis: Axis::RightX | Axis::RightY,
                        which,
                        ..
                    },
                ) => {
                    let state = app_state.lock().expect("Unable to lock application state");

                    let controller = controllers.get(&which).unwrap();
                    let mut normalized_x = 0.;
                    if let Some(x) = controller.axis(Axis::RightX).to_value().to_f64() {
                        normalized_x = (x / 32767.) as f32;
                    }
                    normalized_x = if normalized_x.abs() > 0.1 {
                        normalized_x.clamp(-1., 1.) * state.pan_sens * state.invert_rs_x.to_mult()
                    } else {
                        0.
                    };

                    let mut normalized_y = 0.;
                    if let Some(y) = controller.axis(Axis::RightY).to_value().to_f64() {
                        normalized_y = (y / 32767.) as f32;
                    }
                    normalized_y = if normalized_y.abs() > 0.1 {
                        normalized_y.clamp(-1., 1.) * state.tilt_sens * state.invert_rs_y.to_mult()
                    } else {
                        0.
                    };

                    if normalized_x != 0. || normalized_y != 0. {
                        log::debug!("Right stick: X: {} Y: {}", normalized_x, normalized_y);
                        stream_channel
                            .send(StreamChannelMessage::PtzAction(PtzAction::PanTiltSpeed {
                                pan_speed: normalized_x,
                                tilt_speed: normalized_y,
                            }))
                            .unwrap();
                        pan_tilt_zeroed = false;
                    } else if normalized_x == 0. && normalized_y == 0. && !pan_tilt_zeroed {
                        stream_channel
                            .send(StreamChannelMessage::PtzAction(PtzAction::PanTiltSpeed {
                                pan_speed: normalized_x,
                                tilt_speed: normalized_y,
                            }))
                            .unwrap();
                        pan_tilt_zeroed = true;
                    }
                }

                // Handle left stick axes
                (
                    ApplicationStatus::Streaming,
                    Event::ControllerAxisMotion {
                        axis: Axis::LeftX | Axis::LeftY,
                        which,
                        ..
                    },
                ) => {
                    let state = app_state.lock().expect("Unable to lock application state");

                    let controller = controllers.get(&which).unwrap();
                    let mut normalized_y = 0.;
                    if let Some(y) = controller.axis(Axis::LeftY).to_value().to_f64() {
                        normalized_y = (y / 32767.) as f32;
                    }
                    normalized_y = if normalized_y.abs() > 0.1 {
                        normalized_y.clamp(-1., 1.) * state.invert_rs_y.to_mult()
                    } else {
                        0.
                    };

                    if normalized_y >= 0.5 || normalized_y <= -0.5 {
                        log::debug!("Left stick: Y: {}", normalized_y);
                        stream_channel
                            .send(StreamChannelMessage::PtzAction(PtzAction::ZoomSpeed {
                                speed: normalized_y,
                            }))
                            .unwrap();
                        focus_zoom_zeroed = false;
                    } else if normalized_y == 0. && !focus_zoom_zeroed {
                        stream_channel
                            .send(StreamChannelMessage::PtzAction(PtzAction::ZoomSpeed {
                                speed: normalized_y,
                            }))
                            .unwrap();
                        focus_zoom_zeroed = true;
                    }
                }

                // WB
                (
                    ApplicationStatus::Streaming,
                    Event::ControllerButtonDown {
                        button: Button::X, ..
                    },
                ) => {
                    log::info!("wb oneshot");
                    stream_channel
                        .send(StreamChannelMessage::PtzAction(WhiteBalanceOneshot))
                        .unwrap();
                }

                // Focus
                (
                    ApplicationStatus::Streaming,
                    Event::ControllerButtonDown {
                        button: Button::RightShoulder,
                        ..
                    },
                ) => {
                    rb_pressed = true;
                }
                (
                    ApplicationStatus::Streaming,
                    Event::ControllerButtonDown {
                        button: Button::LeftShoulder,
                        ..
                    },
                ) => {
                    lb_pressed = true;
                }
                (
                    ApplicationStatus::Streaming,
                    Event::ControllerButtonUp {
                        button: Button::RightShoulder,
                        ..
                    },
                ) => {
                    rb_pressed = false;
                }
                (
                    ApplicationStatus::Streaming,
                    Event::ControllerButtonUp {
                        button: Button::LeftShoulder,
                        ..
                    },
                ) => {
                    lb_pressed = false;
                }
                _ => {}
            }
        }

        if rb_pressed {
            focus += 0.0025;
            focus = focus.clamp(0., 1.);
            stream_channel
                .send(StreamChannelMessage::PtzAction(Focus { value: focus }))
                .unwrap();
        } else if lb_pressed {
            focus -= 0.0025;
            focus = focus.clamp(0., 1.);
            stream_channel
                .send(StreamChannelMessage::PtzAction(Focus { value: focus }))
                .unwrap();
        }

        if let Ok(recv_frame) = frame_rx.try_recv() {
            frame = Some(recv_frame);
            log::debug!("New frame received by main thread");
        }

        // render frame data
        if let Some(ref mut texture) = texture
            && let Some(ref frame) = frame
        {
            render_frame(texture, frame);
            canvas.copy(texture, None, None).ok();
        } else if let Some(ref frame) = frame {
            texture = Some(
                texture_creator
                    .create_texture(
                        frame.pixel_format.to_sdl2_pixel_format(),
                        sdl2::render::TextureAccess::Streaming,
                        frame.width as u32,
                        frame.height as u32,
                    )
                    .unwrap(),
            );
        } else {
            while !frame_rx.is_empty() {
                frame_rx.recv().unwrap();
            }
            canvas.clear();
            canvas.set_draw_color(Color::RGB(0, 0, 0));
        }

        canvas.present();
        ::std::thread::sleep(Duration::new(0, 1_000_000_000u32 / 60));
    }
    Ok(())
}

fn render_frame(texture: &mut Texture, frame: &VideoFrame) {
    if let LineStrideOrSize::LineStrideBytes(stride) = frame.line_stride_or_size {
        texture.update(None, &frame.data, stride as usize).ok();
    } else {
        log::warn!("Could not determine line stride");
    }
}
