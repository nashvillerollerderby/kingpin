import {
  AutoFocus,
  ExposureAuto,
  PanTiltSpeed,
  PtzAction,
  RecallPreset,
  StorePreset,
  WhiteBalanceAuto, WhiteBalanceOneshot,
  ZoomSpeed,
} from '../models';
import { GamepadRef, getGamepadRef } from '../models/gamepad-models.ts';
import { BehaviorSubject } from 'rxjs';

const presetGroups = {
  one: [200, 201, 202, 203],
  // one: [5464, 5465, 5466, 5467],
  two: [204, 205, 206, 207],
  three: [208, 209, 210, 211],
  four: [5476, 5477, 5478, 5479],
};

type PresetGroup = 'one' | 'two' | 'three' | 'four';

class GamepadHolderState {
  presetSpeed: number = 0.25;
  presetGroup: PresetGroup = 'one';
  editPresets = false;
  socketStarted = false;
  controllerLoopStarted = false;
  ptzIsSupported = false;
}

class GamepadHolder {

  gamepadRef?: GamepadRef;
  gamepad?: Gamepad;
  interval?: number;
  buttonState: boolean[] = new Array(24);
  axisZeroed: boolean[] = new Array(10);
  stateSubject = new BehaviorSubject<GamepadHolderState>(new GamepadHolderState());

  socket?: WebSocket;

  lsHorSens = 0.5;
  lsVertSense = 0.2;
  invertLsHor = 1;
  invertLsVert = 1;
  invertRsHor = -1;
  invertRsVert = -1;

  get presetSpeed(): number {
    return this.stateSubject.value.presetSpeed;
  }

  set presetSpeed(value: number) {
    this.stateSubject.next({
      ...this.stateSubject.value,
      presetSpeed: value
    });
  }

  get presetGroup(): PresetGroup {
    return this.stateSubject.value.presetGroup;
  }

  set presetGroup(value: PresetGroup) {
    this.stateSubject.next({
      ...this.stateSubject.value,
      presetGroup: value
    });
  }

  get editPresets(): boolean {
    return this.stateSubject.value.editPresets;
  }

  set editPresets(value: boolean) {
    this.stateSubject.next({
      ...this.stateSubject.value,
      editPresets: value
    });
  }

  get socketStarted(): boolean {
    return this.stateSubject.value.socketStarted;
  }

  set socketStarted(value: boolean) {
    this.stateSubject.next({
      ...this.stateSubject.value,
      socketStarted: value
    });
  }

  get controllerLoopStarted(): boolean {
    return this.stateSubject.value.controllerLoopStarted;
  }

  set controllerLoopStarted(value: boolean) {
    this.stateSubject.next({
      ...this.stateSubject.value,
      controllerLoopStarted: value
    });
  }

  get ptzIsSupported(): boolean {
    return this.stateSubject.value.ptzIsSupported;
  }

  set ptzIsSupported(value: boolean) {
    console.log('set ptzIsSupported', value);
    this.stateSubject.next({
      ...this.stateSubject.value,
      ptzIsSupported: value
    })
  }

  constructor() {
    window.addEventListener('gamepadconnected', this.connected());
    window.addEventListener('gamepaddisconnected', this.disconnected());
  }

  initSocket() {
    this.socket = new WebSocket('/api/ndi/ws');
    this.socket.onopen = () => {
      console.log('PTZ socket connected');
    };
    this.socket.onmessage = event => {
      const data = JSON.parse(event.data);
      console.log(data);
      if (data.Ptz) {
        this.ptzIsSupported = data.Ptz.is_supported
      }
    };
    this.socket.onclose = () => {
      const reinit = () => {
        this.initSocket();
      };
      setTimeout(reinit, 2000);
    };
    this.socketStarted = true;
  }

  connected() {
    return ({ gamepad }: { gamepad: Gamepad }) => {
      console.log('Gamepad connected');
      this.gamepad = gamepad;
      this.gamepadRef = getGamepadRef(gamepad);
    };
  }

  startSocket() {
    this.initSocket();
  }

  stopSocket() {
    if (this.socket) {
      this.socket.onclose = () => {
      };
      this.socket.close();
      this.socketStarted = false;
    }
  }

  async startControllerLoop() {
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
        const a_lsHor = axes[axisRef.ls_hor];
        const a_lsVert = axes[axisRef.ls_vert];
        const a_rsVert = axes[axisRef.rs_vert];

        // left stick
        if (a_lsHor == 0 && !this.axisZeroed[axisRef.ls_hor] ||
          a_lsVert == 0 && !this.axisZeroed[axisRef.ls_vert]) {
          actions.push(new PanTiltSpeed({
            pan_speed: a_lsHor * this.invertLsHor,
            tilt_speed: a_lsVert * this.invertLsVert,
          }));
          this.axisZeroed[axisRef.ls_hor] = a_lsHor == 0;
          this.axisZeroed[axisRef.ls_vert] = a_lsVert == 0;
        }
        if (a_lsHor > 0.0001 || a_lsHor < -0.0001 || a_lsVert > 0.0001 || a_lsVert < -0.0001) {
          actions.push(new PanTiltSpeed({
            pan_speed: (a_lsHor * this.invertLsHor) * this.lsHorSens,
            tilt_speed: (a_lsVert * this.invertLsVert) * this.lsVertSense,
          }));
          this.axisZeroed[axisRef.ls_hor] = a_lsHor == 0;
          this.axisZeroed[axisRef.ls_vert] = a_lsVert == 0;
        }
        // right stick vertical
        if (a_rsVert == 0 && !this.axisZeroed[axisRef.rs_vert]) {
          actions.push(new ZoomSpeed({ speed: a_rsVert * this.invertRsVert }));
          this.axisZeroed[axisRef.rs_vert] = a_rsVert == 0;
        }
        if (a_rsVert > 0.0001 || a_rsVert < -0.0001) {
          actions.push(new ZoomSpeed({ speed: a_rsVert * this.invertRsVert }));
          this.axisZeroed[axisRef.rs_vert] = a_rsVert == 0;
        }

        // a
        if (b_a != this.buttonState[buttonRef.a]) {
          if (b_a) {
            if (this.editPresets) {
              actions.push(new StorePreset({ preset_no: presetGroups[this.presetGroup][0] }));
            } else {
              actions.push(new RecallPreset({ preset: presetGroups[this.presetGroup][0], speed: this.presetSpeed }));
            }
          }
          this.buttonState[buttonRef.a] = b_a;
        }

        // b
        if (b_b != this.buttonState[buttonRef.b]) {
          if (b_b) {
            if (this.editPresets) {
              actions.push(new StorePreset({ preset_no: presetGroups[this.presetGroup][1] }));
            } else {
              actions.push(new RecallPreset({ preset: presetGroups[this.presetGroup][1], speed: this.presetSpeed }));
            }
          }
          this.buttonState[buttonRef.b] = b_b;
        }

        // y
        if (b_y != this.buttonState[buttonRef.y]) {
          if (b_y) {
            if (this.editPresets) {
              actions.push(new StorePreset({ preset_no: presetGroups[this.presetGroup][2] }));
            } else {
              actions.push(new RecallPreset({ preset: presetGroups[this.presetGroup][2], speed: this.presetSpeed }));
            }
          }
          this.buttonState[buttonRef.y] = b_y;
        }

        // x
        if (b_x != this.buttonState[buttonRef.x]) {
          if (b_x) {
            if (this.editPresets) {
              actions.push(new StorePreset({ preset_no: presetGroups[this.presetGroup][3] }));
            } else {
              actions.push(new RecallPreset({ preset: presetGroups[this.presetGroup][3], speed: this.presetSpeed }));
            }
          }
          this.buttonState[buttonRef.x] = b_x;
        }

        // d_up
        if (b_d_up != this.buttonState[buttonRef.d_up]) {
          if (b_d_up) {
            this.presetGroup = 'one';
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
        if (b_rb != this.buttonState[buttonRef.rb]) {
          if (b_rb) {
            actions.push(new AutoFocus());
          }
          this.buttonState[buttonRef.rb] = b_rb;
        }

        // lb
        if (b_lb != this.buttonState[buttonRef.lb]) {
          if (b_lb) {
            actions.push(new WhiteBalanceOneshot());
          }
          this.buttonState[buttonRef.lb] = b_lb;
        }

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
      if (actions.length > 0) {
        // console.log(JSON.stringify(actions));
        if (this.socket) {
          this.socket.send(JSON.stringify({
            Actions: {
              actions: actions.map(a => a.serde()),
            },
          }));
        }
      }
    };
    this.interval = setInterval(intervalMethod, 100);
    this.controllerLoopStarted = true;
  }

  async stopControllerLoop() {
    clearInterval(this.interval);
    this.controllerLoopStarted = false;
  }

  disconnected() {
    return ({ gamepad }: { gamepad: Gamepad }) => {

    };
  }

}

export const GamepadHolderInstance = new GamepadHolder();