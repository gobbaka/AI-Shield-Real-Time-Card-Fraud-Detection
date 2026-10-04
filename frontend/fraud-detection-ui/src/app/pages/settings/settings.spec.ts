import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Settings } from './settings';

describe('Settings', () => {
  let component: Settings;
  let fixture: ComponentFixture<Settings>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Settings],
    }).compileComponents();

    fixture = TestBed.createComponent(Settings);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('resets settings to defaults', () => {
    component.settings.threshold = 95;
    component.reset();
    expect(component.settings.threshold).toBe(80);
  });
});
