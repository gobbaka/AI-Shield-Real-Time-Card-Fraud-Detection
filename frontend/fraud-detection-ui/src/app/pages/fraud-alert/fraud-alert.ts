import { Component } from '@angular/core';
import { Sidebar } from '../../shared/sidebar/sidebar';
import { Navbar } from '../../shared/navbar/navbar';
import { Chatbot } from '../../shared/chatbot/chatbot';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FraudAlert, FraudDataService } from '../../services/fraud-data.service';

@Component({
  selector: 'app-fraud-alert',
  standalone: true,
imports: [CommonModule, FormsModule, Sidebar, Navbar, Chatbot],
  templateUrl: './fraud-alert.html',
  styleUrl: './fraud-alert.css'
})
export class FraudAlertComponent {
  search = '';
  filter = 'All';
  selected: FraudAlert | null = null;
  investigatingCase: FraudAlert | null = null;
  investigationNotes: string = '';
  actionFeedback: string = '';
  loading = false;
  error = '';

  constructor(private readonly data: FraudDataService) {}

  get alerts(): FraudAlert[] { 
    const query = this.search.toLowerCase().trim(); 
    return this.data.getAlerts().filter(item => (!query || `${item.id} ${item.customer} ${item.location}`.toLowerCase().includes(query)) && (this.filter === 'All' || item.status === this.filter)); 
  }

  get criticalCount(): number { return this.data.getAlerts().filter(item => item.riskScore >= 90 && item.status !== 'Resolved').length; }
  get reviewCount(): number { return this.data.getAlerts().filter(item => item.status === 'Reviewing' || item.status === 'Open').length; }
  get resolvedCount(): number { return this.data.getAlerts().filter(item => item.status === 'Resolved').length; }

  refresh(): void { 
    this.loading = true; 
    setTimeout(() => this.loading = false, 250); 
  }

  setStatus(alert: FraudAlert, status: FraudAlert['status']): void { 
    this.data.updateAlert(alert.id, status); 
  }

  investigateAlert(alert: FraudAlert): void {
    this.investigatingCase = alert;
    this.investigationNotes = `AI Shield Case ${alert.id}: Initial assessment flagged ${alert.riskScore}% risk at ${alert.location}.`;
    this.actionFeedback = '';
  }

  closeInvestigation(): void {
    this.investigatingCase = null;
    this.actionFeedback = '';
  }

  dispatchVerificationSms(alert: FraudAlert): void {
    this.actionFeedback = `📲 Out-of-band Step-Up verification SMS re-dispatched to ${alert.customer}'s registered mobile number.`;
    setTimeout(() => this.actionFeedback = '', 3500);
  }

  emergencyFreezeCard(alert: FraudAlert): void {
    this.setStatus(alert, 'Blocked');
    this.actionFeedback = `🔒 Card for ${alert.customer} has been permanently FROZEN in the central banking core.`;
    setTimeout(() => this.actionFeedback = '', 3500);
  }

  resolveInvestigationCase(alert: FraudAlert): void {
    this.setStatus(alert, 'Resolved');
    this.actionFeedback = `✅ Fraud Case ${alert.id} resolved as Legitimate / Authorized.`;
    setTimeout(() => {
      this.closeInvestigation();
    }, 1500);
  }
}