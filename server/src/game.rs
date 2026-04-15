// mod scoreboard;
mod error;

use std::collections::HashMap;
use std::fmt::Formatter;
use std::io::Read;
use std::sync::Arc;
use std::usize;
use tokio::sync::{Mutex, RwLock};
use std::time::Duration;
use serde::{Deserialize, Deserializer, Serialize};
use serde::de::{MapAccess, Visitor};
use serde_json::Value;
// use scoreboard::ScoreboardRouterState;

#[derive(Debug, Deserialize)]
pub struct GameFile {
    state: GameFileState
}

#[derive(Debug, Serialize)]
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
        let game_id = regex::Regex::new(r"ScoreBoard\.Game\(([a-z0-9\-]+)\)").unwrap();

        let mut state = GameFileState::new();
        while let Ok(Some((_k, s))) = map.next_entry::<String, Value>() {
            for (k, v) in s.as_object().unwrap().into_iter() {
                let no_left_paren = k.replacen('(', ".", usize::MAX);
                let no_right_paren = no_left_paren.replacen(')', "", usize::MAX);
                let split = no_right_paren.split('.').collect::<Vec<&str>>();
                log::info!("{:?}: {}", split, v);
            }
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

#[derive(Debug, Serialize, Deserialize, Eq, PartialEq, Hash)]
#[serde(rename_all(deserialize = "PascalCase"))]
enum ClockType {
    Intermission,
    Jam,
    Lineup,
    Period,
    Timeout,
}

#[derive(Debug, Serialize, Deserialize, Eq, PartialEq, Hash)]
#[serde(rename_all(deserialize = "PascalCase"))]
enum LabelType {

}

#[derive(Debug, Serialize, Deserialize, Eq, PartialEq, Hash)]
#[serde(rename_all(deserialize = "PascalCase"))]
enum PenaltyCode {

}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all(deserialize = "PascalCase"))]
struct Clock {
    direction: bool,
    id: uuid::Uuid,
    inverted_time: u32,
    maximum_time: u32,
    name: String,
    number: u16,
    readonly: bool,
    running: bool,
    time: u32,
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all(deserialize = "PascalCase"))]
struct Period {

}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all(deserialize = "PascalCase"))]
struct Team {

}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all(deserialize = "PascalCase"))]
struct EventInfo {
    date: String,
    start_time: String
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all(deserialize = "PascalCase"))]
struct Game {
    abort_reason: String,
    clock: HashMap<ClockType, Clock>,
    clock_during_final_score: bool,
    current_period: uuid::Uuid,
    current_period_number: u8,
    current_timeout: String,
    event_info: EventInfo,
    export_blocked_by: String,
    filename: String,
    id: uuid::Uuid,
    in_jam: bool,
    in_overtime: bool,
    in_period: bool,
    in_sudden_scoring: bool,
    injury_continuation_upcoming: bool,
    // jam
    json_exists: bool,
    labels: HashMap<LabelType, String>,
    name: String,
    name_format: String,
    no_more_jam: bool,
    official_review: bool,
    official_score: bool,
    penalty_code: HashMap<PenaltyCode, String>,
    periods: HashMap<u8, Period>,
    read_only: bool,
    rule: HashMap<String, String>,
    ruleset_name: String,
    state: String,
    statsbook_exists: bool,
    suspensions_served: String,
    teams: HashMap<u8, Team>,
    timeout_owner: String,
    upcoming_jam: uuid::Uuid,
    upcoming_jam_number: u8,
    update_in_progress: bool,
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

fn main() {
    log4rs::init_file("log4rs.yaml", Default::default()).expect("No log4rs.yaml file found");

    let path = std::path::Path::new("../example-data/crg-game-2025-03-24__Penguins_vs._Polar_Bears_(Finished__129_-_87).json");
    let file = std::fs::read(path).ok().unwrap();
    let wah = serde_json::from_slice::<GameFileState>(file.as_slice()).ok().unwrap();
    log::info!("{}", serde_json::to_string(&wah).ok().unwrap());
}

// #[derive(Debug, Deserialize, Serialize, Clone)]
// pub enum GameListenerMessage {
// }

// pub struct GameListener {
//     router_state: Arc<ScoreboardRouterState>,
//     thread: Option<tokio::task::JoinHandle<()>>,
//     stopped: Arc<RwLock<bool>>,
//     message_bus: Arc<Mutex<bus::Bus<GameListenerMessage>>>,
//     rx: crossbeam_channel::Receiver<GameListenerMessage>,
// }

// impl GameListener {
//     pub fn new(router_state: Arc<ScoreboardRouterState>) -> (Self, crossbeam_channel::Sender<GameListenerMessage>, Arc<Mutex<bus::Bus<GameListenerMessage>>>) {
//         let (tx, rx) = crossbeam_channel::bounded(300);
//         let bus = Arc::new(Mutex::new(bus::Bus::new(300)));
        
//         (GameListener {
//             router_state,
//             thread: None,
//             stopped: Arc::new(RwLock::new(true)),
//             message_bus: bus.clone(),
//             rx,
//         }, tx, bus)
//     }
    
//     pub async fn start(&mut self) {
//         {
//             let mut stopped = self.stopped.write().await;
//             *stopped = false;
//         }
//         let state = self.router_state.clone();
//         let rx = self.rx.clone();
//         let bus = self.message_bus.clone();
//         let stopped = self.stopped.clone();
//         self.thread = Some(tokio::spawn(async move {
//             let host = state.host.read().await;
            
            
//             while !*stopped.read().await {
//                 while let Ok(message) = rx.try_recv() {
//                     match message {
//                         _ => {}
//                     }
//                 }
                
//                 tokio::time::sleep(Duration::from_millis(1000)).await;
//             }
//         }))
//     }
// }


