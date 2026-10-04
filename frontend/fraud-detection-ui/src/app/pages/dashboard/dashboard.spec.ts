import { Dashboard } from './dashboard';
import { FraudDataService } from '../../services/fraud-data.service';
import { Router } from '@angular/router';

describe('Dashboard', () => {
  it('derives dashboard metrics and recent rows from the shared data service', () => {
    const component = new Dashboard(new FraudDataService({} as any, {} as any), {} as Router);

    expect(component.transactions.length).toBe(7);
    expect(component.recentTransactions[0].id).toBe('#TX10007');
    expect(component.fraudAlertCount).toBe(3);
    expect(component.safeTransactionRate).toBe(42.9);
    expect(component.trustScore).toBe(47);
  });

  it('navigates to the transactions workflow from View All', () => {
    let destination: string[] = [];
    const router = { navigate: (commands: string[]) => { destination = commands; return Promise.resolve(true); } } as unknown as Router;
    const component = new Dashboard(new FraudDataService({} as any, {} as any), router);

    component.viewAllTransactions();

    expect(destination).toEqual(['/transactions']);
  });
});
