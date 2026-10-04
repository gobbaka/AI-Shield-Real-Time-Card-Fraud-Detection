import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { FraudDataService, TransactionVerification } from '../../services/fraud-data.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {

  displayName = 'Pradeep';
  displayRole = 'Administrator';
  searchTerm = '';
  searchResults: Array<{ label: string; route: string }> = [];

  // Mobile Device SMS / Push Notification State
  showMobilePhone = false;
  activeVerification: TransactionVerification | null = null;
  isAuthenticating = false;
  authMessage = '';
  authSuccess = false;

  constructor(
    private router: Router, 
    private auth: AuthService,
    private fraudData: FraudDataService,
    public notificationService: NotificationService
  ) {
    try {
      const raw = localStorage.getItem('profile');
      if (raw) {
        const p = JSON.parse(raw);
        this.displayName = p.name || this.displayName;
        this.displayRole = p.role || this.displayRole;
      }
    } catch (e) {
      console.warn('Failed to parse profile from localStorage', e);
    }

    window.addEventListener('profileUpdated', () => {
      try {
        const raw = localStorage.getItem('profile');
        if (raw) {
          const p = JSON.parse(raw);
          this.displayName = p.name || this.displayName;
          this.displayRole = p.role || this.displayRole;
        }
      } catch (e) {
        console.warn('Failed to parse profile from localStorage', e);
      }
    });

    // Auto-display phone notification if there's a pending verification
    setTimeout(() => {
      if (this.pendingVerifications.length > 0) {
        this.activeVerification = this.pendingVerifications[0];
        this.showMobilePhone = true;
      }
    }, 1200);
  }

  get pendingVerifications(): TransactionVerification[] {
    return this.fraudData.getPendingVerifications();
  }

  logout() {
    this.showLogoutConfirm = true;
  }

  showLogoutConfirm = false;
  toastMessage = '';
  showToastFlag = false;

  confirmLogout(doLogout: boolean) {
    this.showLogoutConfirm = false;
    if (!doLogout) return;
    this.auth.logout();
    this.router.navigate(['/']);
    this.showToast('Signed out');
  }

  showToast(msg: string, ms = 3000) {
    this.toastMessage = msg;
    this.showToastFlag = true;
    setTimeout(() => this.showToastFlag = false, ms);
  }

  enableNativeAlerts(): void {
    this.notificationService.requestPermission().then(granted => {
      if (granted) {
        this.showToast('🔔 Native Real Device Push Alerts Enabled!');
      } else {
        this.showToast('ℹ️ Notification permission required for alerts.');
      }
    });
  }

  goProfile() {
    this.router.navigate(['/profile']);
  }

  isListening = false;
  showNotifications = false;

  notifications = [
    {
      title: '🚨 High Risk Transaction',
      message: 'Card ****4582 flagged • ₹48,500',
      time: 'Just now'
    },
    {
      title: '📍 Suspicious Login',
      message: 'Unknown device • Delhi',
      time: '1 min ago'
    },
    {
      title: '🤖 AI Trust Score',
      message: 'System Health 98.9%',
      time: '3 mins ago'
    }
  ];

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
  }

  openNotification(idx: number) {
    const note = this.notifications[idx];
    if (!note) return;
    this.showToast(`${note.title}: ${note.message}`);
    this.notifications.splice(idx, 1);
  }

  toggleMobilePhone(v?: TransactionVerification) {
    this.activeVerification = v || this.pendingVerifications[0] || null;
    this.showMobilePhone = !this.showMobilePhone;
    this.authMessage = '';
    this.authSuccess = false;
    this.showNotifications = false;
  }

  closeMobilePhone() {
    this.showMobilePhone = false;
    this.isAuthenticating = false;
  }

  async confirmAuthorization() {
    if (!this.activeVerification) return;
    this.isAuthenticating = true;
    this.authMessage = '📱 Validating Device Biometric / Passkey...';

    setTimeout(() => {
      const reqId = this.activeVerification!.verificationRequestId;
      const challenge = this.activeVerification!.challengeToken;

      this.fraudData.confirmStepUpVerification(reqId, challenge).subscribe({
        next: (res) => {
          this.isAuthenticating = false;
          this.authSuccess = true;
          this.authMessage = '✅ Cardholder Authenticated: Transaction OWNER_VERIFIED.';
          this.showToast('Transaction confirmed via mobile device authentication!');
          setTimeout(() => {
            this.closeMobilePhone();
            this.authMessage = '';
          }, 2400);
        },
        error: (err) => {
          this.isAuthenticating = false;
          this.authMessage = '❌ Verification challenge failed or expired.';
        }
      });
    }, 1000);
  }

  rejectAuthorization() {
    if (!this.activeVerification) return;
    const reqId = this.activeVerification.verificationRequestId;
    this.isAuthenticating = true;
    this.authMessage = '🚨 Declining transaction & freezing card...';

    this.fraudData.rejectVerification(reqId, 'Cardholder confirmed unauthorized fraud').subscribe({
      next: (res) => {
        this.isAuthenticating = false;
        this.authSuccess = false;
        this.authMessage = '🛡️ Fraud Confirmed: Card frozen, transaction declined, case escalated.';
        this.showToast('Card blocked & escalated to fraud investigation team.');
        setTimeout(() => {
          this.closeMobilePhone();
          this.authMessage = '';
        }, 2500);
      },
      error: () => {
        this.isAuthenticating = false;
        this.closeMobilePhone();
      }
    });
  }

  startVoice() {
    const SpeechRecognition =
      (window as any).webkitSpeechRecognition ||
      (window as any).SpeechRecognition;

    if (!SpeechRecognition) {
      this.showToast('⚠️ Voice recognition is not supported in this browser.');
      return;
    }

    // If currently listening, toggle off
    if (this.isListening) {
      this.isListening = false;
      this.showToast('Mic deactivated');
      return;
    }

    this.isListening = true;

    // 1. Activate & Request Browser Push Notifications
    this.notificationService.requestPermission().then(granted => {
      if (granted) {
        this.notificationService.sendPushNotification(
          '🎙️ Voice Assistant & Live Alerts Active',
          'Microphone listening for command. Say "Dashboard", "Transactions", "Fraud Alerts", "Cards", or "Analytics".'
        );
      }
    });

    // 2. Play Audio Speech Confirmation
    this.speakFeedback('Voice assistant activated. Notifications are active. Listening for your command.');

    // 3. Display In-App Notification Toast
    this.showToast('🎙️ Mic Activated: Listening for commands... (🔔 Notifications Active)', 4500);

    const recognition = new SpeechRecognition();
    recognition.lang = 'en-US';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    recognition.start();

    recognition.onresult = (event: any) => {
      this.isListening = false;
      const command = event.results[0][0].transcript.toLowerCase().trim();
      this.handleVoiceCommand(command);
    };

    recognition.onerror = (event: any) => {
      this.isListening = false;
      this.showToast(`Voice assistant error: ${event.error || 'Speech capture failed'}`);
    };

    recognition.onend = () => {
      this.isListening = false;
    };
  }

  private handleVoiceCommand(command: string): void {
    const isUserRole = this.auth.getRole() === 'USER';

    if (command.includes('dashboard') || command.includes('home') || command.includes('overview')) {
      const route = isUserRole ? '/user/dashboard' : '/dashboard';
      this.executeVoiceAction('Dashboard', route);
    } else if (command.includes('transaction') || command.includes('payment') || command.includes('history')) {
      const route = isUserRole ? '/user/transactions' : '/transactions';
      this.executeVoiceAction('Transactions', route);
    } else if (command.includes('fraud') || command.includes('alert')) {
      const route = isUserRole ? '/user/alerts' : '/fraud-alert';
      this.executeVoiceAction('Fraud Alerts', route);
    } else if (command.includes('card') || command.includes('travel') || command.includes('freeze')) {
      this.executeVoiceAction('My Cards & Travel Mode', '/user/cards');
    } else if (command.includes('analytics') || command.includes('report') || command.includes('chart')) {
      this.executeVoiceAction('Analytics & Reports', '/analytics');
    } else if (command.includes('customer') || command.includes('user')) {
      this.executeVoiceAction('Customers', '/customers');
    } else if (command.includes('live') || command.includes('tracking') || command.includes('map')) {
      this.executeVoiceAction('Live Geo-Tracking', '/live-tracking');
    } else if (command.includes('setting') || command.includes('model') || command.includes('rule')) {
      this.executeVoiceAction('Settings & AI Governance', '/settings');
    } else if (command.includes('profile') || command.includes('account')) {
      this.executeVoiceAction('User Profile', '/profile');
    } else if (command.includes('notification')) {
      this.showNotifications = true;
      this.showToast('🔔 Opening security notifications panel');
      this.speakFeedback('Opening notifications feed');
    } else {
      this.showToast(`🗣️ Heard: "${command}" (No direct page match)`, 3500);
      this.speakFeedback(`Recognized ${command}. Try saying Dashboard, Transactions, or Alerts.`);
    }
  }

  private executeVoiceAction(pageName: string, route: string): void {
    this.showToast(`⚡ Voice Command Executed: Opening ${pageName}...`);
    this.speakFeedback(`Opening ${pageName}`);
    this.notificationService.sendPushNotification(
      `⚡ Voice Action: ${pageName}`,
      `Successfully navigated to ${pageName} via voice command.`
    );
    this.router.navigate([route]);
  }

  private speakFeedback(text: string): void {
    try {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 1.05;
        utterance.pitch = 1.0;
        window.speechSynthesis.speak(utterance);
      }
    } catch {}
  }

  updateSearch(): void {
    const query = this.searchTerm.toLowerCase().trim();
    const pages = [
      { label: 'Transactions', route: '/transactions' }, 
      { label: 'Fraud Alert Center', route: '/fraud-alert' }, 
      { label: 'Analytics', route: '/analytics' }, 
      { label: 'Customers', route: '/customers' }, 
      { label: 'Live Tracking', route: '/live-tracking' }
    ];
    this.searchResults = query ? pages.filter(item => item.label.toLowerCase().includes(query)) : [];
  }

  openSearchResult(result: { route: string }): void { 
    this.router.navigate([result.route]); 
    this.searchTerm = ''; 
    this.searchResults = []; 
  }
}