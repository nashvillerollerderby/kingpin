import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import {
  Direction,
  GamepadSettings,
  PresetIndex,
  PtzMessage,
  PtzService,
  WebInterfaceSettings,
} from '../../services/ptz.service';
import { debounceTime, delay, merge, Subject, takeUntil, tap } from 'rxjs';
import { AsyncPipe, NgClass } from '@angular/common';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AutoFocus, Focus, PanTiltSpeed, ZoomSpeed } from '../../models';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-ptz-controls',
  imports: [
    AsyncPipe,
    NgClass,
    FormsModule,
    ReactiveFormsModule,
  ],
  templateUrl: './ptz-controls.component.html',
  styleUrl: './ptz-controls.component.scss'
})
export class PtzControlsComponent implements OnInit, OnDestroy{

  isProduction = environment.production;
  ptzService = inject(PtzService);
  messages = new Array<PtzMessage>();
  directionIntervals: { [key: string]: any} = {};

  $destroyed = new Subject<void>();

  $ptzState = this.ptzService.ptzStateUpdated;
  $messages = this.ptzService.messageSubject;
  $pushMessage = new Subject<PtzMessage>();
  $sendPresetAction = new Subject<PresetIndex>();

  gamepadForm = new FormBuilder().group({
    presetSpeed: new FormControl<number>(this.ptzService.gamepadSettings.presetSpeed),
    presetGroup: new FormControl(this.ptzService.gamepadSettings.presetGroup),
    lsHorSens: new FormControl(this.ptzService.gamepadSettings.lsHorSens),
    lsVertSens: new FormControl(this.ptzService.gamepadSettings.lsVertSens),
    invertLsHor: new FormControl(this.ptzService.gamepadSettings.invertLsHor == -1),
    invertLsVert: new FormControl(this.ptzService.gamepadSettings.invertLsVert == -1),
    invertRsHor: new FormControl(this.ptzService.gamepadSettings.invertRsHor == -1),
    invertRsVert: new FormControl(this.ptzService.gamepadSettings.invertRsVert == -1),
  });

  webInterfaceForm = new FormBuilder().group({
    invertPan: new FormControl(this.ptzService.webInterfaceSettings.invertPan == -1),
    invertTilt: new FormControl(this.ptzService.webInterfaceSettings.invertTilt == -1),
    invertZoom: new FormControl(this.ptzService.webInterfaceSettings.invertZoom == -1),
    showBlankTrack: new FormControl(this.ptzService.webInterfaceSettings.showBlankTrack),
  });

  trackImage = "/assets/blank-derby-track.svg";

  _$gamepadFormUpdated = this.gamepadForm.valueChanges
    .pipe(
      debounceTime(100),
      tap(value => {
        this.ptzService.gamepadSettings.lsHorSens = value.lsHorSens as number;
        this.ptzService.gamepadSettings.lsVertSens = value.lsVertSens as number;
        this.ptzService.gamepadSettings.invertLsHor = value.invertLsHor ? -1 : 1;
        this.ptzService.gamepadSettings.invertLsVert = value.invertLsVert ? -1 : 1;
        this.ptzService.gamepadSettings.invertRsHor = value.invertRsHor ? -1 : 1;
        this.ptzService.gamepadSettings.invertRsVert = value.invertRsVert ? -1 : 1;

        this.ptzService.gamepadSettingsUpdated.next(this.ptzService.gamepadSettings);
      })
    );

  _$webInterfaceFormUpdated = this.webInterfaceForm.valueChanges
    .pipe(
      debounceTime(100),
      tap(value => {
        this.ptzService.webInterfaceSettings.invertPan = value.invertPan ? -1 : 1;
        this.ptzService.webInterfaceSettings.invertTilt = value.invertTilt ? -1 : 1;
        this.ptzService.webInterfaceSettings.invertZoom = value.invertZoom ? -1 : 1;
        this.ptzService.webInterfaceSettings.showBlankTrack = value.showBlankTrack ?? true;

        if (!this.ptzService.webInterfaceSettings.showBlankTrack) {
          this.trackImage = '/assets/derby-track.svg';
        } else {
          this.trackImage = '/assets/blank-derby-track.svg';
        }

        this.ptzService.webInterfaceSettingsUpdated.next(this.ptzService.webInterfaceSettings);
      })
    )

  _$sendPresetAction = this.$sendPresetAction
    .pipe(
      debounceTime(50),
      tap(value => {
        this.ptzService.sendAndNotify(this.ptzService.presetAction(value));
      })
    )

  ngOnInit(): void {
    this.ptzService.startSocket();
    merge(
      this.$ptzState,
      this.$messages,
    ).pipe(
      takeUntil(this.$destroyed)
    ).subscribe(value => {
      if (value instanceof PtzMessage) {
        this.$pushMessage.next(value);
      }
    });

    this.$pushMessage.pipe(
      tap(message => {
        this.messages.push(message)
      }),
      delay(5000),
      takeUntil(this.$destroyed)
    ).subscribe(() => {
      this.messages.shift();
    });

    merge(
      this._$gamepadFormUpdated,
      this._$sendPresetAction,
      this._$webInterfaceFormUpdated,
    ).pipe(takeUntil(this.$destroyed))
      .subscribe();
  }

  async ngOnDestroy(): Promise<void> {
    this.$destroyed.next();
    await this.ptzService.stopSocket();
  }

  sendPresetAction(index: PresetIndex) {
    this.$sendPresetAction.next(index);
  }

  startDirection(direction: Direction) {
    this.directionIntervals[direction] = setInterval(() => {
      switch (direction) {
        case 'up':
          this.ptzService.sendActions(new PanTiltSpeed({ pan_speed: 0, tilt_speed: .1 * this.ptzService.webInterfaceSettings.invertTilt }));
          break;
        case 'down':
          this.ptzService.sendActions(new PanTiltSpeed({ pan_speed: 0, tilt_speed: -.1 * this.ptzService.webInterfaceSettings.invertTilt }));
          break;
        case 'left':
          this.ptzService.sendActions(new PanTiltSpeed({ pan_speed: -.1 * this.ptzService.webInterfaceSettings.invertPan, tilt_speed: 0 }));
          break;
        case 'right':
          this.ptzService.sendActions(new PanTiltSpeed({ pan_speed: .1 * this.ptzService.webInterfaceSettings.invertPan, tilt_speed: 0 }));
          break;
        case 'in':
          this.ptzService.sendActions(new ZoomSpeed({ speed: .1 * this.ptzService.webInterfaceSettings.invertZoom }));
          break;
        case 'out':
          this.ptzService.sendActions(new ZoomSpeed({ speed: -.1 * this.ptzService.webInterfaceSettings.invertZoom }));
          break;
      }
    }, 100);
  }

  focusOneshot() {
    this.ptzService.sendAndNotify(new AutoFocus());
  }

  endDirection(direction: Direction) {
    clearInterval(this.directionIntervals[direction]);
    switch (direction) {
      case 'up':
      case 'down':
      case 'left':
      case 'right':
        this.ptzService.sendActions(new PanTiltSpeed({ pan_speed: 0, tilt_speed: 0 }));
        break;
      case 'in':
      case 'out':
        this.ptzService.sendActions(new ZoomSpeed({ speed: 0 }));
    }
  }

}
