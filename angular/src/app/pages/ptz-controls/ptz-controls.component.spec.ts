import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PtzControlsComponent } from './ptz-controls.component';

describe('PtzControlsComponent', () => {
  let component: PtzControlsComponent;
  let fixture: ComponentFixture<PtzControlsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PtzControlsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PtzControlsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
