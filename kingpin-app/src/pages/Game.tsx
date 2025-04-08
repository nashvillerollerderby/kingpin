import {
  chooseGame,
  selectClocks,
  selectGames,
  selectGameSelectedGameId,
  updateConnectionStatus,
  updateGameState
} from "../store/gameSlice.ts";
import { ConnectionStatus, SocketHolderInstance } from "../tooling";
import { store } from "../store/store.ts";
import { useKingpinDispatch, useKingpinSelector } from "../store/hooks.ts";

export function Game() {
  if (!SocketHolderInstance.isInstantiated()) {
    SocketHolderInstance.getOrCreateSocket();
    SocketHolderInstance.onStateUpdate((_state, changed) => {
      store.dispatch(updateGameState(changed));
    });
    SocketHolderInstance.onConnectionStatusChange(status => {
      console.debug(`Socket status change: ${status}`);
      store.dispatch(updateConnectionStatus(status));
      if (status === ConnectionStatus.DISCONNECTED) {
        setTimeout(() => {
          SocketHolderInstance.connect('ScoreBoard');
        }, 3000);
      }
    });
    SocketHolderInstance.connect('ScoreBoard');
  }

  const dispatch = useKingpinDispatch();
  const { Period, Intermission, Jam, Lineup, Timeout } = useKingpinSelector(selectClocks);
  const games = useKingpinSelector(selectGames);
  const selectedGameId = useKingpinSelector(selectGameSelectedGameId);

  const numberToTimeRepresentation = (longtime: number) => {
    const minutes = Math.floor((longtime) / 1000 / 60);
    let seconds: number | string = (longtime) / 1000 % 60;
    seconds = `${seconds > 9 ? seconds : `0${seconds}`}`;
    return `${minutes}:${seconds}`;
  }

  const gameSelect = (key: string) => {
    dispatch(chooseGame(key));
    dispatch(updateGameState(undefined));
  }

  return (
    <>
      {Period ? <>
        <p>Period: {numberToTimeRepresentation(Period.Time)}</p>
      </> : null}
      {Intermission ? <>
        <p>Intermission: {numberToTimeRepresentation(Intermission.Time)}</p>
      </> : null}
      {Jam ? <>
        <p>Jam: {numberToTimeRepresentation(Jam.Time)}</p>
      </> : null}
      {Lineup ? <>
        <p>Lineup: {numberToTimeRepresentation(Lineup.Time)}</p>
      </> : null}
      {Timeout ? <>
        <p>Timeout: {numberToTimeRepresentation(Timeout.Time)}</p>
      </> : null}
      <br/>
      <hr/>
      <br/>
      {Object.entries(games).map(([ k, v ]) => k === selectedGameId ? <b key={k}
                                                                         onClick={() => gameSelect(k)}>{v}
      </b> : <p key={k}
                onClick={() => gameSelect(k)}>{v}
      </p>)}
    </>
  )
}
