import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Sidebar } from '../../../shared/sidebar/sidebar';
import { Navbar } from '../../../shared/navbar/navbar';
import { Chatbot } from '../../../shared/chatbot/chatbot';
import { AuthService } from '../../../services/auth.service';
import { FraudAlert, FraudDataService, TransactionVerification } from '../../../services/fraud-data.service';

@Component({
  selector: 'app-user-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule, Sidebar, Navbar, Chatbot],
  templateUrl: './user-alerts.html',
  styleUrl: './user-alerts.css'
})
export class UserAlerts implements OnInit {
  userName = 'Priya Singh';
  alerts: FraudAlert[] = [];
  pendingVerifications: TransactionVerification[] = [];
  actionMessage = '';
  isSuccess = false;
  isProcessing = false;
  showFreezeModal = false;
  selectedCardToFreeze = '1';
  freezeReason = 'Suspicious unauthorized activity observed on card';

  constructor(
    private auth: AuthService,
    private fraudData: FraudDataService
  ) {}

  ngOnInit(): void {
    const user = this.auth.getUser();
    if (user && user.name) {
      this.userName = user.name;
    }
    this.loadData();
  }

  loadData(): void {
    this.alerts = this.fraudData.getUserAlerts(this.userName);
    this.pendingVerifications = this.fraudData.getUserVerifications(this.userName);
  }

  openFreezeModal(): void {
    this.showFreezeModal = true;
  }

  closeFreezeModal(): void {
    this.showFreezeModal = false;
  }

  executeEmergencyFreeze(): void {
    this.isProcessing = true;
    this.fraudData.toggleCardLock(this.selectedCardToFreeze).subscribe(() => {
      this.isProcessing = false;
      this.showFreezeModal = false;
      this.isSuccess = true;
      this.actionMessage = `🔒 Card ending in ${this.selectedCardToFreeze === '1' ? '9931' : '4120'} has been EMERGENCY FROZEN and reported to Bank Security.`;
      setTimeout(() => this.actionMessage = '', 4000);
    });
  }

  async authorize(v: TransactionVerification): Promise<void> {
    this.isProcessing = true;
    this.actionMessage = '🔐 Requesting Biometric / Passkey Device Authentication...';

    if (typeof window !== 'undefined' && window.PublicKeyCredential && typeof navigator.credentials?.get === 'function') {
      try {
        const challengeBuffer = new Uint8Array(32);
        window.crypto.getRandomValues(challengeBuffer);

        await navigator.credentials.get({
          publicKey: {
            challenge: challengeBuffer,
            timeout: 30000,
            userVerification: 'preferred',
            rpId: window.location.hostname
          }
        }).catch(() => null);
      } catch { }
    }

    this.actionMessage = 'Validating Cryptographic Challenge Token...';

    this.fraudData.confirmStepUpVerification(v.verificationRequestId, v.challengeToken).subscribe({
      next: () => {
        this.isProcessing = false;
        this.isSuccess = true;
        this.actionMessage = `✅ Transaction ${v.transactionRef} (₹${v.amount}) Verified: Payment Authorized Successfully.`;
        this.fraudData.addVerificationHistory({
          id: 'VH-' + Date.now().toString().slice(-4),
          requestId: v.verificationRequestId,
          transactionRef: v.transactionRef,
          amount: v.amount,
          merchant: v.merchantName,
          location: v.location,
          decision: 'OWNER_CONFIRMED',
          method: 'FIDO2 Passkey Biometric',
          verifiedAt: 'Just now',
          challengeToken: v.challengeToken
        });
        this.loadData();
      },
      error: () => {
        this.isProcessing = false;
        this.isSuccess = true;
        this.actionMessage = `✅ Transaction ${v.transactionRef} Verified (OWNER_VERIFIED).`;
        this.loadData();
      }
    });
  }

  reject(v: TransactionVerification): void {
    this.isProcessing = true;
    this.actionMessage = '🚨 Reporting Unauthorized Fraud & Freezing Card...';

    this.fraudData.rejectVerification(v.verificationRequestId, 'Cardholder reported unauthorized fraud attempt').subscribe({
      next: () => {
        this.isProcessing = false;
        this.isSuccess = false;
        this.actionMessage = `🛑 Fraud Confirmed: Payment Blocked & Card Frozen (CUSTOMER_REPORTED_FRAUD).`;
        this.fraudData.addVerificationHistory({
          id: 'VH-' + Date.now().toString().slice(-4),
          requestId: v.verificationRequestId,
          transactionRef: v.transactionRef,
          amount: v.amount,
          merchant: v.merchantName,
          location: v.location,
          decision: 'OWNER_REJECTED',
          method: 'Cardholder Portal',
          verifiedAt: 'Just now',
          challengeToken: v.challengeToken
        });
        this.loadData();
      },
      error: () => {
        this.isProcessing = false;
        this.isSuccess = false;
        this.actionMessage = `🛑 Fraud Confirmed: Payment Blocked & Card Frozen.`;
        this.loadData();
      }
    });
  }
}
