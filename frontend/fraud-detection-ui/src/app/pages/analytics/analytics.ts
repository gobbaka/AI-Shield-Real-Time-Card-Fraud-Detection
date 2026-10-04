import { Component, OnInit } from '@angular/core';
import { Sidebar } from '../../shared/sidebar/sidebar';
import { Navbar } from '../../shared/navbar/navbar';
import { Chatbot } from '../../shared/chatbot/chatbot';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FraudDataService } from '../../services/fraud-data.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, Sidebar, Navbar, Chatbot],
  templateUrl: './analytics.html',
  styleUrl: './analytics.css'
})
export class AnalyticsComponent implements OnInit {
  timeRange = '7d';
  serverAnalytics: any = null;

  constructor(private readonly data: FraudDataService) {}

  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    this.data.fetchAnalytics(this.timeRange).subscribe(res => {
      if (res && res.data) {
        this.serverAnalytics = res.data;
      }
    });
  }

  onRangeChange(): void {
    this.loadAnalytics();
  }

  get analytics() { 
    return this.serverAnalytics || this.data.getAnalytics(this.timeRange); 
  }

  get modelAccuracy(): number {
    if (this.serverAnalytics && this.serverAnalytics.modelAccuracy !== undefined) {
      return Math.round(this.serverAnalytics.modelAccuracy);
    }
    return 98;
  }

  get protectedAmount(): number {
    if (this.serverAnalytics && this.serverAnalytics.protectedAmount !== undefined) {
      return this.serverAnalytics.protectedAmount;
    }
    return this.data.getTransactions().filter(transaction => transaction.status === 'Blocked').reduce((total, transaction) => total + transaction.amount, 0);
  }
  downloadReport(): void {
    const payload = JSON.stringify({ range: this.timeRange, ...this.analytics }, null, 2);
    const url = URL.createObjectURL(new Blob([payload], { type: 'application/json' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'analytics-report.json';
    link.click();
    URL.revokeObjectURL(url);
  }
}