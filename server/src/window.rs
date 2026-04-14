use crate::application::SharedApplicationState;
use grafton_ndi::{LineStrideOrSize, PixelFormat, VideoFrame};
use sdl2::pixels::Color;
use sdl2::event::Event;
use sdl2::keyboard::Keycode;
use sdl2::render::Texture;
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

pub fn spawn_window(shared_application_state: SharedApplicationState) {
    let sdl_context = sdl2::init().unwrap();
    let video_subsystem = sdl_context.video().unwrap();

    let window = video_subsystem.window("Kingpin PTZ Controller", 1920, 1080)
        .vulkan()
        .position_centered()
        .fullscreen()
        .resizable()
        .build()
        .unwrap();

    let mut canvas = window.into_canvas().build().unwrap();
    let texture_creator = canvas.texture_creator();

    let frame_rx = shared_application_state.frame_rx.clone();
    let mut frame: Option<VideoFrame> = None;
    let mut texture: Option<Texture> = None;

    canvas.set_draw_color(Color::RGB(0, 0, 0));
    canvas.clear();
    canvas.present();
    let mut event_pump = sdl_context.event_pump().unwrap();
    'running: loop {
        canvas.clear();
        for event in event_pump.poll_iter() {
            match event {
                Event::Quit {..} |
                Event::KeyDown { keycode: Some(Keycode::Escape), .. } => {
                    break 'running
                },
                _ => {}
            }
        }
        
        if let Ok(recv_frame) = frame_rx.try_recv() {
            frame = Some(recv_frame);
            log::info!("New frame received by main thread");
        }

        // render frame data
        if let Some(ref mut texture) = texture && let Some(ref frame) = frame {
            render_frame(texture, frame);
            canvas.copy(texture, None, None).ok();
        } else if let Some(ref frame) = frame {
            texture = Some(texture_creator.create_texture(
                frame.pixel_format.to_sdl2_pixel_format(),
                sdl2::render::TextureAccess::Streaming,
                frame.width as u32,
                frame.height as u32
            ).unwrap());
        }

        canvas.present();
        ::std::thread::sleep(Duration::new(0, 1_000_000_000u32 / 60));
    }
}

fn render_frame(texture: &mut Texture, frame: &VideoFrame) {
    if let LineStrideOrSize::LineStrideBytes(stride) = frame.line_stride_or_size {
        texture.update(None, &frame.data, stride as usize).ok();
    } else {
        log::warn!("Could not determine line stride");
    }
}