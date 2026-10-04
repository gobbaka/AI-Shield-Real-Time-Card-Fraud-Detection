import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FraudAlertComponent } from './fraud-alert';

describe('FraudAlertComponent', () => {
  let component: FraudAlertComponent;
  let fixture: ComponentFixture<FraudAlertComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FraudAlertComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FraudAlertComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('updates alert status through the shared data service', () => {
    const alert = component.alerts[0];
    component.setStatus(alert, 'Resolved');
    expect(alert.status).toBe('Resolved');
  });
});
