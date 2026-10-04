import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Sidebar } from '../../../shared/sidebar/sidebar';
import { Navbar } from '../../../shared/navbar/navbar';
import { Chatbot } from '../../../shared/chatbot/chatbot';
import { AuthService } from '../../../services/auth.service';
import { FraudDataService, VerificationHistoryItem } from '../../../services/fraud-data.service';

@Component({
  selector: 'app-user-history',
  standalone: true,
  imports: [CommonModule, Sidebar, Navbar, Chatbot],
  templateUrl: './user-history.html',
  styleUrl: './user-history.css'
})
export class UserHistory implements OnInit {
  userName = 'Priya Singh';
  userEmail = 'priya@example.com';
  historyList: VerificationHistoryItem[] = [];
  isLoading = false;

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
    this.loadHistory();
  }

  loadHistory(): void {
    this.isLoading = true;
    this.fraudData.fetchVerificationHistory(this.userEmail).subscribe({
      next: (list) => {
        this.historyList = list;
        this.isLoading = false;
      },
      error: () => {
        this.historyList = this.fraudData.getVerificationHistory(this.userName);
        this.isLoading = false;
      }
    });
  }
}
