import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { RootState } from "./store.ts";
import { ConnectionStatus } from "../tooling/socket.ts";
import { Game } from "../models";

export interface GameState {
  currentGame: Game
  selectedGameId?: string
  games: { [key: string]: string }
  latestState: any
  connectionStatus: ConnectionStatus
}

const initialState: GameState = {
  currentGame: {
    Clock: {},
    Jam: {},
    PenaltyCode: {},
    Rule: {},
    Team: {}
  },
  games: {},
  latestState: {},
  connectionStatus: ConnectionStatus.UNINITIALIZED
}

export const gameReducer = createSlice({
  name: 'game',
  initialState,
  reducers: {
    chooseGame: (gameState, action: PayloadAction<string>) => {
      gameState.selectedGameId = action.payload;
    },
    updateConnectionStatus: (gameState, action: PayloadAction<ConnectionStatus>) => {
      gameState.connectionStatus = action.payload
    },
    updateGameState: (gameState, action: PayloadAction<any>) => {
      const changes = action.payload ?? gameState.latestState;
      gameState.latestState = {
        ...gameState.latestState,
        ...changes
      };
      const changeKeys = Object.keys(changes);
      const changeset = new Set(changeKeys);
      const changesetIncludes = (key: string): boolean => {
        for (const change of changeset) {
          if (change.includes(key)) {
            return true;
          }
        }
        return false;
      }
      const getObjectFromChanges = (objectKey: string): any => {
        const obj: any = {};
        let started = false
        Object.entries(changes).forEach(([k, v]) => {
          if (k.startsWith(objectKey)) {
            started = true;
            let newKey = k.replace(objectKey, '');
            if (newKey.startsWith('.')) {
              newKey = newKey.substring(1);
            }
            newKey = newKey.replaceAll(/[()]/g, '');
            obj[newKey] = v;
          } else {
            if (started) {
              return obj;
            }
          }
        });
        return obj;
      }

      if (changesetIncludes(`ScoreBoard.Game`)) {
        const gameIdRegExp = /ScoreBoard\.Game\(([a-z0-9-]+)\)\.Id/;
        for (const change of changeKeys.filter(c => gameIdRegExp.test(c))) {
          const [_, id] = gameIdRegExp.exec(change) ?? [];
          if (!gameState.games[id]) {
            gameState.games[id] = changes[`ScoreBoard.Game(${id}).Name`];
          }
        }
      }

      const gameKeys = Object.keys(gameState.games);
      if (gameKeys.length === 1) {
        gameState.selectedGameId = gameKeys[0];
      }

      const forGame = (id: string) => {
        const gameIdPrefix = `ScoreBoard.${id}`;
        console.log(gameIdPrefix, changes);

        if (changesetIncludes(`${gameIdPrefix}.Clock(Intermission)`)) {
          gameState.currentGame.Clock.Intermission = {
            ...gameState.currentGame.Clock.Intermission ?? {},
            ...getObjectFromChanges(`${gameIdPrefix}.Clock(Intermission)`)
          }
        }
        if (changesetIncludes(`${gameIdPrefix}.Clock(Jam)`)) {
          gameState.currentGame.Clock.Jam = {
            ...gameState.currentGame.Clock.Jam ?? {},
            ...getObjectFromChanges(`${gameIdPrefix}.Clock(Jam)`)
          }
        }
        if (changesetIncludes(`${gameIdPrefix}.Clock(Lineup)`)) {
          gameState.currentGame.Clock.Lineup = {
            ...gameState.currentGame.Clock.Lineup ?? {},
            ...getObjectFromChanges(`${gameIdPrefix}.Clock(Lineup)`)
          }
        }
        if (changesetIncludes(`${gameIdPrefix}.Clock(Period)`)) {
          gameState.currentGame.Clock.Period = {
            ...gameState.currentGame.Clock?.Period ?? {},
            ...getObjectFromChanges(`${gameIdPrefix}.Clock(Period)`)
          }
        }
        if (changesetIncludes(`${gameIdPrefix}.Clock(Timeout)`)) {
          gameState.currentGame.Clock.Timeout = {
            ...gameState.currentGame.Clock?.Timeout ?? {},
            ...getObjectFromChanges(`${gameIdPrefix}.Clock(Timeout)`)
          }
        }
        if (changeset.has(`${gameIdPrefix}.Game`)) {
          gameState.currentGame.Id = changes[`${gameIdPrefix}.Game`];
        }
        if (changesetIncludes(`${gameIdPrefix}.PenaltyCode`)) {
          gameState.currentGame.PenaltyCode = getObjectFromChanges(`${gameIdPrefix}.PenaltyCode`);
        }
        if (changesetIncludes(`${gameIdPrefix}.Rule`)) {
          gameState.currentGame.Rule = getObjectFromChanges(`${gameIdPrefix}.Rule`);
        }
        if (changeset.has(`${gameIdPrefix}.Ruleset`)) {
          gameState.currentGame.Ruleset = changes[`${gameIdPrefix}.Ruleset`];
        }
        if (changeset.has(`${gameIdPrefix}.State`)) {
          gameState.currentGame.State = changes[`${gameIdPrefix}.State`];
        }
        if (changeset.has(`${gameIdPrefix}.TimeoutOwner`)) {
          gameState.currentGame.TimeoutOwner = changes[`${gameIdPrefix}.TimeoutOwner`];
        }
      }
      forGame(gameState.selectedGameId ? `Game(${gameState.selectedGameId})` : `CurrentGame`);
    }
  }
});

export const selectGameState = (state: RootState) => state.game;
export const selectPeriodClock = (state: RootState) => selectGameState(state).currentGame.Clock.Period;
export const selectClocks = (state: RootState) => selectGameState(state).currentGame.Clock;
export const selectGames = (state: RootState) => selectGameState(state).games;
export const selectGameSelectedGameId = (state: RootState) => selectGameState(state).selectedGameId;

export const { updateGameState, chooseGame, updateConnectionStatus } = gameReducer.actions

export default gameReducer.reducer