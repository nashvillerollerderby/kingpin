import { useState, useEffect } from "react";
import { GamepadHolderInstance } from '../tooling';
import {
  useGetSourcesQuery, useIsRunningQuery,
  useRefreshSourcesMutation,
  useSelectSourceMutation,
  useStopMutation,
} from '../store/ndiService.ts';

export function StreamControls() {
  const [state, setState] = useState(GamepadHolderInstance.stateSubject.value);

  const { data: sources } = useGetSourcesQuery();
  const [refreshSources] = useRefreshSourcesMutation();
  const [selectSource] = useSelectSourceMutation();
  const { data: isRunning } = useIsRunningQuery();
  const [stopStreaming] = useStopMutation();

  const load = () => {
    refreshSources();
  };

  const startSocket = () => {
    GamepadHolderInstance.startSocket();
  }

  const stopSocket = () => {
    GamepadHolderInstance.stopSocket();
  }

  const startGamepad = async () => {
    await GamepadHolderInstance.startControllerLoop();
  }

  const stopGamepad = async () => {
    await GamepadHolderInstance.stopControllerLoop();
  }

  const stop = ()=> {
    stopStreaming();
  }

  useEffect(() => {
    const subscription= GamepadHolderInstance.stateSubject.subscribe((state) => {
      setState(state);
    });

    return () => {
      subscription.unsubscribe();
    }
  });

  const sourcesList = sources?.map((source, i) => {
    return <button className="btn btn-success" key={i} onClick={() => selectSource(source)}>{source}</button>;
  });

  return (
    <>
      {isRunning ?
        <button className="btn btn-error" onClick={() => stop()}>Stop Streaming</button> :
        <>
          <button className="btn btn-primary" onClick={() => load()}>Refresh</button>
          {sourcesList ? <>
            {sourcesList}
          </> : null}
        </>
      }

      <br/>

      { state.socketStarted ?
        <button className="btn btn-error" onClick={() => stopSocket()}>Stop Socket</button> :
        <button className="btn btn-success" onClick={() => startSocket()}>Start Socket</button>
      }

      <br/>

      { state.controllerLoopStarted ?
        <button className="btn btn-error" onClick={() => stopGamepad()}>Stop Controller Loop</button> :
        <button className="btn btn-success" onClick={() => startGamepad()}>Start Controller Loop</button>
      }

      <br/>

      {state.editPresets ?
        <p>Editing Presets</p> :
        null
      }
      <p>Preset group: {state.presetGroup}</p>
      {state.ptzIsSupported ?
        <p>PTZ is supported!</p> :
        null
      }


    </>
  );
}