import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class NotificationService {

  private permission: NotificationPermission = 'default';

  constructor(private router: Router) {
    if (typeof window !== 'undefined' && 'Notification' in window) {
      this.permission = Notification.permission;
    }
  }

  get isSupported(): boolean {
    return typeof window !== 'undefined' && 'Notification' in window;
  }

  get isGranted(): boolean {
    return this.permission === 'granted';
  }

  async requestPermission(): Promise<boolean> {
    if (!this.isSupported) {
      console.warn('Web Push Notifications not supported on this browser.');
      return false;
    }

    try {
      const result = await Notification.requestPermission();
      this.permission = result;
      if (result === 'granted') {
        this.sendPushNotification(
          '🛡️ AI Shield Real-Time Protection Active',
          'Live fraud alerts and transaction step-up authorizations will now be delivered directly to your device screen.'
        );
      }
      return result === 'granted';
    } catch (e) {
      console.error('Error requesting notification permission:', e);
      return false;
    }
  }

  sendPushNotification(
    title: string,
    body: string,
    routeUrl?: string,
    tag: string = 'ai-shield-alert'
  ): void {
    if (!this.isSupported) return;

    if (this.permission === 'granted') {
      try {
        const notification = new Notification(title, {
          body,
          icon: 'favicon.ico',
          badge: 'favicon.ico',
          tag,
          requireInteraction: true,
          silent: false
        });

        notification.onclick = (event) => {
          event.preventDefault();
          window.focus();
          notification.close();
          if (routeUrl) {
            this.router.navigateByUrl(routeUrl);
          }
        };
      } catch (e) {
        console.warn('Native notification failed, attempting serviceWorker fallback', e);
      }
    } else if (this.permission === 'default') {
      // Auto-prompt on first notification
      this.requestPermission().then(granted => {
        if (granted) {
          this.sendPushNotification(title, body, routeUrl, tag);
        }
      });
    }
  }

  sendFraudAlert(
    customerName: string,
    amount: number,
    merchantName: string,
    location: string,
    cardLast4: string,
    verificationId?: string
  ): void {
    const title = `🚨 AI SHIELD FRAUD ALERT • Card ending in ${cardLast4}`;
    const body = `Suspicious charge of ₹${amount.toLocaleString('en-IN')} at ${merchantName} (${location}) was flagged. Tap to authorize or freeze card immediately.`;
    const targetUrl = verificationId ? `/verify/${verificationId}` : '/user/alerts';

    this.sendPushNotification(title, body, targetUrl, `fraud-${Date.now()}`);
  }
}
