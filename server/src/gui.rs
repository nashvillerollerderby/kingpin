mod application;
mod error;
mod ndi;
mod stream;
mod window;

extern crate sdl2;

use crate::error::Result;
use grafton_ndi::VideoFrame;
use serde::{Deserialize, Serialize};
use std::net::SocketAddr;
use std::sync::RwLock;
use clap::Parser;
use tower_http::services::ServeDir;

#[derive(Parser, Debug, Serialize, Deserialize)]
#[command(version, about, long_about = None)]
pub struct Args {
    #[clap(short, long)]
    fullscreen: bool
}

#[tokio::main]
async fn main() -> Result<()> {
    let args = Args::parse();
    log4rs::init_file("log4rs.yaml", Default::default()).expect("No log4rs.yaml file found");

    let (frame_tx, frame_rx) = crossbeam_channel::bounded::<VideoFrame>(1);

    let ndi = ndi::NdiStreaming::new(frame_tx);

    application::launch(frame_rx, ndi, args.fullscreen).await?;

    Ok(())
}
