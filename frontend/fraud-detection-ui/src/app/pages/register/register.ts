import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  name = '';
  email = '';
  phone = '';
  password = '';
  confirmPassword = '';
  
  // OTP Verification state
  step: 'REGISTRATION' | 'OTP_VERIFICATION' = 'REGISTRATION';
  challengeId = '';
  otpCode = '';
  otpDestination = '';
  cooldownSeconds = 60;
  cooldownTimer: any = null;

  loading = false;
  feedbackMessage = '';
  feedbackType: 'error' | 'success' = 'error';

  constructor(private router: Router, private authService: AuthService) {}

  register() {
    if (!this.name || !this.email || !this.phone || !this.password || !this.confirmPassword) {
      this.feedbackType = 'error';
      this.feedbackMessage = 'Please fill in all fields including your mobile phone number.';
      return;
    }

    const cleanPhone = this.phone.trim().replaceAll(' ', '');
    if (cleanPhone.length < 10) {
      this.feedbackType = 'error';
      this.feedbackMessage = 'Please enter a valid 10-digit mobile number for security alerts.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.feedbackType = 'error';
      this.feedbackMessage = 'Passwords do not match.';
      return;
    }

    this.loading = true;
    this.feedbackMessage = '';

    this.authService.registerUser(this.name.trim(), this.email.trim(), this.password, cleanPhone)
      .subscribe({
        next: (res) => {
          this.loading = false;
          if (res && res.success && res.data) {
            if (res.data.step === 'AUTHENTICATED' && res.data.authResponse) {
              this.feedbackType = 'success';
              this.feedbackMessage = 'Account created successfully! Redirecting...';
              setTimeout(() => this.router.navigate(['/user/dashboard']), 500);
              return;
            }
            this.challengeId = res.data.challengeId || '';
            this.otpDestination = res.data.destinationMasked || cleanPhone;
            this.step = 'OTP_VERIFICATION';
            this.feedbackType = 'success';
            this.feedbackMessage = `Security OTP sent to ${this.otpDestination}. Please verify to activate protection.`;
            this.startCooldown(res.data.resendAvailableInSeconds || 60);
          }
        },
        error: (err) => {
          this.loading = false;
          this.feedbackType = 'error';
          this.feedbackMessage = err.error?.message || 'Registration failed. Please check your details.';
        }
      });
  }

  verifyOtp() {
    if (!this.otpCode || this.otpCode.trim().length !== 6) {
      this.feedbackType = 'error';
      this.feedbackMessage = 'Please enter the 6-digit security code sent to your mobile.';
      return;
    }

    this.loading = true;
    this.feedbackMessage = '';

    this.authService.verifyLoginOtp(this.challengeId, this.otpCode.trim())
      .subscribe({
        next: (res) => {
          this.loading = false;
          this.feedbackType = 'success';
          this.feedbackMessage = 'Mobile number verified successfully! Redirecting to your Dashboard...';
          setTimeout(() => {
            this.router.navigate(['/user/dashboard']);
          }, 800);
        },
        error: (err) => {
          this.loading = false;
          this.feedbackType = 'error';
          this.feedbackMessage = err.error?.message || 'Invalid or expired OTP. Please try again.';
        }
      });
  }

  resendOtp() {
    if (this.cooldownSeconds > 0) return;

    this.loading = true;
    this.feedbackMessage = '';

    this.authService.resendLoginOtp(this.challengeId)
      .subscribe({
        next: (res) => {
          this.loading = false;
          this.feedbackType = 'success';
          this.feedbackMessage = 'A new security code has been dispatched to your mobile phone.';
          this.startCooldown(res.data?.resendAvailableInSeconds || 60);
        },
        error: (err) => {
          this.loading = false;
          this.feedbackType = 'error';
          this.feedbackMessage = err.error?.message || 'Failed to resend OTP. Please wait and try again.';
        }
      });
  }

  startCooldown(seconds = 60) {
    this.cooldownSeconds = seconds;
    if (this.cooldownTimer) clearInterval(this.cooldownTimer);
    this.cooldownTimer = setInterval(() => {
      if (this.cooldownSeconds > 0) {
        this.cooldownSeconds--;
      } else {
        clearInterval(this.cooldownTimer);
      }
    }, 1000);
  }
}
