mod application;
mod error;
mod ndi;
mod stream;
mod window;

extern crate sdl2;

use crate::error::Result;
use grafton_ndi::VideoFrame;
use serde::Deserialize;
use std::net::SocketAddr;
use std::sync::RwLock;
use tower_http::services::ServeDir;

#[derive(Deserialize)]
pub struct Config {}

#[tokio::main]
async fn main() -> Result<()> {
    log4rs::init_file("log4rs.yaml", Default::default()).expect("No log4rs.yaml file found");

    let (frame_tx, frame_rx) = crossbeam_channel::bounded::<VideoFrame>(1);

    let ndi = ndi::NdiStreaming::new(frame_tx);

    application::launch(frame_rx, ndi).await?;

    Ok(())
}
