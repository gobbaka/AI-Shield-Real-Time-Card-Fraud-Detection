import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Sidebar } from '../../shared/sidebar/sidebar';
import { Navbar } from '../../shared/navbar/navbar';
import { Chatbot } from '../../shared/chatbot/chatbot';
import { FraudDataService, Transaction } from '../../services/fraud-data.service';

export interface AnalysisDetail {
  transactionId?: string;
  amount?: number;
  customer?: string;
  location?: string;
  riskScore: number;
  riskLevel: string;
  decision: string;
  status: string;
  reasons: string[];
  message: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, Sidebar, Navbar, Chatbot],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard {

  showSimulation = false;
  isAnalyzing = false;
  reportMessage = '';
  
  // Real-world terminal input models
  simCustomer = 'Rahul Sharma';
  simCard = '4721';
  simAmount = 145000;
  simMerchant = 'Binance Global Crypto';
  simCategory = 'CRYPTO';
  simLocation = 'London (High-Risk IP)';
  simDevice = 'DEV-TOR-PROXY-99';

  analysisResult: AnalysisDetail | null = null;

  constructor(private readonly data: FraudDataService, private readonly router: Router) {}

  get transactions(): Transaction[] { return this.data.getTransactions(); }
  get recentTransactions(): Transaction[] {
    return [...this.transactions].sort((left, right) => new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime()).slice(0, 5);
  }
  get fraudAlertCount(): number { return this.data.getAlerts().filter(alert => alert.status !== 'Resolved').length; }
  get safeTransactionRate(): number { return this.transactions.length ? Math.round((this.transactions.filter(transaction => transaction.status === 'Approved').length / this.transactions.length) * 1000) / 10 : 0; }
  get trustScore(): number { return this.transactions.length ? Math.round(100 - this.transactions.reduce((total, transaction) => total + transaction.riskScore, 0) / this.transactions.length) : 0; }
  
  viewAllTransactions(): void { this.router.navigate(['/transactions']); }

  openSimulation() {
    this.analysisResult = null;
    this.showSimulation = true;
  }

  closeSimulation() {
    this.showSimulation = false;
    this.analysisResult = null;
  }

  setPreset(type: 'crypto' | 'safe' | 'velocity' | 'travel') {
    if (type === 'crypto') {
      this.simCustomer = 'Arjun Kumar';
      this.simCard = '6510';
      this.simAmount = 185000;
      this.simMerchant = 'Global Crypto Exchange';
      this.simCategory = 'CRYPTO';
      this.simLocation = 'London (Darknet Node)';
      this.simDevice = 'DEV-UNKNOWN-TOR';
    } else if (type === 'safe') {
      this.simCustomer = 'Sneha Reddy';
      this.simCard = '6421';
      this.simAmount = 3500;
      this.simMerchant = 'Amazon India';
      this.simCategory = 'RETAIL';
      this.simLocation = 'Chennai';
      this.simDevice = 'DEV-FIREFOX-WIN (Known)';
    } else if (type === 'travel') {
      this.simCustomer = 'Rahul Sharma';
      this.simCard = '4721';
      this.simAmount = 92000;
      this.simMerchant = 'Dubai Luxury DutyFree';
      this.simCategory = 'JEWELRY';
      this.simLocation = 'Dubai (20 mins after Hyderabad)';
      this.simDevice = 'DEV-IPHONE-IOS';
    } else if (type === 'velocity') {
      this.simCustomer = 'Santu Vanjarapu';
      this.simCard = '7328';
      this.simAmount = 240000;
      this.simMerchant = 'Express Wire Transfer';
      this.simCategory = 'FOREX';
      this.simLocation = 'Pune';
      this.simDevice = 'DEV-LINUX-BOT';
    }
  }

  runSimulation() {
    this.isAnalyzing = true;
    this.analysisResult = null;

    this.data.simulateFraud(this.simCard, this.simAmount).subscribe(res => {
      this.isAnalyzing = false;
      if (res && res.data) {
        this.analysisResult = {
          transactionId: res.data.transactionId || `#TX${Math.floor(10000 + Math.random() * 9000)}`,
          amount: this.simAmount,
          customer: this.simCustomer,
          location: this.simLocation,
          riskScore: res.data.riskScore ?? (this.simAmount > 100000 ? 98 : 12),
          riskLevel: res.data.riskLevel ?? (this.simAmount > 100000 ? 'High' : 'Low'),
          decision: res.data.decision ?? (this.simAmount > 100000 ? 'DECLINE' : 'APPROVE'),
          status: res.data.status ?? (this.simAmount > 100000 ? 'Blocked' : 'Approved'),
          reasons: res.data.reasons ?? (this.simAmount > 100000 
            ? ['Critical amount threshold exceeded (> ₹1,00,000)', 'High-risk merchant category: ' + this.simCategory, 'Geolocation anomaly detected']
            : ['Legitimate transaction attributes verified']),
          message: res.data.message || `Processed card authorization for ₹${this.simAmount.toLocaleString('en-IN')}`
        };
      }
    });
  }

  generateReport() {
    const csvRows = [
      ['Transaction ID', 'User', 'Amount', 'Status'],
      ...this.recentTransactions.map(tx => [tx.id, tx.customer, String(tx.amount), tx.status])
    ];
    const csvContent = csvRows.map(row => row.join(',')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'fraud-dashboard-report.csv';
    anchor.click();
    URL.revokeObjectURL(url);
    this.reportMessage = 'Report generated successfully and downloaded.';
    setTimeout(() => this.reportMessage = '', 4000);
  }

}