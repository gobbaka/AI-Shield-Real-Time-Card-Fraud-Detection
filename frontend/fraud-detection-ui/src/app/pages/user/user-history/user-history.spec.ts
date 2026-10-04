import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { UserHistory } from './user-history';

describe('UserHistory', () => {
  let component: UserHistory;
  let fixture: ComponentFixture<UserHistory>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserHistory],
      providers: [provideHttpClient(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(UserHistory);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load verification history ledger', () => {
    expect(component).toBeTruthy();
    expect(component.historyList.length).toBeGreaterThan(0);
    expect(component.historyList[0].decision).toBeDefined();
  });
});
