use crate::error::{Error, Result, ScoreboardError};
use axum::Router;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "PascalCase")]
struct Game {
    abort_reason: String,
    direction: bool,
}

#[derive(Eq, PartialEq, Hash, Serialize, Deserialize)]
enum VersionKey {
    Release,
    Commit,
    Host,
    Time,
    User
}

impl From<VersionKey> for String {
    fn from(value: VersionKey) -> Self {
        match value {
            VersionKey::Release => "release",
            VersionKey::Commit => "release.commit",
            VersionKey::Host => "release.host",
            VersionKey::Time => "release.time",
            VersionKey::User => "release.user",
        }.into()
    }
}

impl TryFrom<String> for VersionKey {
    type Error = Error;

    fn try_from(value: String) -> Result<VersionKey> {
        Ok(match value.as_str() {
            "release" => VersionKey::Release,
            "release.commit" => VersionKey::Commit,
            "release.host" => VersionKey::Host,
            "release.time" => VersionKey::Time,
            "release.user" => VersionKey::User,
            _ => return Err(Error::Scoreboard(ScoreboardError::InvalidVersionKey(value))),
        })
    }
}

#[derive(Default, Serialize, Deserialize)]
struct ScoreboardState {
    game: HashMap<String, Game>,
    version: HashMap<VersionKey, String>
}

impl ScoreboardState {
    pub fn new() -> Self {
        Self {
            ..Default::default()
        }
    }
}

struct ScoreboardRouterState {
    scoreboard: Arc<RwLock<ScoreboardState>>
}

impl ScoreboardRouterState {
    pub fn new() -> Self {
        Self {
            scoreboard: Default::default()
        }
    }
}

pub fn scoreboard() -> Router {
    let state = Arc::new(ScoreboardState::new());

    Router::new()
        .with_state(state)
}