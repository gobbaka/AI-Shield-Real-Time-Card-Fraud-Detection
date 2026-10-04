import { FraudDataService } from './fraud-data.service';

describe('FraudDataService', () => {
  const mockHttp = {
    get: () => ({ pipe: () => ({ subscribe: () => {} }) }),
    patch: () => ({ pipe: () => ({ subscribe: () => {} }) }),
    put: () => ({ pipe: () => ({ subscribe: () => {} }) }),
    post: () => ({ pipe: () => ({ subscribe: () => {} }) })
  } as any;

  const mockNotification = {
    sendFraudAlert: () => {}
  } as any;

  it('returns analytics derived from current transaction and alert state', () => {
    const service = new FraudDataService(mockHttp, mockNotification);

    expect(service.getAnalytics().totalAmount).toBe(566300);
    expect(service.getAnalytics().distribution).toEqual({ approved: 3, review: 2, blocked: 2 });
    expect(service.getAnalytics().fraudCount).toBe(3);
  });

  it('updates an alert and its linked transaction status', () => {
    const service = new FraudDataService(mockHttp, mockNotification);

    service.updateAlert('FA1024', 'Blocked');

    expect(service.getAlerts().find(alert => alert.id === 'FA1024')?.status).toBe('Blocked');
    expect(service.getTransactions().find(transaction => transaction.id === '#TX10002')?.status).toBe('Blocked');
  });

  it('persists and resets settings', () => {
    const service = new FraudDataService(mockHttp, mockNotification);
    const settings = { ...service.getSettings(), darkMode: true, threshold: 65 };

    service.saveSettings(settings);
    expect(service.getSettings()).toEqual(settings);
    expect(service.resetSettings()).toEqual({ darkMode: false, notifications: true, aiDetection: true, autoBlock: false, threshold: 80 });
  });
});