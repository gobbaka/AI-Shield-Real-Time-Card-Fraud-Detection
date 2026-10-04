import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Sidebar } from '../../shared/sidebar/sidebar';
import { Navbar } from '../../shared/navbar/navbar';
import { Chatbot } from '../../shared/chatbot/chatbot';
import { FraudDataService, Transaction, TransactionStatus, AuditLogItem } from '../../services/fraud-data.service';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule, Sidebar, Navbar, Chatbot],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css'
})
export class TransactionsComponent {
  readonly pageSize = 5;
  search = '';
  status: TransactionStatus | 'All' = 'All';
  dateRange = 'All';
  sortKey: keyof Transaction = 'timestamp';
  sortDirection: 'asc' | 'desc' = 'desc';
  currentPage = 1;
  selected: Transaction | null = null;
  loading = false;
  error = '';

  // Tab State
  activeTab: 'transactions' | 'audit-logs' = 'transactions';

  // Audit Logs State
  auditLogs: AuditLogItem[] = [];
  loadingAuditLogs = false;
  totalAuditLogs = 0;
  auditPage = 0;
  readonly auditPageSize = 10;
  selectedAuditLog: AuditLogItem | null = null;
  auditSearch = '';

  // New Transaction Modal State
  showNewTxModal = false;
  isProcessingTx = false;
  txSuccessMessage = '';
  newCustomer = 'Rahul Sharma';
  newCard = '4721';
  newAmount = 85000;
  newMerchant = 'Croma Electronics Store';
  newCategory = 'ELECTRONICS';
  newLocation = 'Mumbai';
  newDevice = 'DEV-MOBILE-ANDROID';

  constructor(private readonly data: FraudDataService) {}

  switchTab(tab: 'transactions' | 'audit-logs'): void {
    this.activeTab = tab;
    if (tab === 'audit-logs' && this.auditLogs.length === 0) {
      this.loadAuditLogs();
    }
  }

  loadAuditLogs(): void {
    this.loadingAuditLogs = true;
    this.data.fetchAuditLogs(this.auditPage, this.auditPageSize).subscribe(res => {
      this.loadingAuditLogs = false;
      this.auditLogs = res.content || [];
      this.totalAuditLogs = res.totalElements || 0;
    });
  }

  changeAuditPage(delta: number): void {
    const next = this.auditPage + delta;
    if (next >= 0 && next * this.auditPageSize < this.totalAuditLogs) {
      this.auditPage = next;
      this.loadAuditLogs();
    }
  }

  viewAuditLog(log: AuditLogItem): void {
    this.selectedAuditLog = log;
  }

  closeAuditLogModal(): void {
    this.selectedAuditLog = null;
  }

  get filteredAuditLogs(): AuditLogItem[] {
    const q = this.auditSearch.trim().toLowerCase();
    if (!q) return this.auditLogs;
    return this.auditLogs.filter(l =>
      (l.eventType || '').toLowerCase().includes(q) ||
      (l.transactionRef || '').toLowerCase().includes(q) ||
      (l.customerName || '').toLowerCase().includes(q) ||
      (l.actionTaken || '').toLowerCase().includes(q) ||
      (l.triggeredRules || '').toLowerCase().includes(q)
    );
  }

  get auditPageCount(): number {
    return Math.max(1, Math.ceil(this.totalAuditLogs / this.auditPageSize));
  }

  getAuditBadgeClass(eventType: string): string {
    const e = (eventType || '').toUpperCase();
    if (e.includes('EVALUAT')) return 'badge-evaluated';
    if (e.includes('STEP_UP') || e.includes('VERIF')) return 'badge-stepup';
    if (e.includes('DISPUTE') || e.includes('ALERT')) return 'badge-dispute';
    return 'badge-general';
  }

  openNewTxModal(): void {
    this.showNewTxModal = true;
    this.txSuccessMessage = '';
  }

  closeNewTxModal(): void {
    this.showNewTxModal = false;
  }

  setTxPreset(preset: 'safe' | 'crypto' | 'travel' | 'high'): void {
    if (preset === 'safe') {
      this.newCustomer = 'Priya Singh';
      this.newCard = '9931';
      this.newAmount = 2450;
      this.newMerchant = 'Starbucks Coffee';
      this.newCategory = 'RETAIL';
      this.newLocation = 'Bangalore';
    } else if (preset === 'crypto') {
      this.newCustomer = 'Arjun Kumar';
      this.newCard = '6510';
      this.newAmount = 145000;
      this.newMerchant = 'WazirX Crypto Exchange';
      this.newCategory = 'CRYPTO';
      this.newLocation = 'London (Tor IP)';
    } else if (preset === 'travel') {
      this.newCustomer = 'Rahul Sharma';
      this.newCard = '4721';
      this.newAmount = 68000;
      this.newMerchant = 'Emirates Airline DutyFree';
      this.newCategory = 'TRAVEL';
      this.newLocation = 'Dubai (20m after Mumbai)';
    } else if (preset === 'high') {
      this.newCustomer = 'Santu Vanjarapu';
      this.newCard = '7328';
      this.newAmount = 220000;
      this.newMerchant = 'Luxury Jewelry World';
      this.newCategory = 'JEWELRY';
      this.newLocation = 'Delhi';
    }
  }

  submitNewTransaction(): void {
    if (!this.newCustomer || !this.newAmount || this.newAmount <= 0) return;
    this.isProcessingTx = true;

    this.data.processTransaction({
      customerName: this.newCustomer,
      cardLast4: this.newCard,
      amount: this.newAmount,
      merchantName: this.newMerchant,
      merchantCategory: this.newCategory,
      location: this.newLocation,
      deviceFingerprint: this.newDevice
    }).subscribe(res => {
      this.isProcessingTx = false;
      this.txSuccessMessage = `✅ Transaction processed & evaluated by AI Shield. Score: ${res.data?.riskScore || 85}/100 (${res.data?.status || 'Verification Required'}).`;
      this.currentPage = 1;
      setTimeout(() => {
        this.showNewTxModal = false;
        this.txSuccessMessage = '';
      }, 2500);
    });
  }

  get transactions(): Transaction[] { 
    return this.data.getTransactions(); 
  }

  get filteredTransactions(): Transaction[] {
    const query = this.search.trim().toLowerCase();
    const newestTimestamp = Math.max(...this.transactions.map(item => new Date(item.timestamp).getTime()));
    const rangeDays: Record<string, number> = { Today: 1, Yesterday: 2, 'This Week': 7, 'This Month': 31 };
    const selectedDays = rangeDays[this.dateRange];
    const rangeStart = selectedDays ? newestTimestamp - selectedDays * 24 * 60 * 60 * 1000 : 0;
    const yesterdayStart = newestTimestamp - 2 * 24 * 60 * 60 * 1000;
    
    return this.transactions.filter(item => {
      const timestamp = new Date(item.timestamp).getTime();
      const matchesDate = this.dateRange === 'Yesterday'
        ? timestamp < newestTimestamp && timestamp >= yesterdayStart
        : !selectedDays || timestamp >= rangeStart;
      return (!query || `${item.id} ${item.customer} ${item.location}`.toLowerCase().includes(query))
        && (this.status === 'All' || item.status === this.status)
        && matchesDate;
    });
  }

  get sortedTransactions(): Transaction[] { 
    return [...this.filteredTransactions].sort((a, b) => { 
      const left = a[this.sortKey] ?? ''; 
      const right = b[this.sortKey] ?? ''; 
      const result = left < right ? -1 : left > right ? 1 : 0; 
      return this.sortDirection === 'asc' ? result : -result; 
    }); 
  }

  get pagedTransactions(): Transaction[] { 
    const start = (this.currentPage - 1) * this.pageSize; 
    return this.sortedTransactions.slice(start, start + this.pageSize); 
  }

  get pageCount(): number { 
    return Math.max(1, Math.ceil(this.sortedTransactions.length / this.pageSize)); 
  }

  get approvedCount(): number { 
    return this.transactions.filter(item => item.status === 'Approved').length; 
  }

  get reviewCount(): number { 
    return this.transactions.filter(item => item.status === 'Review').length; 
  }

  get blockedCount(): number { 
    return this.transactions.filter(item => item.status === 'Blocked').length; 
  }

  applyFilters(): void { 
    this.currentPage = 1; 
  }

  sortBy(key: keyof Transaction): void { 
    this.sortDirection = this.sortKey === key && this.sortDirection === 'asc' ? 'desc' : 'asc'; 
    this.sortKey = key; 
  }

  changePage(delta: number): void { 
    this.currentPage = Math.min(this.pageCount, Math.max(1, this.currentPage + delta)); 
  }

  getStatusClass(status: string): string {
    const s = (status || '').toLowerCase().replace(/[\s_]+/g, '-');
    return 'status-' + s;
  }

  view(transaction: Transaction): void { 
    this.selected = transaction; 
  }

  exportReport(): void { 
    const rows = [
      ['Transaction ID', 'Customer', 'Amount', 'Location', 'Risk', 'Status'], 
      ...this.sortedTransactions.map(item => [item.id, item.customer, String(item.amount), item.location, String(item.riskScore), item.status])
    ]; 
    const blob = new Blob([rows.map(row => row.join(',')).join('\n')], { type: 'text/csv' }); 
    const url = URL.createObjectURL(blob); 
    const link = document.createElement('a'); 
    link.href = url; 
    link.download = 'transactions.csv'; 
    link.click(); 
    URL.revokeObjectURL(url); 
  }
}