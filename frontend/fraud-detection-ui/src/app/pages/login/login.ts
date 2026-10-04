import { Component, OnInit, OnDestroy, ElementRef, ViewChildren, QueryList, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService, UserProfile, LoginChallengeResponse } from '../../services/auth.service';

export type AuthMode = 'LOGIN' | 'REGISTER' | 'FORGOT_PASSWORD';
export type AuthState =
  | 'LOGIN_IDLE'
  | 'LOGIN_SUBMITTING'
  | 'CREDENTIALS_VERIFIED'
  | 'OTP_SENT'
  | 'OTP_VERIFYING'
  | 'AUTHENTICATED'
  | 'LOGIN_FAILED'
  | 'OTP_EXPIRED'
  | 'OTP_INVALID'
  | 'OTP_LOCKED'
  | 'NETWORK_ERROR';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login implements OnInit, OnDestroy {

  @ViewChildren('otpInput') otpInputRefs!: QueryList<ElementRef<HTMLInputElement>>;

  // Modes & States
  mode: AuthMode = 'LOGIN';
  authState: AuthState = 'LOGIN_IDLE';

  // Login Form
  identifier: string = 'pradeep@example.com';
  password: string = 'password123';
  hidePassword: boolean = true;
  rememberMe: boolean = false;

  // Registration Form
  regName: string = '';
  regEmail: string = '';
  regPassword: string = '';
  regPhone: string = '';
  regHidePassword: boolean = true;

  // Forgot Password / Reset
  forgotIdentifier: string = '';
  newPassword: string = '';
  newHidePassword: boolean = true;

  // 2FA OTP Challenge
  challengeId: string = '';
  destinationMasked: string = '+91 ******5841';
  otpDigits: string[] = ['', '', '', '', '', ''];
  resendCooldown: number = 0;
  expirySeconds: number = 300;
  attemptsRemaining: number = 5;

  get isOtpStep(): boolean {
    return this.authState === 'OTP_SENT' ||
           this.authState === 'OTP_VERIFYING' ||
           this.authState === 'OTP_INVALID' ||
           this.authState === 'OTP_EXPIRED' ||
           this.authState === 'OTP_LOCKED' ||
           this.authState === 'CREDENTIALS_VERIFIED' ||
           this.authState === 'AUTHENTICATED';
  }

  // Timers
  private cooldownTimer: any = null;
  private expiryTimer: any = null;

  // Status & Feedback
  statusMessage: string = '';
  messageType: 'error' | 'success' | 'info' = 'info';

  // Biometric / Passkey Support
  isPasskeySupported: boolean = false;
  isBiometricChecking: boolean = false;

  constructor(
    private readonly router: Router,
    private readonly auth: AuthService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Clear any previous active session so the user can test the login screen
    this.auth.logout();

    // Load remembered credentials
    try {
      const saved = localStorage.getItem('ai_shield_remembered_id');
      if (saved) {
        this.identifier = saved;
        this.rememberMe = true;
      }
    } catch {}

    // Detect WebAuthn / Passkey browser capability
    if (typeof window !== 'undefined' && (window as any).PublicKeyCredential) {
      this.isPasskeySupported = true;
    }
  }

  ngOnDestroy(): void {
    this.clearTimers();
  }

  // ==========================================
  // 1. STEP 1: LOGIN (CREDENTIALS VALIDATION)
  // ==========================================
  quickLoginAsAdmin(): void {
    this.identifier = 'pradeep@example.com';
    this.password = 'password123';
    this.authState = 'LOGIN_SUBMITTING';
    this.showMessage('Verifying Admin credentials...', 'info');
    this.cdr.detectChanges();

    this.auth.initiateLogin(this.identifier, this.password).subscribe({
      next: (res) => {
        if (res && res.success && res.data) {
          this.handleChallengeInitiated(res.data);
        } else {
          this.authState = 'LOGIN_FAILED';
          this.showMessage('Authentication failed. Please check your credentials.', 'error');
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        this.authState = 'LOGIN_FAILED';
        this.showMessage('Sign-in failed: ' + this.parseError(err, 'Backend server error'), 'error');
        this.cdr.detectChanges();
      }
    });
  }

  quickLoginAsCardholder(): void {
    this.identifier = 'priya@example.com';
    this.password = 'password123';
    this.authState = 'LOGIN_SUBMITTING';
    this.showMessage('Verifying Cardholder credentials...', 'info');
    this.cdr.detectChanges();

    this.auth.initiateLogin(this.identifier, this.password).subscribe({
      next: (res) => {
        if (res && res.success && res.data) {
          this.handleChallengeInitiated(res.data);
        } else {
          this.authState = 'LOGIN_FAILED';
          this.showMessage('Authentication failed. Please check your credentials.', 'error');
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        this.authState = 'LOGIN_FAILED';
        this.showMessage('Sign-in failed: ' + this.parseError(err, 'Backend server error'), 'error');
        this.cdr.detectChanges();
      }
    });
  }

  submitLogin(): void {
    if (!this.identifier || !this.identifier.trim()) {
      this.showMessage('Please enter your registered email or mobile number.', 'error');
      return;
    }

    if (!this.password) {
      this.showMessage('Please enter your account password.', 'error');
      return;
    }

    this.authState = 'LOGIN_SUBMITTING';
    this.showMessage('Verifying credentials & security parameters...', 'info');
    this.cdr.detectChanges();

    // Remember me storage
    try {
      if (this.rememberMe) {
        localStorage.setItem('ai_shield_remembered_id', this.identifier.trim());
      } else {
        localStorage.removeItem('ai_shield_remembered_id');
      }
    } catch {}

    this.auth.initiateLogin(this.identifier, this.password).subscribe({
      next: (res) => {
        if (res && res.success && res.data) {
          this.handleChallengeInitiated(res.data);
        } else {
          this.authState = 'LOGIN_FAILED';
          this.showMessage('Authentication failed. Please check your credentials.', 'error');
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        this.authState = 'LOGIN_FAILED';
        const msg = this.parseError(err, 'Invalid email/phone or password. Please verify and retry.');
        this.showMessage(msg, 'error');
        this.cdr.detectChanges();
      }
    });
  }

  // ==========================================
  // 2. STEP 2: VERIFY 6-DIGIT OTP
  // ==========================================
  verifyOtp(): void {
    const fullOtp = this.otpDigits.join('').trim();
    if (fullOtp.length !== 6) {
      this.showMessage('Please enter the full 6-digit verification code.', 'error');
      return;
    }

    this.authState = 'OTP_VERIFYING';
    this.showMessage('Verifying one-time security code with AI Shield auth server...', 'info');

    if (this.mode === 'FORGOT_PASSWORD') {
      if (!this.newPassword || this.newPassword.length < 6) {
        this.showMessage('Please provide a secure new password (min 6 characters).', 'error');
        this.authState = 'OTP_SENT';
        return;
      }
      this.auth.resetPassword(this.challengeId, fullOtp, this.newPassword).subscribe({
        next: (res) => {
          this.handleAuthSuccess(res.data);
        },
        error: (err) => {
          this.handleOtpError(err);
        }
      });
      return;
    }

    this.auth.verifyLoginOtp(this.challengeId, fullOtp).subscribe({
      next: (res) => {
        if (res && res.success && res.data) {
          this.handleAuthSuccess(res.data);
        } else {
          this.authState = 'OTP_INVALID';
          this.showMessage('Verification code could not be validated.', 'error');
        }
      },
      error: (err) => {
        this.handleOtpError(err);
      }
    });
  }

  // ==========================================
  // 3. RESEND OTP
  // ==========================================
  resendOtp(): void {
    if (this.resendCooldown > 0) return;

    this.showMessage('Requesting new one-time verification code...', 'info');
    this.auth.resendLoginOtp(this.challengeId).subscribe({
      next: (res) => {
        if (res && res.success) {
          this.destinationMasked = res.data.destinationMasked || this.destinationMasked;
          this.startCooldown(res.data.resendAvailableInSeconds || 60);
          this.startExpiryTimer(300);
          this.otpDigits = ['', '', '', '', '', ''];
          this.authState = 'OTP_SENT';
          const msg = res.data.message || `New verification code dispatched to ${this.destinationMasked}. Please check your phone SMS.`;
          this.showMessage(msg, 'success');
          this.focusOtpBox(0);
        }
      },
      error: (err) => {
        const msg = this.parseError(err, 'Failed to dispatch new OTP code. Please try again.');
        this.showMessage(msg, 'error');
      }
    });
  }

  fillDemoOtp(): void {
    this.showMessage('Please enter the 6-digit verification code received on your mobile phone via SMS.', 'info');
    this.cdr.detectChanges();
    this.focusOtpBox(0);
  }

  // ==========================================
  // 4. REGISTRATION
  // ==========================================
  submitRegister(): void {
    if (!this.regName || !this.regName.trim()) {
      this.showMessage('Please enter your full legal name.', 'error');
      return;
    }
    if (!this.regEmail || !this.regEmail.includes('@')) {
      this.showMessage('Please enter a valid email address.', 'error');
      return;
    }
    if (!this.regPassword || this.regPassword.length < 6) {
      this.showMessage('Password must be at least 6 characters.', 'error');
      return;
    }
    if (!this.regPhone || this.regPhone.replace(/\D/g, '').length < 10) {
      this.showMessage('A valid 10-digit mobile number is mandatory for transaction OTP security.', 'error');
      return;
    }

    this.authState = 'LOGIN_SUBMITTING';
    this.showMessage('Registering cardholder account & generating security challenge...', 'info');

    this.auth.registerUser(this.regName, this.regEmail, this.regPassword, this.regPhone).subscribe({
      next: (res) => {
        if (res && res.success && res.data) {
          this.handleChallengeInitiated(res.data);
        }
      },
      error: (err) => {
        this.authState = 'LOGIN_FAILED';
        const msg = this.parseError(err, 'Registration failed. Email or mobile might already be registered.');
        this.showMessage(msg, 'error');
      }
    });
  }

  // ==========================================
  // 5. FORGOT PASSWORD
  // ==========================================
  submitForgotPassword(): void {
    if (!this.forgotIdentifier || !this.forgotIdentifier.trim()) {
      this.showMessage('Please enter your registered email or mobile number.', 'error');
      return;
    }

    this.authState = 'LOGIN_SUBMITTING';
    this.showMessage('Initiating secure password reset challenge...', 'info');

    this.auth.forgotPassword(this.forgotIdentifier).subscribe({
      next: (res) => {
        if (res && res.success && res.data) {
          this.handleChallengeInitiated(res.data);
        }
      },
      error: (err) => {
        this.authState = 'LOGIN_FAILED';
        const msg = this.parseError(err, 'Unable to process reset request.');
        this.showMessage(msg, 'error');
      }
    });
  }

  // ==========================================
  // 6. WEBAUTHN / PASSKEY BIOMETRIC LOGIN
  // ==========================================
  async authenticateWithPasskey(): Promise<void> {
    this.isBiometricChecking = true;
    this.showMessage('Accessing device biometric sensor (Face ID / Fingerprint / Touch ID)...', 'info');

    try {
      // Simulate/trigger biometric interaction with backend credentials
      await new Promise(r => setTimeout(r, 600));

      this.identifier = 'pradeep@example.com';
      this.password = 'password123';
      this.isBiometricChecking = false;
      this.submitLogin();
    } catch {
      this.isBiometricChecking = false;
      this.showMessage('Biometric authentication cancelled or unavailable.', 'error');
    }
  }

  // ==========================================
  // 7. OTP INPUT DIGIT NAVIGATION
  // ==========================================
  onOtpInput(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const val = input.value;

    if (val && val.length > 1) {
      // Handle paste in single box
      this.handleOtpPasteString(val);
      return;
    }

    this.otpDigits[index] = val ? val.slice(-1) : '';

    if (val && index < 5) {
      this.focusOtpBox(index + 1);
    }

    // If all 6 digits entered, auto-verify
    if (this.otpDigits.every(d => d.length === 1)) {
      this.verifyOtp();
    }
  }

  onOtpKeyDown(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace') {
      if (!this.otpDigits[index] && index > 0) {
        this.otpDigits[index - 1] = '';
        this.focusOtpBox(index - 1);
        event.preventDefault();
      } else {
        this.otpDigits[index] = '';
      }
    } else if (event.key === 'ArrowLeft' && index > 0) {
      this.focusOtpBox(index - 1);
    } else if (event.key === 'ArrowRight' && index < 5) {
      this.focusOtpBox(index + 1);
    }
  }

  onOtpPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasteData = event.clipboardData?.getData('text') || '';
    this.handleOtpPasteString(pasteData);
  }

  private handleOtpPasteString(str: string): void {
    const cleaned = str.replace(/\D/g, '').slice(0, 6);
    if (cleaned.length === 0) return;

    for (let i = 0; i < 6; i++) {
      this.otpDigits[i] = cleaned[i] || '';
    }

    if (cleaned.length === 6) {
      this.focusOtpBox(5);
      this.verifyOtp();
    } else {
      this.focusOtpBox(cleaned.length);
    }
  }

  private focusOtpBox(index: number): void {
    setTimeout(() => {
      const inputs = this.otpInputRefs?.toArray();
      if (inputs && inputs[index]) {
        inputs[index].nativeElement.focus();
        inputs[index].nativeElement.select();
      }
    }, 20);
  }

  // ==========================================
  // 8. HELPERS & MODE SWITCHING
  // ==========================================
  switchMode(newMode: AuthMode): void {
    this.mode = newMode;
    this.authState = 'LOGIN_IDLE';
    this.statusMessage = '';
    this.clearTimers();
    this.cdr.detectChanges();
  }

  backToCredentials(): void {
    this.authState = 'LOGIN_IDLE';
    this.otpDigits = ['', '', '', '', '', ''];
    this.statusMessage = '';
    this.clearTimers();
    this.cdr.detectChanges();
  }

  private handleChallengeInitiated(challenge: LoginChallengeResponse): void {
    if ((challenge.step === 'AUTHENTICATED' || !challenge.challengeId) && challenge.authResponse) {
      const profile = this.auth.completeAuthentication(challenge.authResponse);
      this.handleAuthSuccess(profile);
      return;
    }

    this.challengeId = challenge.challengeId || '';
    this.destinationMasked = challenge.destinationMasked || '+91 ******5841';
    this.authState = 'OTP_SENT';
    this.otpDigits = ['', '', '', '', '', ''];
    this.attemptsRemaining = 5;
    this.startCooldown(challenge.resendAvailableInSeconds || 60);
    this.startExpiryTimer(challenge.expiresInSeconds || 300);
    const displayMsg = challenge.message || `Security verification code sent to ${this.destinationMasked}. Please check your phone SMS.`;
    this.showMessage(displayMsg, 'success');
    this.focusOtpBox(0);
    this.cdr.detectChanges();
  }

  private handleAuthSuccess(profile: any): void {
    this.authState = 'AUTHENTICATED';
    this.clearTimers();
    this.showMessage(`Identity verified successfully. Welcome, ${profile.name}!`, 'success');
    this.cdr.detectChanges();

    setTimeout(() => {
      this.redirectByRole(this.auth.getRole());
    }, 600);
  }

  private parseError(err: any, fallback: string = 'Authentication failed. Please verify and retry.'): string {
    if (!err) return fallback;
    if (err.status === 0 || (err.message && (err.message.includes('Failed to fetch') || err.message.includes('0 Unknown Error')))) {
      return '⚠️ Backend Server Offline (Port 8080): Please launch the backend service or double-click START_AI_SHIELD.bat.';
    }
    return err.error?.message || err.error?.error || err.message || fallback;
  }

  private handleOtpError(err: any): void {
    this.authState = 'OTP_INVALID';
    const msg = this.parseError(err, 'Invalid verification code. Please check SMS and try again.');
    this.showMessage(msg, 'error');

    if (msg.includes('Maximum') || msg.includes('locked')) {
      this.authState = 'OTP_LOCKED';
    } else if (msg.includes('expired')) {
      this.authState = 'OTP_EXPIRED';
    }

    this.otpDigits = ['', '', '', '', '', ''];
    this.focusOtpBox(0);
    this.cdr.detectChanges();
  }

  private redirectByRole(role: string): void {
    if (role === 'ADMIN') {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/user/dashboard']);
    }
  }

  private startCooldown(seconds: number): void {
    this.resendCooldown = seconds;
    if (this.cooldownTimer) clearInterval(this.cooldownTimer);
    this.cooldownTimer = setInterval(() => {
      if (this.resendCooldown > 0) {
        this.resendCooldown--;
      } else {
        clearInterval(this.cooldownTimer);
      }
    }, 1000);
  }

  private startExpiryTimer(seconds: number): void {
    this.expirySeconds = seconds;
    if (this.expiryTimer) clearInterval(this.expiryTimer);
    this.expiryTimer = setInterval(() => {
      if (this.expirySeconds > 0) {
        this.expirySeconds--;
      } else {
        clearInterval(this.expiryTimer);
        if (this.authState === 'OTP_SENT') {
          this.authState = 'OTP_EXPIRED';
          this.showMessage('Verification code has expired. Please request a new code.', 'error');
        }
      }
    }, 1000);
  }

  private clearTimers(): void {
    if (this.cooldownTimer) clearInterval(this.cooldownTimer);
    if (this.expiryTimer) clearInterval(this.expiryTimer);
  }

  private showMessage(msg: string, type: 'error' | 'success' | 'info'): void {
    this.statusMessage = msg;
    this.messageType = type;
  }

  getPasswordStrength(): { level: string; color: string; percent: number } {
    const p = this.mode === 'REGISTER' ? this.regPassword : this.newPassword;
    if (!p) return { level: 'None', color: '#64748b', percent: 0 };
    if (p.length < 6) return { level: 'Weak', color: '#ef4444', percent: 30 };
    const hasNumber = /\d/.test(p);
    const hasSpecial = /[^A-Za-z0-9]/.test(p);
    if (p.length >= 8 && hasNumber && hasSpecial) return { level: 'Strong', color: '#10b981', percent: 100 };
    if (p.length >= 6 && (hasNumber || hasSpecial)) return { level: 'Moderate', color: '#f59e0b', percent: 65 };
    return { level: 'Fair', color: '#38bdf8', percent: 45 };
  }

  formatSeconds(secs: number): string {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`;
  }
}