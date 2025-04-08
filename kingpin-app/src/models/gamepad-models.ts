export interface GamepadRef {
  identifier: GamepadIdentifier;
  buttonRef: {
    a: number
    b: number
    y: number
    x: number
    lb: number
    rb: number
    select: number
    start: number
    ls: number
    rs: number
    d_up: number
    d_down: number
    d_left: number
    d_right: number
    rt: number
    lt: number
  };
  axisRef: {
    ls_hor: number
    ls_vert: number
    rs_hor: number
    rs_vert: number
    lt: number
    rt: number
  };
  propButtonRef?: {
    [key: string]: number
  };
  propAxisRef?: {
    [key: string]: number
  };
}

export enum GamepadIdentifier {
  Playstation = 'Playstation',
  Xbox = 'Xbox',
  Stadia = 'Stadia',
}

export const Stadia: GamepadRef = {
  identifier: GamepadIdentifier.Stadia,
  buttonRef: {
    a: 0,
    b: 1,
    y: 2,
    x: 3,
    lb: 4,
    rb: 5,
    select: 8,
    start: 9,
    ls: 10,
    rs: 11,
    d_up: 12,
    d_down: 13,
    d_left: 14,
    d_right: 15,
    rt: 22,
    lt: 23,
  },
  axisRef: {
    ls_hor: 0,
    ls_vert: 1,
    rs_hor: 4,
    rs_vert: 5,
    rt: 6,
    lt: 7,
  },
  propButtonRef: {
    stadia: 16,
    google: 20,
    box: 21,
  },
};

export const getGamepadRef = (gamepad: Gamepad): GamepadRef | undefined => {
  if (gamepad.id.includes(GamepadIdentifier.Stadia)) {
    return Stadia;
  } else {
    return undefined;
  }
};
