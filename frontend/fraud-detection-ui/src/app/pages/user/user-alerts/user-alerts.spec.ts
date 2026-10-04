import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { UserAlerts } from './user-alerts';

describe('UserAlerts', () => {
  let component: UserAlerts;
  let fixture: ComponentFixture<UserAlerts>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserAlerts],
      providers: [provideHttpClient(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(UserAlerts);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load pending alerts', () => {
    expect(component).toBeTruthy();
    expect(component.alerts).toBeDefined();
    expect(component.pendingVerifications).toBeDefined();
  });
});
