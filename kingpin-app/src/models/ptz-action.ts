
export class PtzActionError extends Error {
  constructor(message: string) {
    super();
    this.message = `PtzActionError: ${message}`
  }
}

function assert_f32(value: number) {
  if (value > 3.40282347E+38 || value < -3.40282347E+38) {
    throw new PtzActionError(`value ${value} must conform to f32 bounds`);
  }
}

function assert_u32(value: number) {
  if (!Number.isInteger(value) || value < 0 || value > 4_294_967_295) {
    throw new PtzActionError(`value ${value} must conform to u32 bounds`);
  }
}

function assert_i32(value: number) {
  if (!Number.isInteger(value) || value > 2_147_483_648 || value < -2_147_483_648) {
    throw new PtzActionError(`value ${value} must conform to i32 bounds`);
  }
}

export enum PtzActionType {
  RECALL_PRESET = "RecallPreset",
  ZOOM = "Zoom",
  ZOOM_SPEED = "ZoomSpeed",
  PAN_TILT = "PanTilt",
  PAN_TILT_SPEED = "PanTiltSpeed",
  STORE_PRESET = "StorePreset",
  AUTO_FOCUS = "AutoFocus",
  FOCUS = "Focus",
  FOCUS_SPEED = "FocusSpeed",
  WHITE_BALANCE_AUTO = "WhiteBalanceAuto",
  WHITE_BALANCE_INDOOR = "WhiteBalanceIndoor",
  WHITE_BALANCE_OUTDOOR = "WhiteBalanceOutdoor",
  WHITE_BALANCE_ONESHOT = "WhiteBalanceOneshot",
  WHITE_BALANCE_MANUAL = "WhiteBalanceManual",
  EXPOSURE_AUTO = "ExposureAuto",
  EXPOSURE_MANUAL = "ExposureManual",
  EXPOSURE_MANUAL_V2 = "ExposureManualV2",
}

export abstract class PtzAction {
  abstract readonly type: PtzActionType;

  protected abstract data?: unknown

  serde(): any {
    if (typeof this.data === 'undefined') {
      return this.type;
    }
    return {
      [this.type]: this.data
    }
  }
}

interface IRecallPreset {
  // u32
  preset: number,
  // f32
  speed: number
}

export class RecallPreset extends PtzAction {
  readonly type = PtzActionType.RECALL_PRESET;

  constructor(protected data: IRecallPreset) {
    super();
    assert_u32(data.preset);
    assert_f32(data.speed);
  }
}

interface IZoom {
  // f32
  value: number
}

export class Zoom extends PtzAction {
  readonly type = PtzActionType.ZOOM;

  constructor(protected data: IZoom) {
    super();
  }
}

interface IZoomSpeed {
  // f32
  speed: number
}

export class ZoomSpeed extends PtzAction {
  readonly type = PtzActionType.ZOOM_SPEED;

  constructor(protected data: IZoomSpeed) {
    super();
  }
}

interface IPanTilt {
  // f32
  pan: number,
  // f32
  tilt: number
}

export class PanTilt extends PtzAction {
  readonly type = PtzActionType.PAN_TILT;

  constructor(protected data: IPanTilt) {
    super();
    assert_f32(data.pan);
    assert_f32(data.tilt);
  }
}

interface IPanTiltSpeed {
  // f32
  pan_speed: number,
  // f32
  tilt_speed: number
}

export class PanTiltSpeed extends PtzAction {
  readonly type = PtzActionType.PAN_TILT_SPEED;

  constructor(protected data: IPanTiltSpeed) {
    super();
    assert_f32(data.pan_speed);
    assert_f32(data.tilt_speed);
  }
}

interface IStorePreset {
  // i32
  preset_no: number
}

export class StorePreset extends PtzAction {
  readonly type = PtzActionType.STORE_PRESET;

  constructor(protected data: IStorePreset) {
    super();
    assert_i32(data.preset_no);
  }
}

export class AutoFocus extends PtzAction {
  readonly type = PtzActionType.AUTO_FOCUS;

  protected data: undefined;
}

type IFocus = IZoom;

export class Focus extends PtzAction {
  readonly type = PtzActionType.FOCUS;

  constructor(protected data: IFocus) {
    super();
    assert_f32(data.value);
  }
}

type IFocusSpeed = IZoomSpeed;

export class FocusSpeed extends PtzAction {
  readonly type = PtzActionType.FOCUS_SPEED;

  constructor(protected data: IFocusSpeed) {
    super();
    assert_f32(data.speed);
  }
}

export class WhiteBalanceAuto extends PtzAction {
  readonly type = PtzActionType.WHITE_BALANCE_AUTO;

  protected data: undefined;
}

export class WhiteBalanceIndoor extends PtzAction {
  readonly type = PtzActionType.WHITE_BALANCE_INDOOR;

  protected data: undefined;
}

export class WhiteBalanceOutdoor extends PtzAction {
  readonly type = PtzActionType.WHITE_BALANCE_OUTDOOR;

  protected data: undefined;
}

export class WhiteBalanceOneshot extends PtzAction {
  readonly type = PtzActionType.WHITE_BALANCE_ONESHOT;

  protected data: undefined;
}

interface IWhiteBalanceManual {
  // f32
  red: number,
  // f32
  blue: number,
}

export class WhiteBalanceManual extends PtzAction {
  readonly type = PtzActionType.WHITE_BALANCE_MANUAL;

  constructor(protected data: IWhiteBalanceManual) {
    super();
    assert_f32(data.red);
    assert_f32(data.blue);
  }
}

export class ExposureAuto extends PtzAction {
  readonly type = PtzActionType.EXPOSURE_AUTO;

  protected data: undefined;
}

interface IExposureManual {
  // f32
  level: number
}

export class ExposureManual extends PtzAction {
  readonly type = PtzActionType.EXPOSURE_MANUAL;

  constructor(protected data: IExposureManual) {
    super();
    assert_f32(data.level);
  }
}

interface IExposureManualV2 {
  // f32
  iris: number
  // f32
  gain: number
  // f32
  shutterSpeed: number
}

export class ExposureManualV2 extends PtzAction {
  readonly type = PtzActionType.EXPOSURE_MANUAL_V2;

  constructor(protected data: IExposureManualV2) {
    super();
    assert_f32(data.iris);
    assert_f32(data.gain);
    assert_f32(data.shutterSpeed);
  }
}

export type PtzActions = RecallPreset
  | Zoom
  | ZoomSpeed
  | PanTilt
  | PanTiltSpeed
  | StorePreset
  | AutoFocus
  | Focus
  | FocusSpeed
  | WhiteBalanceAuto
  | WhiteBalanceIndoor
  | WhiteBalanceOutdoor
  | WhiteBalanceOneshot
  | WhiteBalanceManual
  | ExposureAuto
  | ExposureManual
  | ExposureManualV2;