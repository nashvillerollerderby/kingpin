use crate::stream::PtzAction;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum Error {
    #[error("Axum: {0}")]
    Axum(#[from] axum::Error),
    #[error("Send: {0}")]
    Send(#[from] crossbeam_channel::SendError<PtzAction>),
    #[error("SerdeJson: {0}")]
    SerdeJson(#[from] serde_json::Error),
    #[error("Scoreboard: {0}")]
    Scoreboard(#[from] ScoreboardError),
    #[error("NDI: {0}")]
    NDI(#[from] ndi_bindings::Error),
}

#[derive(Debug, Error)]
pub enum ScoreboardError {
    #[error("InvalidVersionKey: {0}")]
    InvalidVersionKey(String)
}

pub type Result<T> = std::result::Result<T, self::Error>;