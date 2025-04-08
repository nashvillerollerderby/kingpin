mod ndi;
mod stream;
mod error;
mod scoreboard;

use std::net::SocketAddr;
use std::sync::RwLock;
use crate::error::Result;
use crate::ndi::ndi_router;
use axum::{routing::get, Router};
use serde::Deserialize;

#[derive(Deserialize)]
pub struct Config {

}

#[tokio::main]
async fn main() -> Result<()> {
    log4rs::init_file("log4rs.yaml", Default::default()).unwrap();

    log::info!("Initializing web server");

    // build our application with a single route
    let app = Router::new()
        .route("/", get(|| async { "Hello, World!" }))
        .nest("/api/", scoreboard::scoreboard())
        .nest("/api/ndi", ndi_router().await);

    // run our app with hyper, listening globally on port 3000
    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await.unwrap();
    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>()
    ).await.unwrap();
    Ok(())
}
