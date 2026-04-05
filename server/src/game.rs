use std::collections::HashMap;
use std::fmt::Formatter;
use std::sync::Arc;
use tokio::sync::{Mutex, RwLock};
use std::time::Duration;
use serde::{Deserialize, Deserializer, Serialize};
use serde::de::{MapAccess, Visitor};
use serde_json::Value;
use crate::scoreboard::ScoreboardRouterState;

#[derive(Debug, Deserialize)]
pub struct GameFile {
    state: GameFileState
}

#[derive(Debug)]
struct GameFileState {
    games: HashMap<uuid::Uuid, Game>,
    version: HashMap<VersionKey, String>,
}

impl GameFileState {
    fn new() -> Self {
        Self {
            games: HashMap::new(),
            version: HashMap::new(),
        }
    }
}

struct GameFileStateVisitor;

impl<'de> Visitor<'de> for GameFileStateVisitor {
    type Value = GameFileState;

    fn expecting(&self, formatter: &mut Formatter) -> std::fmt::Result {
        todo!()
    }

    fn visit_map<A>(self, mut map: A) -> Result<Self::Value, A::Error>
    where
        A: MapAccess<'de>
    {
        let mut state = GameFileState::new();
        while let Ok(Some((k, v))) = map.next_entry::<String, Value>() {
            let no_left_paren = k.replace('(', ".");
            let no_right_paren = no_left_paren.replace(')', "");
            let split = no_right_paren.split('.').collect::<Vec<&str>>();
            log::info!("{:?}: {}", split, v);
        }
        Ok(state)
    }
}

impl<'de> Deserialize<'de> for GameFileState {
    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
    where
        D: Deserializer<'de>
    {
        deserializer.deserialize_map(GameFileStateVisitor)
    }
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "PascalCase")]
struct Game {
    abort_reason: String,
    direction: bool,
}

#[derive(Debug, Eq, PartialEq, Hash, Serialize, Deserialize)]
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

// impl TryFrom<String> for VersionKey {
//     type Error = Error;
//
//     fn try_from(value: String) -> Result<VersionKey> {
//         Ok(match value.as_str() {
//             "release" => VersionKey::Release,
//             "release.commit" => VersionKey::Commit,
//             "release.host" => VersionKey::Host,
//             "release.time" => VersionKey::Time,
//             "release.user" => VersionKey::User,
//             _ => return Err(Error::Scoreboard(ScoreboardError::InvalidVersionKey(value))),
//         })
//     }
// }



#[derive(Debug, Deserialize, Serialize, Clone)]
pub enum GameListenerMessage {
}

pub struct GameListener {
    router_state: Arc<ScoreboardRouterState>,
    thread: Option<tokio::task::JoinHandle<()>>,
    stopped: Arc<RwLock<bool>>,
    message_bus: Arc<Mutex<bus::Bus<GameListenerMessage>>>,
    rx: crossbeam_channel::Receiver<GameListenerMessage>,
}

impl GameListener {
    pub fn new(router_state: Arc<ScoreboardRouterState>) -> (Self, crossbeam_channel::Sender<GameListenerMessage>, Arc<Mutex<bus::Bus<GameListenerMessage>>>) {
        let (tx, rx) = crossbeam_channel::bounded(300);
        let bus = Arc::new(Mutex::new(bus::Bus::new(300)));
        
        (GameListener {
            router_state,
            thread: None,
            stopped: Arc::new(RwLock::new(true)),
            message_bus: bus.clone(),
            rx,
        }, tx, bus)
    }
    
    pub async fn start(&mut self) {
        {
            let mut stopped = self.stopped.write().await;
            *stopped = false;
        }
        let state = self.router_state.clone();
        let rx = self.rx.clone();
        let bus = self.message_bus.clone();
        let stopped = self.stopped.clone();
        self.thread = Some(tokio::spawn(async move {
            let host = state.host.read().await;
            
            
            while !*stopped.read().await {
                while let Ok(message) = rx.try_recv() {
                    match message {
                        _ => {}
                    }
                }
                
                tokio::time::sleep(Duration::from_millis(1000)).await;
            }
        }))
    }
}


