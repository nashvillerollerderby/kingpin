mod ndi;
mod stream;
mod error;
mod scoreboard;
mod game;
mod window;
mod application;
// mod convert_gamefile;

extern crate sdl2;

use std::net::SocketAddr;
use std::sync::RwLock;
use crate::error::Result;
use axum::{routing::get, Router};
use grafton_ndi::VideoFrame;
use serde::Deserialize;
use tower_http::services::ServeDir;

#[derive(Deserialize)]
pub struct Config {

}

#[tokio::main]
async fn main() -> Result<()> {
    log4rs::init_file("log4rs.yaml", Default::default()).expect("No log4rs.yaml file found");

    log::info!("Initializing web server");

    let (frame_tx, frame_rx) = crossbeam_channel::bounded::<VideoFrame>(1);

    let ndi = ndi::NdiStreaming::new(frame_tx);

    application::launch(frame_rx, ndi);

    Ok(())
}
