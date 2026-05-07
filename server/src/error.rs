#[cfg(feature = "server")]
use crate::stream::PtzAction;
use sdl2::IntegerOrSdlError;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum Error {
    #[cfg(feature = "server")]
    #[error("Axum: {0}")]
    Axum(#[from] axum::Error),
    #[cfg(feature = "server")]
    #[error("Send: {0}")]
    Send(#[from] crossbeam_channel::SendError<PtzAction>),
    #[error("SerdeJson: {0}")]
    SerdeJson(#[from] serde_json::Error),
    #[cfg(feature = "server")]
    #[error("Scoreboard: {0}")]
    Scoreboard(#[from] ScoreboardError),
    #[error("SDL2: {0}")]
    SDL2Error(String),
}

impl From<String> for Error {
    fn from(str: String) -> Self {
        Self::SDL2Error(str)
    }
}

impl From<IntegerOrSdlError> for Error {
    fn from(err: IntegerOrSdlError) -> Self {
        Self::SDL2Error(err.to_string())
    }
}

#[derive(Debug, Error)]
pub enum ScoreboardError {
    #[error("InvalidVersionKey: {0}")]
    InvalidVersionKey(String),
}

pub type Result<T> = std::result::Result<T, self::Error>;
