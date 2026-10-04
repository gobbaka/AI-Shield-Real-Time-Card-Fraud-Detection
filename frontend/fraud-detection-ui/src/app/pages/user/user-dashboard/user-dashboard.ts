import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Sidebar } from '../../../shared/sidebar/sidebar';
import { Navbar } from '../../../shared/navbar/navbar';
import { Chatbot } from '../../../shared/chatbot/chatbot';
import { AuthService } from '../../../services/auth.service';
import { FraudDataService, Transaction, TransactionVerification, UserCard } from '../../../services/fraud-data.service';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, Sidebar, Navbar, Chatbot],
  templateUrl: './user-dashboard.html',
  styleUrl: './user-dashboard.css'
})
export class UserDashboard implements OnInit {
  userName = 'Priya Singh';
  userEmail = 'priya@example.com';
  cards: UserCard[] = [];
  recentTxns: Transaction[] = [];
  pendingVerifications: TransactionVerification[] = [];
  securityScore = 96;

  constructor(
    private auth: AuthService,
    private fraudData: FraudDataService
  ) {}

  ngOnInit(): void {
    const user = this.auth.getUser();
    if (user) {
      this.userName = user.name || 'Cardholder';
      this.userEmail = user.email || 'priya@example.com';
    }

    this.fraudData.fetchUserCards(this.userEmail, this.userName).subscribe(cards => {
      this.cards = cards;
    });
    this.recentTxns = this.fraudData.getUserTransactions(this.userName);
    this.pendingVerifications = this.fraudData.getUserVerifications(this.userName);
  }

  get activeCardsCount(): number {
    return this.cards.filter(c => c.status === 'ACTIVE').length;
  }

  get totalAvailableCredit(): number {
    return this.cards.reduce((sum, c) => sum + c.availableLimit, 0);
  }
}
