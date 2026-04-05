import { Injectable } from '@angular/core';
import { merge, Subject, tap } from 'rxjs';
import {
  AutoFocus,
  ExposureAuto,
  Focus,
  GamepadRef,
  getGamepadRef,
  PanTiltSpeed,
  PtzAction,
  PtzActionType,
  RecallPreset,
  StorePreset,
  WhiteBalanceOneshot,
  ZoomSpeed,
} from '../models';

const presetGroups = {
  one: [200, 201, 202, 203],
  two: [204, 205, 206, 207],
  three: [208, 209, 210, 211],
  four: [212, 213, 214, 215],
};

const focus_v_interval = 0.005;
const focus_vs = [ 0, 0, 0, 0 ];

const GAMEPAD_SETTINGS_KEY = 'gamepad_settings';
const WEB_INTERFACE_SETTINGS_KEY = 'web_interface_settings';

export type PresetGroup = 'one' | 'two' | 'three' | 'four';

export type PresetIndex = 0 | 1 | 2 | 3;

export type Direction = 'up' | 'down' | 'left' | 'right' | 'in' | 'out';

export class GamepadSettings {
  constructor(
    public presetSpeed: number = 0.25,
    public presetGroup: PresetGroup = 'one',
    public lsHorSens: number = 0.5,
    public lsVertSens: number = 0.5,
    public rsHorSens: number = 0.15,
    public rsVertSens: number = 0.05,
    public invertLsHor: number = 1,
    public invertLsVert: number = 1,
    public invertRsHor: number = -1,
    public invertRsVert: number = -1,
    public focus_v: number = 0,
    public deadzone_pct: number = 0.075,
  ) {}
}

enum MessageClass {
  Primary = 'alert-primary',
  Secondary = 'alert-secondary',
  Success = 'alert-success',
  Info = 'alert-info',
  Warning = 'alert-warning',
  Error = 'alert-error',
}

export class WebInterfaceSettings {
  constructor(
    public invertPan: number = 1,
    public invertTilt: number = 1,
    public invertZoom: number = 1,
    public showBlankTrack: boolean = true,
  ) {}
}

export class WebInterfaceState {

}

export class PtzHolderState {
  constructor(
    public editPresets = false,
    public gamepadLoopStarted = false,
    public socketStarted = false,
    public ptzIsSupported = false,
    public sources = new Array<string>(),
    public selectedSource: string | undefined = undefined,
    public streamRunning = false,
    public gamepadConnected = false,
    public frame: string = '',
  ) {}
}

export class PtzMessage {
  constructor(public message: string, public messageClass: MessageClass) {}
}

@Injectable({
  providedIn: 'root'
})
export class PtzService {

  presetGroups = presetGroups;
  gamepadRef?: GamepadRef;
  gamepad?: Gamepad;
  interval?: any;
  buttonState: boolean[] = new Array(24);
  axisZeroed: boolean[] = new Array(10);
  messageSubject = new Subject<PtzMessage>();
  gamepadSettings = new GamepadSettings();
  webInterfaceState = new WebInterfaceState();
  webInterfaceSettings = new WebInterfaceSettings();
  ptzState = new PtzHolderState();
  gamepadSettingsUpdated = new Subject<GamepadSettings>();
  webInterfaceStateUpdated = new Subject<WebInterfaceState>();
  webInterfaceSettingsUpdated = new Subject<WebInterfaceSettings>();
  ptzStateUpdated = new Subject<PtzHolderState>();

  socket?: WebSocket;

  onSocketMessage = (event: MessageEvent<any>) => {
    const data = JSON.parse(event.data);
    if (data.Ptz) {
      this.ptzIsSupported = data.Ptz.is_supported;
    } else if (data.Sources) {
      this.sources = data.Sources.sources;
    } else if (data.Running) {
      this.streamRunning = data.Running.is_running;
    } else if (data.SelectedSource) {
      this.selectedSource = data.SelectedSource.source;
    } else if (data.Frame) {
      this.frame = data.Frame.data;
    }
  }

  constructor() {
    window.addEventListener('gamepadconnected', this.connected());
    window.addEventListener('gamepaddisconnected', this.disconnected());

    let storedGamepadSettings: any = localStorage.getItem(GAMEPAD_SETTINGS_KEY);
    if (storedGamepadSettings) {
      try {
        storedGamepadSettings = JSON.parse(storedGamepadSettings);
        this.gamepadSettings = new GamepadSettings(...Object.values(storedGamepadSettings) as any);
        this.gamepadSettingsUpdated.next(this.gamepadSettings);
      } catch (e) {
        console.error(e);
      }
    } else {
      localStorage.setItem(GAMEPAD_SETTINGS_KEY, JSON.stringify(this.gamepadSettings));
    }

    let storedWebInterfaceSettings: any = localStorage.getItem(WEB_INTERFACE_SETTINGS_KEY);
    if (storedWebInterfaceSettings) {
      try {
        storedWebInterfaceSettings = JSON.parse(storedWebInterfaceSettings);
        this.webInterfaceSettings = new WebInterfaceSettings(...Object.values(storedWebInterfaceSettings) as any);
        this.webInterfaceSettingsUpdated.next(this.webInterfaceSettings);
        console.log(storedWebInterfaceSettings, JSON.stringify(this.webInterfaceSettings));
      } catch (e) {
        console.error(e);
      }
    } else {
      localStorage.setItem(WEB_INTERFACE_SETTINGS_KEY, JSON.stringify(this.webInterfaceSettings));
    }

    merge(
      this.gamepadSettingsUpdated,
      this.webInterfaceSettingsUpdated,
    )
      .pipe(
        tap(settings => {
          console.log(settings);
          if (settings instanceof GamepadSettings) {
            localStorage.setItem(GAMEPAD_SETTINGS_KEY, JSON.stringify(settings));
          } else if (settings instanceof WebInterfaceSettings) {
            localStorage.setItem(WEB_INTERFACE_SETTINGS_KEY, JSON.stringify(settings));
          }
        })
      ).subscribe();
  }

  startSocket() {
    this.socket = new WebSocket('/api/ndi/ws');
    this.socket.onopen = () => {
      console.log('PTZ socket connected');
      this.handleMessage('PTZ socket connected!', MessageClass.Success);
    };
    this.socket.onmessage = this.onSocketMessage;
    this.socket.onclose = () => {
      const reinit = () => {
        this.startSocket();
      };
      this.handleMessage('PTZ socket disconnected unexpectedly, reinitializing...', MessageClass.Warning);
      setTimeout(reinit, 2000);
    };
    this.socketStarted = true;
  }

  connected() {
    return ({ gamepad }: { gamepad: Gamepad }) => {
      this.gamepad = gamepad;
      this.gamepadRef = getGamepadRef(gamepad);
      this.gamepadConnected = true;
    };
  }

  async stopSocket() {
    console.log('PTZ socket disconnected');
    if (this.socket) {
      this.socket.onclose = () => {
      };
      this.socket.close();
      this.frame = '';
      this.socketStarted = false;
    }
    await this.stopGamepadLoop();
    this.handleMessage('PTZ socket stopped', MessageClass.Info);
  }

  sendActions(...actions: PtzAction[]) {
    if (this.socket && actions.length > 0) {
      this.socket.send(JSON.stringify({
        Actions: {
          actions: actions.map(a => a.serde()),
        },
      }));
      return true;
    } else if (!this.socket) {
      console.error("Unable to send actions to server: Socket has not been initialized");
      return false;
    } else {
      return false;
    }
  }

  sendAndNotify(action: PtzAction) {
    const result = this.sendActions(action);
    if (result) {
      if (action instanceof RecallPreset) {
        this.handleMessage(`Recalled preset ${action.preset.preset}`, MessageClass.Info);
      } else if (action instanceof StorePreset) {
        this.handleMessage(`Stored preset ${action.preset.preset_no}`, MessageClass.Info);
      } else {
        this.handleMessage(`${action.type} sent!`, MessageClass.Info);
      }
    } else {
      this.handleMessage(`${action.type} could not be sent`, MessageClass.Warning);
    }
  }

  refreshSources() {
    if (this.socket) {
      this.socket.send(JSON.stringify("Refresh"));
    }
  }

  selectSource(source: string) {
    if (this.socket) {
      this.socket.send(JSON.stringify({
        SelectSource: {
          source
        }
      }));
    }
  }

  stopStream() {
    if (this.socket) {
      this.socket.send(JSON.stringify("Stop"));
    }
  }

  applyDeadzone(v: number): number {
    return Math.abs(v) < this.deadzone_pct ? 0 : v > 0 ? v - this.deadzone_pct : v + this.deadzone_pct;
  }

  async startGamepadLoop() {
    const intervalMethod = async () => {
      const actions: PtzAction[] = [];
      if (this.gamepad && this.gamepadRef) {
        const { axisRef, buttonRef } = this.gamepadRef;
        const { axes, buttons } = this.gamepad;
      
        const b_a = buttons[buttonRef.a].touched;
        const b_b = buttons[buttonRef.b].touched;
        const b_y = buttons[buttonRef.y].touched;
        const b_x = buttons[buttonRef.x].touched;
        const b_d_up = buttons[buttonRef.d_up].touched;
        const b_d_right = buttons[buttonRef.d_right].touched;
        const b_d_down = buttons[buttonRef.d_down].touched;
        const b_d_left = buttons[buttonRef.d_left].touched;
        const b_rs = buttons[buttonRef.rs].touched;
        const b_rb = buttons[buttonRef.rb].touched;
        const b_lb = buttons[buttonRef.lb].touched;
        const b_select = buttons[buttonRef.select].touched;
        
        const a_lsHor = this.applyDeadzone(axes[axisRef.ls_hor]);
        const a_lsVert = this.applyDeadzone(axes[axisRef.ls_vert]);
        const a_rsHor = this.applyDeadzone(axes[axisRef.rs_hor]);
        const a_rsVert = this.applyDeadzone(axes[axisRef.rs_vert]);
        const a_lt = axes[axisRef.lt];

        // pan/tilt
        if (a_rsHor == 0 && !this.axisZeroed[axisRef.rs_hor] ||
          a_rsVert == 0 && !this.axisZeroed[axisRef.rs_vert]) {
          actions.push(new PanTiltSpeed({
            pan_speed: a_rsHor * this.invertRsHor,
            tilt_speed: a_rsVert * this.invertRsVert,
          }));
          this.axisZeroed[axisRef.rs_hor] = a_rsHor == 0;
          this.axisZeroed[axisRef.rs_vert] = a_rsVert == 0;
        }
        if (a_rsHor > 0.0001 || a_rsHor < -0.0001 || a_rsVert > 0.0001 || a_rsVert < -0.0001) {
          actions.push(new PanTiltSpeed({
            pan_speed: (a_rsHor * this.invertRsHor) * this.rsHorSens,
            tilt_speed: (a_rsVert * this.invertRsVert) * this.rsVertSens,
          }));
          this.axisZeroed[axisRef.rs_hor] = a_rsHor == 0;
          this.axisZeroed[axisRef.rs_vert] = a_rsVert == 0;
        }
        // zoom
        if (a_lsVert == 0 && !this.axisZeroed[axisRef.ls_vert]) {
          actions.push(new ZoomSpeed({ speed: a_lsVert * this.invertLsVert }));
          this.axisZeroed[axisRef.ls_vert] = a_lsVert == 0;
        }
        if (a_lsVert > 0.0001 || a_lsVert < -0.0001) {
          actions.push(new ZoomSpeed({ speed: a_lsVert * this.invertLsVert }));
          this.axisZeroed[axisRef.ls_vert] = a_lsVert == 0;
        }

        // a
        if (b_a != this.buttonState[buttonRef.a]) {
          if (b_a) {
            actions.push(new AutoFocus());
            // actions.push(this.presetAction(0));
          }
          this.buttonState[buttonRef.a] = b_a;
        }

        // b
        if (b_b != this.buttonState[buttonRef.b]) {
          if (b_b) {
            actions.push(new WhiteBalanceOneshot());
            // actions.push(this.presetAction(1));
          }
          this.buttonState[buttonRef.b] = b_b;
        }

        // y
        if (b_y != this.buttonState[buttonRef.y]) {
          if (b_y) {
            actions.push(this.presetAction(2));
          }
          this.buttonState[buttonRef.y] = b_y;
        }

        // x
        if (b_x != this.buttonState[buttonRef.x]) {
          if (b_x) {
            actions.push(this.presetAction(3));
          }
          this.buttonState[buttonRef.x] = b_x;
        }

        // d_up
        if (b_d_up != this.buttonState[buttonRef.d_up]) {
          if (b_d_up) {
            if (this.editPresets) {
              focus_vs[0] = this.focus_v;
            } else {
              actions.push(new Focus({ value: focus_vs[0] }));
            }
          }
          this.buttonState[buttonRef.d_up] = b_d_up;
        }

        // d_right
        if (b_d_right != this.buttonState[buttonRef.d_right]) {
          if (b_d_right) {
            this.presetGroup = 'two';
          }
          this.buttonState[buttonRef.d_right] = b_d_right;
        }

        // d_down
        if (b_d_down != this.buttonState[buttonRef.d_down]) {
          if (b_d_down) {
            this.presetGroup = 'three';
          }
          this.buttonState[buttonRef.d_down] = b_d_down;
        }

        // d_left
        if (b_d_left != this.buttonState[buttonRef.d_left]) {
          if (b_d_left) {
            this.presetGroup = 'four';
          }
          this.buttonState[buttonRef.d_left] = b_d_left;
        }

        // rs
        if (b_rs != this.buttonState[buttonRef.rs]) {
          if (b_rs) {
            actions.push(new ExposureAuto());
          }
          this.buttonState[buttonRef.rs] = b_rs;
        }

        // rb
        // if (b_rb != this.buttonState[buttonRef.rb]) {
        //   if (b_rb) {
        //     // actions.push(new Focus({ value: a_ }));
        //   }
        //   this.buttonState[buttonRef.rb] = b_rb;
        // }

        // focus
        // if (b_lb != this.buttonState[buttonRef.lb]) {
          if (b_lb) {
            this.focus_v = this.focus_v - focus_v_interval;
            actions.push(new Focus({ value: this.focus_v }));
          }
          if (b_rb) {
            this.focus_v = this.focus_v + focus_v_interval;
            actions.push(new Focus({ value: this.focus_v }));
          }
          // this.buttonState[buttonRef.lb] = b_lb;
        // }

        // select
        if (b_select != this.buttonState[buttonRef.select]) {
          if (b_select) {
            this.editPresets = !this.editPresets;
          }
          this.buttonState[buttonRef.select] = b_select;
        }
      } else {
        // console.warn("Gamepad or GamepadRef is missing", this.gamepad, this.gamepadRef);
      }
      this.sendActions(...actions);
    };
    this.interval = setInterval(intervalMethod, 100);
    this.controllerLoopStarted = true;
  }

  async stopGamepadLoop() {
    clearInterval(this.interval);
    this.controllerLoopStarted = false;
  }

  presetAction(index: PresetIndex): StorePreset | RecallPreset {
    if (this.editPresets) {
      return new StorePreset({ preset_no: presetGroups[this.presetGroup][index] });
    } else {
      return new RecallPreset({ preset: presetGroups[this.presetGroup][index], speed: this.presetSpeed });
    }
  }

  disconnected() {
    return ({ gamepad }: { gamepad: Gamepad }) => {
      console.warn("Gamepad disconnected", gamepad);
      this.gamepadConnected = false;
    };
  }

  handleMessage(message: string, messageClass: MessageClass) {
    this.messageSubject.next(new PtzMessage(message, messageClass));
  }

  get presetSpeed(): number {
    return this.gamepadSettings.presetSpeed;
  }

  set presetSpeed(value: number) {
    this.gamepadSettings.presetSpeed = value;
  }

  get presetGroup(): PresetGroup {
    return this.gamepadSettings.presetGroup;
  }

  set presetGroup(value: PresetGroup) {
    this.gamepadSettings.presetGroup = value;
  }

  get editPresets(): boolean {
    return this.ptzState.editPresets;
  }

  set editPresets(value: boolean) {
    this.ptzState.editPresets = value;
    this.ptzStateUpdated.next(this.ptzState);
  }

  get socketStarted(): boolean {
    return this.ptzState.socketStarted;
  }

  set socketStarted(value: boolean) {
    this.ptzState.socketStarted = value;
    this.ptzStateUpdated.next(this.ptzState);
  }

  get controllerLoopStarted(): boolean {
    return this.ptzState.gamepadLoopStarted;
  }

  set controllerLoopStarted(value: boolean) {
    this.ptzState.gamepadLoopStarted = value;
    this.ptzStateUpdated.next(this.ptzState);
  }

  get ptzIsSupported(): boolean {
    return this.ptzState.ptzIsSupported;
  }

  set ptzIsSupported(value: boolean) {
    this.ptzState.ptzIsSupported = value;
    this.ptzStateUpdated.next(this.ptzState);
  }

  get lsHorSens(): number {
    return this.gamepadSettings.lsHorSens;
  }

  get lsVertSens(): number {
    return this.gamepadSettings.lsVertSens;
  }

  get rsHorSens(): number {
    return this.gamepadSettings.rsHorSens;
  }

  get rsVertSens(): number {
    return this.gamepadSettings.rsVertSens;
  }

  get invertLsHor(): number {
    return this.gamepadSettings.invertLsHor;
  }

  get invertLsVert(): number {
    return this.gamepadSettings.invertLsVert;
  }

  get invertRsHor(): number {
    return this.gamepadSettings.invertRsHor;
  }

  get invertRsVert(): number {
    return this.gamepadSettings.invertRsVert;
  }

  get sources(): string[] {
    return this.ptzState.sources;
  }

  set sources(sources: string[]) {
    this.ptzState.sources = sources;
    this.ptzStateUpdated.next(this.ptzState);
  }

  get streamRunning(): boolean {
    return this.ptzState.streamRunning;
  }

  set streamRunning(running: boolean) {
    this.ptzState.streamRunning = running;
    this.ptzStateUpdated.next(this.ptzState);
  }

  get selectedSource(): string | undefined {
    return this.ptzState.selectedSource;
  }

  set selectedSource(source: string | undefined) {
    this.ptzState.selectedSource = source;
    this.ptzStateUpdated.next(this.ptzState);
  }

  get gamepadConnected(): boolean {
    return this.ptzState.gamepadConnected;
  }

  set gamepadConnected(connected: boolean) {
    this.ptzState.gamepadConnected = connected;
    this.ptzStateUpdated.next(this.ptzState);
  }

  get frame(): string {
    return this.ptzState.frame ?? '';
  }

  set frame(frame: string) {
    this.ptzState.frame = frame;
    this.ptzStateUpdated.next(this.ptzState);
  }

  get focus_v(): number {
    return this.gamepadSettings.focus_v;
  }

  set focus_v(v: number) {
    this.gamepadSettings.focus_v = v;
    this.gamepadSettingsUpdated.next(this.gamepadSettings);
  }

  get deadzone_pct(): number {
    return this.gamepadSettings.deadzone_pct;
  }

  set deadzone_pct(pct: number) {
    this.gamepadSettings.deadzone_pct = pct;
    this.gamepadSettingsUpdated.next(this.gamepadSettings);
  }
}
