mod ndi;
mod stream;
mod error;
mod scoreboard;
mod game;
// mod convert_gamefile;

use std::net::SocketAddr;
use std::sync::RwLock;
use crate::error::Result;
use crate::ndi::ndi_router;
use axum::{routing::get, Router};
use serde::Deserialize;
use tower_http::services::ServeDir;

#[derive(Deserialize)]
pub struct Config {

}

#[tokio::main]
async fn main() -> Result<()> {
    log4rs::init_file("log4rs.yaml", Default::default()).expect("No log4rs.yaml file found");

    log::info!("Initializing web server");

    // build our application with a single route
    let app = Router::new()
        // .nest("/api/", scoreboard::scoreboard())
        .nest("/api/ndi", ndi_router().await)
        .fallback_service(ServeDir::new("kingpin/browser"));

    // run our app with hyper, listening globally on port 3000
    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await.unwrap();
    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>()
    ).await.unwrap();
    Ok(())
}
