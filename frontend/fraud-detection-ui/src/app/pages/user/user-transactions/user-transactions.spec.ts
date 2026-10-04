import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { UserTransactions } from './user-transactions';

describe('UserTransactions', () => {
  let component: UserTransactions;
  let fixture: ComponentFixture<UserTransactions>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserTransactions],
      providers: [provideHttpClient(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(UserTransactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load cardholder transactions', () => {
    expect(component).toBeTruthy();
    expect(component.transactions.length).toBeGreaterThan(0);
    expect(component.filteredTransactions.length).toBeGreaterThan(0);
  });

  it('should filter transactions by search query', () => {
    component.search = 'Hyderabad';
    const filtered = component.filteredTransactions;
    expect(filtered.every(tx => tx.location.includes('Hyderabad') || tx.id.includes('Hyderabad'))).toBe(true);
  });
});
