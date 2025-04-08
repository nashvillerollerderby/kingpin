import { expect, test } from 'vitest';
import { PtzActionType, RecallPreset } from './ptz-action.ts';

test('PtzAction to convert to serde enum representation', () => {
  const action = new RecallPreset({
    preset: 1,
    speed: 5.0
  });

  expect(action.serde()).toEqual({
    [PtzActionType.RECALL_PRESET]: {
      preset: 1,
      speed: 5.0
    }
  });
});