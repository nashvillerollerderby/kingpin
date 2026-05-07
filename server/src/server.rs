mod ndi;
mod stream;
mod error;
mod scoreboard;
mod game;
mod window;
mod ndi_router;
// mod convert_gamefile;

extern crate sdl2;

use std::net::SocketAddr;
use std::sync::RwLock;
use crate::error::Result;
use crate::ndi_router::ndi_router;
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

    run_server(frame_tx).await;

    Ok(())
}

async fn run_server(frame_tx: crossbeam_channel::Sender<VideoFrame>) {
    // build our application with a single route
    let app = Router::new()
        // .nest("/api/", scoreboard::scoreboard())
        .nest("/api/ndi", ndi_router(frame_tx).await)
        .fallback_service(ServeDir::new("kingpin/browser"));

    // run our app with hyper, listening globally on port 3000
    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await.unwrap();
    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>()
    ).await.ok();
}
