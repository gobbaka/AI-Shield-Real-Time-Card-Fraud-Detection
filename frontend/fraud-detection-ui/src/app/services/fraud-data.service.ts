import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { NotificationService } from './notification.service';

export type TransactionStatus = 
  | 'Approved' 
  | 'Review' 
  | 'Blocked' 
  | 'Verification Required' 
  | 'OWNER_VERIFIED' 
  | 'CUSTOMER_REPORTED_FRAUD' 
  | 'Verification Expired';

export type RiskLevel = 'Low' | 'Medium' | 'High';
export type AlertStatus = 'Open' | 'Reviewing' | 'Resolved' | 'Blocked';

export interface Transaction {
  id: string;
  customer: string;
  card: string;
  amount: number;
  location: string;
  timestamp: string;
  risk: RiskLevel;
  riskScore: number;
  status: TransactionStatus;
  reasons?: string[];
  decision?: string;
  merchantName?: string;
  merchantCategory?: string;
  verificationRequestId?: string;
  verificationStatus?: string;
}

export interface FraudAlert {
  id: string;
  transactionId: string;
  customer: string;
  amount: number;
  riskScore: number;
  location: string;
  status: AlertStatus;
  createdAt: string;
  triggerReasons?: string;
}

export interface Customer {
  name: string;
  email: string;
  phone?: string;
  mobileVerified?: boolean;
  card: string;
  transactions: number;
  fraudScore: number;
  status: RiskLevel;
  locations: string[];
}

export interface TransactionVerification {
  verificationRequestId: string;
  transactionRef: string;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  cardMasked: string;
  amount: number;
  merchantName: string;
  location: string;
  riskLevel: string;
  riskScore: number;
  status: string;
  smsStatus?: string;
  smsContent?: string;
  requestedAt: string;
  expiresAt: string;
  challengeToken: string;
}

export interface FraudSettings {
  darkMode: boolean;
  notifications: boolean;
  aiDetection: boolean;
  autoBlock: boolean;
  threshold: number;
}

export interface UserCard {
  id: string;
  cardHolder: string;
  cardNumberMasked: string;
  cardType: string;
  expiry: string;
  status: 'ACTIVE' | 'BLOCKED';
  dailyLimit: number;
  availableLimit: number;
  linkedPhone: string;
}

export interface TravelNotice {
  id: number;
  customerEmail: string;
  cardMasked: string;
  destinationCountry: string;
  destinationCity: string;
  startDate: string;
  endDate: string;
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
  createdAt: string;
  activeNow: boolean;
}

export interface VerificationHistoryItem {
  id: string;
  requestId: string;
  transactionRef: string;
  amount: number;
  merchant: string;
  location: string;
  decision: 'OWNER_CONFIRMED' | 'OWNER_REJECTED';
  method: string;
  verifiedAt: string;
  challengeToken: string;
}

export interface AuditLogItem {
  id: number;
  timestamp: string;
  eventType: string;
  transactionRef?: string;
  cardLast4?: string;
  userId?: number;
  customerName?: string;
  actionTaken?: string;
  riskScore?: number;
  mlProbability?: number;
  triggeredRules?: string;
  clientIp?: string;
  userAgent?: string;
  details?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class FraudDataService {
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  private transactions: Transaction[] = [
    { id: '#TX10001', customer: 'Rahul Sharma', card: '**** **** **** 4721', amount: 25000, location: 'Hyderabad', timestamp: '2026-08-08T10:45:00', risk: 'Low', riskScore: 12, status: 'Approved' },
    { id: '#TX10002', customer: 'Priya Singh', card: '**** **** **** 9931', amount: 82500, location: 'Bangalore', timestamp: '2026-08-08T11:02:00', risk: 'Medium', riskScore: 72, status: 'Verification Required', verificationRequestId: 'VR-89102-X', verificationStatus: 'PENDING' },
    { id: '#TX10003', customer: 'Arjun Kumar', card: '**** **** **** 6510', amount: 142000, location: 'Mumbai', timestamp: '2026-08-08T11:18:00', risk: 'High', riskScore: 98, status: 'CUSTOMER_REPORTED_FRAUD' },
    { id: '#TX10004', customer: 'Neha Patel', card: '**** **** **** 1276', amount: 15800, location: 'Chennai', timestamp: '2026-08-08T11:40:00', risk: 'Low', riskScore: 8, status: 'Approved' },
    { id: '#TX10005', customer: 'Vikram Rao', card: '**** **** **** 8401', amount: 65000, location: 'Delhi', timestamp: '2026-08-08T12:05:00', risk: 'Medium', riskScore: 66, status: 'OWNER_VERIFIED' },
    { id: '#TX10006', customer: 'Santu Vanjarapu', card: '**** **** **** 7328', amount: 218000, location: 'Pune', timestamp: '2026-08-08T12:24:00', risk: 'High', riskScore: 96, status: 'Blocked' },
    { id: '#TX10007', customer: 'Sneha Reddy', card: '**** **** **** 6421', amount: 18000, location: 'Chennai', timestamp: '2026-08-08T12:32:00', risk: 'Low', riskScore: 21, status: 'Approved' }
  ];

  private alerts: FraudAlert[] = [
    { id: 'FA1023', transactionId: '#TX10003', customer: 'Rahul Sharma', amount: 120000, riskScore: 98, location: 'Hyderabad', status: 'Blocked', createdAt: '10 min ago', triggerReasons: '🚨 CUSTOMER REPORTED UNAUTHORIZED FRAUD' },
    { id: 'FA1024', transactionId: '#TX10002', customer: 'Priya Singh', amount: 65000, riskScore: 72, location: 'Bangalore', status: 'Reviewing', createdAt: '18 min ago', triggerReasons: 'Step-Up Verification Pending' },
    { id: 'FA1025', transactionId: '#TX10006', customer: 'Arjun Kumar', amount: 205000, riskScore: 99, location: 'Mumbai', status: 'Blocked', createdAt: '32 min ago' },
    { id: 'FA1026', transactionId: '#TX10007', customer: 'Sneha Reddy', amount: 18000, riskScore: 21, location: 'Chennai', status: 'Resolved', createdAt: '1 hr ago' }
  ];

  private customers: Customer[] = [
    { name: 'Rahul Sharma', email: 'rahul@example.com', card: '****4582', transactions: 248, fraudScore: 12, status: 'Low', locations: ['Hyderabad', 'Mumbai'] },
    { name: 'Priya Singh', email: 'priya@example.com', card: '****8974', transactions: 512, fraudScore: 91, status: 'High', locations: ['Bangalore', 'Delhi'] },
    { name: 'Amit Kumar', email: 'amit@example.com', card: '****2341', transactions: 175, fraudScore: 45, status: 'Medium', locations: ['Pune'] },
    { name: 'Sneha Reddy', email: 'sneha@example.com', card: '****6421', transactions: 322, fraudScore: 8, status: 'Low', locations: ['Chennai'] },
    { name: 'Arjun Kumar', email: 'arjun@example.com', card: '****6510', transactions: 389, fraudScore: 88, status: 'High', locations: ['Mumbai'] }
  ];

  private pendingVerifications: TransactionVerification[] = [
    {
      verificationRequestId: 'VR-89102-X',
      transactionRef: '#TX10002',
      customerName: 'Priya Singh',
      customerEmail: 'priya@example.com',
      cardMasked: '**** **** **** 9931',
      amount: 82500,
      merchantName: 'Croma Electronics',
      location: 'Bangalore',
      riskLevel: 'Medium',
      riskScore: 72,
      status: 'PENDING',
      requestedAt: '18 min ago',
      expiresAt: 'In 10 minutes',
      challengeToken: 'CHALLENGE-TOKEN-PRIYA-98214'
    }
  ];

  constructor(
    private readonly http: HttpClient,
    private readonly notificationService: NotificationService
  ) {
    this.syncWithBackend();
  }

  syncWithBackend(): void {
    this.http.get<ApiResponse<Transaction[]>>(`${this.baseUrl}/transactions`).pipe(
      catchError(() => of(null))
    ).subscribe(res => {
      if (res && res.success && res.data && res.data.length > 0) {
        this.transactions = res.data;
      }
    });

    this.http.get<ApiResponse<FraudAlert[]>>(`${this.baseUrl}/alerts`).pipe(
      catchError(() => of(null))
    ).subscribe(res => {
      if (res && res.success && res.data && res.data.length > 0) {
        this.alerts = res.data;
      }
    });

    this.http.get<ApiResponse<Customer[]>>(`${this.baseUrl}/customers`).pipe(
      catchError(() => of(null))
    ).subscribe(res => {
      if (res && res.success && res.data && res.data.length > 0) {
        this.customers = res.data;
      }
    });

    this.http.get<ApiResponse<TransactionVerification[]>>(`${this.baseUrl}/verifications/pending`).pipe(
      catchError(() => of(null))
    ).subscribe(res => {
      if (res && res.success && res.data) {
        this.pendingVerifications = res.data;
      }
    });
  }

  getTransactions(): Transaction[] { return this.transactions; }
  getAlerts(): FraudAlert[] { return this.alerts; }
  getCustomers(): Customer[] { return this.customers; }
  getPendingVerifications(): TransactionVerification[] { return this.pendingVerifications; }

  updateAlert(id: string, status: AlertStatus): void {
    const alert = this.alerts.find(item => item.id === id);
    if (alert) alert.status = status;
    const transaction = this.transactions.find(item => item.id === alert?.transactionId);
    if (transaction && (status === 'Blocked' || status === 'Resolved')) {
      transaction.status = status === 'Blocked' ? 'Blocked' : 'Approved';
    }

    // Persist to backend
    this.http.patch<ApiResponse<FraudAlert>>(`${this.baseUrl}/alerts/${id}/status`, { status, notes: 'Updated from Alert Center UI' })
      .pipe(catchError(() => of(null)))
      .subscribe();
  }

  blockTransaction(id: string): void {
    const transaction = this.transactions.find(item => item.id === id);
    if (transaction) {
      transaction.status = 'Blocked';
      transaction.risk = 'High';
    }

    // Persist to backend
    this.http.post<ApiResponse<Transaction>>(`${this.baseUrl}/transactions/${id}/block`, {})
      .pipe(catchError(() => of(null)))
      .subscribe();
  }

  confirmStepUpVerification(requestId: string, challengeToken: string): Observable<any> {
    const payload = {
      challengeToken,
      verificationMethod: 'WEBAUTHN_PASSKEY',
      userAgent: navigator.userAgent,
      authenticatorData: 'FIDO2_PASSKEY_AUTH_ASSERTION'
    };

    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/verifications/${requestId}/step-up`, payload).pipe(
      tap(() => {
        // Local update
        this.pendingVerifications = this.pendingVerifications.filter(v => v.verificationRequestId !== requestId);
        const tx = this.transactions.find(t => t.verificationRequestId === requestId || t.status === 'Verification Required');
        if (tx) {
          tx.status = 'OWNER_VERIFIED';
          tx.decision = 'APPROVE';
        }
        this.syncWithBackend();
      }),
      catchError(() => {
        // Fallback local update
        this.pendingVerifications = this.pendingVerifications.filter(v => v.verificationRequestId !== requestId);
        const tx = this.transactions.find(t => t.verificationRequestId === requestId || t.status === 'Verification Required');
        if (tx) {
          tx.status = 'OWNER_VERIFIED';
          tx.decision = 'APPROVE';
        }
        return of({ success: true, message: 'Step-up verification completed successfully.' });
      })
    );
  }

  rejectVerification(requestId: string, reason = 'Customer confirmed unauthorized transaction'): Observable<any> {
    const payload = {
      reason,
      blockCard: true
    };

    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/verifications/${requestId}/reject`, payload).pipe(
      tap(() => {
        // Local update
        this.pendingVerifications = this.pendingVerifications.filter(v => v.verificationRequestId !== requestId);
        const tx = this.transactions.find(t => t.verificationRequestId === requestId || t.status === 'Verification Required');
        if (tx) {
          tx.status = 'CUSTOMER_REPORTED_FRAUD';
          tx.decision = 'DECLINE';
          tx.risk = 'High';
          tx.riskScore = 99;
        }
        this.syncWithBackend();
      }),
      catchError(() => {
        // Fallback local update
        this.pendingVerifications = this.pendingVerifications.filter(v => v.verificationRequestId !== requestId);
        const tx = this.transactions.find(t => t.verificationRequestId === requestId || t.status === 'Verification Required');
        if (tx) {
          tx.status = 'CUSTOMER_REPORTED_FRAUD';
          tx.decision = 'DECLINE';
          tx.risk = 'High';
          tx.riskScore = 99;
        }
        return of({ success: true, message: 'Transaction reported as unauthorized fraud.' });
      })
    );
  }

  getSettings(): FraudSettings {
    const defaults: FraudSettings = { darkMode: false, notifications: true, aiDetection: true, autoBlock: false, threshold: 80 };
    try { return { ...defaults, ...JSON.parse(localStorage.getItem('fraud-settings') ?? '{}') }; } catch { return defaults; }
  }

  saveSettings(settings: FraudSettings): void {
    localStorage.setItem('fraud-settings', JSON.stringify(settings));
    document.body.classList.toggle('dark-theme', settings.darkMode);

    // Sync to backend
    this.http.put<ApiResponse<FraudSettings>>(`${this.baseUrl}/settings`, settings)
      .pipe(catchError(() => of(null)))
      .subscribe();
  }

  resetSettings(): FraudSettings {
    const defaults: FraudSettings = { darkMode: false, notifications: true, aiDetection: true, autoBlock: false, threshold: 80 };
    this.saveSettings(defaults);
    return defaults;
  }

  getAnalytics(range = '7d') {
    const locationCounts = this.alerts.reduce<Record<string, number>>((counts, alert) => { 
      counts[alert.location] = (counts[alert.location] ?? 0) + 1; 
      return counts; 
    }, {});

    return {
      totalAmount: this.transactions.reduce((total, item) => total + item.amount, 0),
      fraudCount: this.alerts.filter(item => item.status !== 'Resolved').length,
      locations: Object.entries(locationCounts).sort((a, b) => b[1] - a[1]),
      trend: range === '90d'
        ? [34, 46, 41, 55, 63, 58, 72, 68, 81, 77, 88, 94]
        : range === '30d'
          ? [42, 58, 51, 76, 68, 89, 94, 82, 91, 96]
          : [42, 58, 51, 76, 68, 89, 94],
      distribution: { 
        approved: this.transactions.filter(item => item.status === 'Approved' || item.status === 'OWNER_VERIFIED').length, 
        review: this.transactions.filter(item => item.status === 'Review' || item.status === 'Verification Required').length, 
        blocked: this.transactions.filter(item => item.status === 'Blocked' || item.status === 'CUSTOMER_REPORTED_FRAUD').length 
      }
    };
  }

  simulateFraud(transactionId: string, amount: number): Observable<any> {
    this.notificationService.sendFraudAlert(
      'Rahul Sharma',
      amount,
      'Binance Global Crypto',
      'London (High Risk IP)',
      '4721',
      'VR-24E120F5'
    );

    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/dashboard/simulate`, {
      transactionId,
      amount,
      customerName: 'Rahul Sharma',
      location: 'London (High Risk IP)',
      merchantCategory: 'CRYPTO'
    }).pipe(
      tap(() => this.syncWithBackend()),
      catchError(() => of({
        success: true,
        data: {
          transactionId,
          amount,
          riskScore: 98,
          riskLevel: 'High',
          status: 'Verification Required',
          decision: 'FLAG',
          reasons: ['Critical amount threshold exceeded (> ₹1,00,000)', 'High-risk merchant category: CRYPTO'],
          message: `Simulation completed for ${transactionId} (₹${amount}): Step-Up Verification request generated for cardholder.`
        }
      }))
    );
  }

  processTransaction(txData: {
    customerName: string;
    cardLast4: string;
    amount: number;
    merchantName: string;
    merchantCategory: string;
    location: string;
    deviceFingerprint?: string;
  }): Observable<any> {
    const cardDigits = txData.cardLast4.replace(/\D/g, '').slice(-4) || '4721';
    const payload = {
      customerName: txData.customerName,
      cardLast4: cardDigits,
      amount: txData.amount,
      merchantName: txData.merchantName,
      merchantCategory: txData.merchantCategory,
      location: txData.location,
      deviceFingerprint: txData.deviceFingerprint || 'DEV-WEB-BROWSER',
      ipAddress: '192.168.1.100'
    };

    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/transactions`, payload).pipe(
      tap(res => {
        if (res && res.data) {
          const newTx: Transaction = {
            id: res.data.transactionRef || `#TX${Date.now().toString().slice(-5)}`,
            customer: res.data.customerName || txData.customerName,
            card: `**** **** **** ${res.data.cardMasked ? res.data.cardMasked.slice(-4) : cardDigits}`,
            amount: res.data.amount || txData.amount,
            merchantName: res.data.merchantName || txData.merchantName,
            merchantCategory: res.data.merchantCategory || txData.merchantCategory,
            location: res.data.location || txData.location,
            timestamp: res.data.timestamp || new Date().toISOString(),
            risk: res.data.risk || (txData.amount > 100000 ? 'High' : 'Low'),
            riskScore: res.data.riskScore !== undefined ? res.data.riskScore : (txData.amount > 100000 ? 88 : 12),
            status: res.data.status || (txData.amount > 100000 ? 'Blocked' : 'Approved'),
            reasons: res.data.decisionReasons ? [res.data.decisionReasons] : undefined
          };
          this.transactions.unshift(newTx);
          this.syncWithBackend();
        }
      }),
      catchError(() => {
        const isHigh = txData.amount >= 100000 || txData.merchantCategory === 'CRYPTO';
        const isMed = txData.amount >= 50000 && !isHigh;
        const newTx: Transaction = {
          id: `#TX${Date.now().toString().slice(-5)}`,
          customer: txData.customerName,
          card: `**** **** **** ${cardDigits}`,
          amount: txData.amount,
          merchantName: txData.merchantName,
          merchantCategory: txData.merchantCategory,
          location: txData.location,
          timestamp: new Date().toISOString(),
          risk: isHigh ? 'High' : (isMed ? 'Medium' : 'Low'),
          riskScore: isHigh ? 92 : (isMed ? 65 : 14),
          status: isHigh ? 'Blocked' : (isMed ? 'Verification Required' : 'Approved'),
          reasons: isHigh ? ['Amount threshold anomaly', 'Risky merchant category: ' + txData.merchantCategory] : ['Standard transaction approved']
        };
        this.transactions.unshift(newTx);
        return of({ success: true, data: newTx });
      })
    );
  }

  fetchAnalytics(range = '7d'): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/analytics?range=${range}`).pipe(
      catchError(() => of({ success: true, data: this.getAnalytics(range), message: 'Local calculation' }))
    );
  }

  // ==================== USER / CARDHOLDER DATA HELPERS ====================

  private userCards: UserCard[] = [
    { id: '1', cardHolder: 'Priya Singh', cardNumberMasked: '**** **** **** 9931', cardType: 'HDFC Visa Platinum', expiry: '09/28', status: 'ACTIVE', dailyLimit: 150000, availableLimit: 67500, linkedPhone: '+91 98222 33445' },
    { id: '2', cardHolder: 'Priya Singh', cardNumberMasked: '**** **** **** 4120', cardType: 'ICICI RuPay Select', expiry: '11/29', status: 'ACTIVE', dailyLimit: 75000, availableLimit: 72000, linkedPhone: '+91 98222 33445' },
    { id: '3', cardHolder: 'Rahul Sharma', cardNumberMasked: '**** **** **** 4721', cardType: 'SBI Global Master', expiry: '06/27', status: 'ACTIVE', dailyLimit: 200000, availableLimit: 175000, linkedPhone: '+91 98111 22334' },
    { id: '4', cardHolder: 'Arjun Kumar', cardNumberMasked: '**** **** **** 6510', cardType: 'Axis Bank Titanium', expiry: '04/26', status: 'BLOCKED', dailyLimit: 100000, availableLimit: 0, linkedPhone: '+91 98333 44556' }
  ];

  private verificationHistory: VerificationHistoryItem[] = [
    { id: 'VH-901', requestId: 'VR-44120-K', transactionRef: '#TX10005', amount: 65000, merchant: 'MakeMyTrip Flights', location: 'Delhi', decision: 'OWNER_CONFIRMED', method: 'FIDO2 Passkey Biometric', verifiedAt: 'Today, 01:13 PM', challengeToken: 'CHALLENGE-TOKEN-VIKRAM-7721' },
    { id: 'VH-902', requestId: 'VR-32109-M', transactionRef: '#TX10003', amount: 142000, merchant: 'Global Crypto Exchange', location: 'Mumbai', decision: 'OWNER_REJECTED', method: 'SMS Step-Up Link', verifiedAt: 'Today, 12:26 PM', challengeToken: 'CHALLENGE-TOKEN-ARJUN-8841' },
    { id: 'VH-903', requestId: 'VR-11002-A', transactionRef: '#TX09841', amount: 34500, merchant: 'Amazon India Electronics', location: 'Bangalore', decision: 'OWNER_CONFIRMED', method: 'FIDO2 Passkey Biometric', verifiedAt: 'Yesterday, 06:40 PM', challengeToken: 'CHALLENGE-TOKEN-PRIYA-1102' }
  ];

  fetchUserCards(email = '', customerName = ''): Observable<UserCard[]> {
    const url = email ? `${this.baseUrl}/customers/cards?email=${encodeURIComponent(email)}` : `${this.baseUrl}/customers/cards`;
    return this.http.get<ApiResponse<any[]>>(url).pipe(
      map(res => {
        if (res && res.success && res.data && res.data.length > 0) {
          const mapped: UserCard[] = res.data.map((c, idx) => ({
            id: String(c.id || idx + 1),
            cardHolder: c.cardHolder || customerName || 'Cardholder',
            cardNumberMasked: c.cardNumberMasked || '**** **** **** 0000',
            cardType: c.cardType || 'VISA',
            expiry: '12/28',
            status: ((c.status || 'ACTIVE').toUpperCase()) as 'ACTIVE' | 'BLOCKED',
            dailyLimit: c.dailyLimit || 100000,
            availableLimit: c.dailyLimit ? Math.round(c.dailyLimit * 0.75) : 75000,
            linkedPhone: c.phone || '+91 98111 22334'
          }));
          this.userCards = mapped;
          return mapped;
        }
        return this.getUserCards(customerName);
      }),
      catchError(() => of(this.getUserCards(customerName)))
    );
  }

  getUserCards(customerName = ''): UserCard[] {
    if (!customerName) return this.userCards;
    const clean = customerName.toLowerCase();
    const filtered = this.userCards.filter(c => c.cardHolder.toLowerCase().includes(clean) || clean.includes(c.cardHolder.toLowerCase()));
    return filtered.length ? filtered : [this.userCards[0]];
  }

  toggleCardLock(cardId: string): Observable<any> {
    const card = this.userCards.find(c => c.id === cardId);
    if (card) {
      card.status = card.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
    }
    const numId = parseInt(cardId.replace(/\D/g, ''), 10) || 1;
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/customers/cards/${numId}/toggle-lock`, {}).pipe(
      catchError(() => of({ success: true, data: card }))
    );
  }

  updateCardLimit(cardId: string, dailyLimit: number): Observable<any> {
    const card = this.userCards.find(c => c.id === cardId);
    if (card) {
      card.dailyLimit = dailyLimit;
      card.availableLimit = Math.min(card.availableLimit || dailyLimit, dailyLimit);
    }
    const numId = parseInt(cardId.replace(/\D/g, ''), 10) || 1;
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/customers/cards/${numId}/limit`, { dailyLimit }).pipe(
      catchError(() => of({ success: true, data: card }))
    );
  }

  getUserTransactions(customerName = ''): Transaction[] {
    if (!customerName) return this.transactions;
    const clean = customerName.toLowerCase();
    const filtered = this.transactions.filter(t => t.customer.toLowerCase().includes(clean) || clean.includes(t.customer.toLowerCase()));
    return filtered.length ? filtered : this.transactions.slice(0, 3);
  }

  fetchUserTransactions(email = '', customerName = ''): Observable<Transaction[]> {
    const url = email 
      ? `${this.baseUrl}/customers/transactions?email=${encodeURIComponent(email)}`
      : `${this.baseUrl}/customers/transactions`;
    return this.http.get<ApiResponse<any[]>>(url).pipe(
      map(res => {
        if (res && res.success && res.data && res.data.length > 0) {
          return res.data.map(t => ({
            id: t.id || `#TX${Date.now()}`,
            customer: t.customer || customerName || 'Cardholder',
            card: t.card || '**** **** **** 0000',
            amount: t.amount || 0,
            merchantName: t.merchantName || 'Merchant Outlet',
            merchantCategory: t.merchantCategory || 'RETAIL',
            location: t.location || 'Unknown',
            timestamp: t.timestamp || new Date().toISOString(),
            risk: t.risk || 'Low',
            riskScore: t.riskScore !== undefined ? t.riskScore : 10,
            status: t.status || 'Approved',
            reasons: t.reasons || [],
            decision: t.decision || 'APPROVE',
            verificationRequestId: t.verificationRequestId
          }));
        }
        return this.getUserTransactions(customerName);
      }),
      catchError(() => of(this.getUserTransactions(customerName)))
    );
  }

  fileDispute(transactionRef: string, reason: string, email = ''): Observable<any> {
    const url = email 
      ? `${this.baseUrl}/customers/disputes?email=${encodeURIComponent(email)}`
      : `${this.baseUrl}/customers/disputes`;
    return this.http.post<ApiResponse<any>>(url, { transactionRef, reason }).pipe(
      tap(() => {
        const tx = this.transactions.find(t => t.id === transactionRef);
        if (tx) tx.status = 'CUSTOMER_REPORTED_FRAUD';
      }),
      catchError(() => of({ success: true, data: { status: 'UNDER_REVIEW' } }))
    );
  }

  getUserAlerts(customerName = ''): FraudAlert[] {
    if (!customerName) return this.alerts;
    const clean = customerName.toLowerCase();
    const filtered = this.alerts.filter(a => a.customer.toLowerCase().includes(clean) || clean.includes(a.customer.toLowerCase()));
    return filtered.length ? filtered : this.alerts.slice(0, 2);
  }

  getUserVerifications(customerName = ''): TransactionVerification[] {
    if (!customerName) return this.pendingVerifications;
    const clean = customerName.toLowerCase();
    const filtered = this.pendingVerifications.filter(v => v.customerName.toLowerCase().includes(clean) || clean.includes(v.customerName.toLowerCase()));
    return filtered.length ? filtered : this.pendingVerifications;
  }

  getVerificationHistory(customerName = ''): VerificationHistoryItem[] {
    return this.verificationHistory;
  }

  fetchVerificationHistory(email = ''): Observable<VerificationHistoryItem[]> {
    const url = email 
      ? `${this.baseUrl}/verifications/history?email=${encodeURIComponent(email)}`
      : `${this.baseUrl}/verifications/history`;
    return this.http.get<ApiResponse<any[]>>(url).pipe(
      map(res => {
        if (res && res.success && res.data && res.data.length > 0) {
          const list: VerificationHistoryItem[] = res.data.map(v => ({
            id: v.id || `VH-${v.requestId || '001'}`,
            requestId: v.requestId || 'VR-UNKNOWN',
            transactionRef: v.transactionRef || '#TX000',
            amount: v.amount || 0,
            merchant: v.merchant || 'Merchant Outlet',
            location: v.location || 'Unknown',
            decision: (v.decision === 'OWNER_CONFIRMED' ? 'OWNER_CONFIRMED' : 'OWNER_REJECTED') as 'OWNER_CONFIRMED' | 'OWNER_REJECTED',
            method: v.method || 'FIDO2 Passkey / Token',
            verifiedAt: v.verifiedAt || 'Recent',
            challengeToken: v.challengeToken || ''
          }));
          this.verificationHistory = list;
          return list;
        }
        return this.verificationHistory;
      }),
      catchError(() => of(this.verificationHistory))
    );
  }

  addVerificationHistory(item: VerificationHistoryItem): void {
    this.verificationHistory.unshift(item);
  }

  fetchFraudRules(): Observable<any[]> {
    return this.http.get<ApiResponse<any[]>>(`${this.baseUrl}/settings/rules`).pipe(
      map(res => res?.data || []),
      catchError(() => of([]))
    );
  }

  saveFraudRules(rules: any[]): Observable<any> {
    return this.http.put<ApiResponse<any>>(`${this.baseUrl}/settings/rules`, rules).pipe(
      catchError(() => of({ success: true, data: rules }))
    );
  }

  fetchAuditLogs(page: number = 0, size: number = 20): Observable<{ content: AuditLogItem[], totalElements: number }> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/audit-logs?page=${page}&size=${size}`).pipe(
      map(res => {
        if (res?.data?.content) {
          return {
            content: res.data.content as AuditLogItem[],
            totalElements: res.data.totalElements || res.data.content.length
          };
        }
        return { content: [], totalElements: 0 };
      }),
      catchError(() => of({ content: [], totalElements: 0 }))
    );
  }

  // ==========================================
  // CARDHOLDER TRAVEL MODE / NOTICES
  // ==========================================
  fetchTravelNotices(email = 'priya@example.com'): Observable<TravelNotice[]> {
    return this.http.get<ApiResponse<TravelNotice[]>>(`${this.baseUrl}/customers/travel-notices?email=${encodeURIComponent(email)}`).pipe(
      map(res => res?.data || []),
      catchError(() => of([
        {
          id: 1,
          customerEmail: email,
          cardMasked: '**** **** **** 8821',
          destinationCountry: 'United Arab Emirates',
          destinationCity: 'Dubai',
          startDate: new Date().toISOString().split('T')[0],
          endDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
          status: 'ACTIVE' as const,
          createdAt: new Date().toISOString(),
          activeNow: true
        } as TravelNotice
      ]))
    );
  }

  createTravelNotice(email: string, notice: { cardMasked?: string; destinationCountry: string; destinationCity: string; startDate: string; endDate: string }): Observable<TravelNotice> {
    return this.http.post<ApiResponse<TravelNotice>>(`${this.baseUrl}/customers/travel-notices?email=${encodeURIComponent(email)}`, notice).pipe(
      map(res => res.data),
      catchError(() => of({
        id: Date.now(),
        customerEmail: email,
        cardMasked: notice.cardMasked || '**** **** **** 8821',
        destinationCountry: notice.destinationCountry,
        destinationCity: notice.destinationCity,
        startDate: notice.startDate,
        endDate: notice.endDate,
        status: 'ACTIVE' as const,
        createdAt: new Date().toISOString(),
        activeNow: true
      }))
    );
  }

  cancelTravelNotice(noticeId: number): Observable<boolean> {
    return this.http.delete<ApiResponse<string>>(`${this.baseUrl}/customers/travel-notices/${noticeId}`).pipe(
      map(res => res?.success || false),
      catchError(() => of(true))
    );
  }
}
