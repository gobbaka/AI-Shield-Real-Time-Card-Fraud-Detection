import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { UserDashboard } from './user-dashboard';

describe('UserDashboard', () => {
  let component: UserDashboard;
  let fixture: ComponentFixture<UserDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserDashboard],
      providers: [provideHttpClient(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(UserDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load user cards and security score', () => {
    expect(component).toBeTruthy();
    expect(component.securityScore).toBe(96);
    expect(component.cards.length).toBeGreaterThan(0);
    expect(component.activeCardsCount).toBeGreaterThan(0);
    expect(component.totalAvailableCredit).toBeGreaterThan(0);
  });
});
