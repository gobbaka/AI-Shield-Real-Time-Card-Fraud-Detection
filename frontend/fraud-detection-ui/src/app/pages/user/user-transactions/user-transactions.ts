import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Sidebar } from '../../../shared/sidebar/sidebar';
import { Navbar } from '../../../shared/navbar/navbar';
import { Chatbot } from '../../../shared/chatbot/chatbot';
import { AuthService } from '../../../services/auth.service';
import { FraudDataService, Transaction } from '../../../services/fraud-data.service';

@Component({
  selector: 'app-user-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule, Sidebar, Navbar, Chatbot],
  templateUrl: './user-transactions.html',
  styleUrl: './user-transactions.css'
})
export class UserTransactions implements OnInit {
  userName = 'Priya Singh';
  userEmail = 'priya@example.com';
  transactions: Transaction[] = [];
  isLoading = false;
  search = '';
  statusFilter = 'All';
  selectedTx: Transaction | null = null;
  disputeMessage = '';

  constructor(
    private auth: AuthService,
    private fraudData: FraudDataService
  ) {}

  ngOnInit(): void {
    const user = this.auth.getUser();
    if (user) {
      if (user.name) this.userName = user.name;
      if (user.email) this.userEmail = user.email;
    }
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.isLoading = true;
    this.fraudData.fetchUserTransactions(this.userEmail, this.userName).subscribe({
      next: (txs) => {
        this.transactions = txs;
        this.isLoading = false;
      },
      error: () => {
        this.transactions = this.fraudData.getUserTransactions(this.userName);
        this.isLoading = false;
      }
    });
  }

  get filteredTransactions(): Transaction[] {
    const q = this.search.toLowerCase().trim();
    return this.transactions.filter(tx => {
      const matchesSearch = !q || 
        tx.id.toLowerCase().includes(q) ||
        (tx.merchantName && tx.merchantName.toLowerCase().includes(q)) ||
        tx.location.toLowerCase().includes(q);

      const matchesStatus = this.statusFilter === 'All' || tx.status === this.statusFilter;
      return matchesSearch && matchesStatus;
    });
  }

  selectTx(tx: Transaction): void {
    this.selectedTx = tx;
  }

  disputeTransaction(tx: Transaction): void {
    this.fraudData.fileDispute(tx.id, 'Unauthorized charge flagged by cardholder in portal', this.userEmail).subscribe({
      next: (res) => {
        tx.status = 'CUSTOMER_REPORTED_FRAUD';
        this.disputeMessage = `🚨 Dispute logged for ${tx.id}. Case submitted to Bank Fraud Operations for investigation.`;
        this.selectedTx = null;
        setTimeout(() => this.disputeMessage = '', 4000);
      },
      error: () => {
        tx.status = 'CUSTOMER_REPORTED_FRAUD';
        this.disputeMessage = `🚨 Dispute recorded locally for ${tx.id}. Our security team will review it.`;
        this.selectedTx = null;
        setTimeout(() => this.disputeMessage = '', 4000);
      }
    });
  }
}
