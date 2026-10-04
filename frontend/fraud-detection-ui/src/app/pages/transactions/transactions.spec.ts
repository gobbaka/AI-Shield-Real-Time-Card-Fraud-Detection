import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransactionsComponent } from './transactions';

describe('TransactionsComponent', () => {
  let component: TransactionsComponent;
  let fixture: ComponentFixture<TransactionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionsComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('filters transactions by customer and paginates results', () => {
    component.search = 'Priya';
    component.applyFilters();
    expect(component.filteredTransactions).toHaveLength(1);
    expect(component.pagedTransactions[0].customer).toBe('Priya Singh');
  });
});
