import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, catchError, throwError } from 'rxjs';

export interface UserProfile {
  id?: number;
  name: string;
  email: string;
  role: 'ADMIN' | 'USER';
  phone?: string;
  mobileVerified?: boolean;
  location?: string;
  bio?: string;
  cardMasked?: string;
}

export interface LoginChallengeResponse {
  challengeId?: string;
  step: string;
  destinationMasked?: string;
  resendAvailableInSeconds?: number;
  expiresInSeconds?: number;
  message?: string;
  authResponse?: AuthResponse;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  name: string;
  email: string;
  role: string;
  phone: string;
  mobileVerified: boolean;
}

export interface OtpResponse {
  success: boolean;
  message: string;
  destinationMasked: string;
  resendAvailableInSeconds: number;
  expiresAt: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/v1/auth';
  private readonly tokenKey = 'auth_token';

  private readonly currentUserSubject = new BehaviorSubject<UserProfile | null>(this.getStoredUser());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  /**
   * STEP 1: Validate credentials and obtain 2FA Challenge (No JWT is issued yet).
   * If OTP service is disabled on the backend, completes authentication directly.
   */
  initiateLogin(identifier: string, password: string): Observable<{ success: boolean; data: LoginChallengeResponse }> {
    const payload = { email: identifier.trim(), password };
    return this.http.post<{ success: boolean; data: LoginChallengeResponse }>(`${this.baseUrl}/login`, payload).pipe(
      tap(res => {
        if (res && res.success && res.data && res.data.authResponse) {
          this.completeAuthentication(res.data.authResponse);
        }
      })
    );
  }

  /**
   * STEP 2: Verify 6-digit OTP and obtain authenticated session + JWT token.
   */
  verifyLoginOtp(challengeId: string, otp: string): Observable<{ success: boolean; data: AuthResponse }> {
    const payload = { challengeId, otp: otp.trim() };
    return this.http.post<{ success: boolean; data: AuthResponse }>(`${this.baseUrl}/verify-login-otp`, payload).pipe(
      tap(res => {
        if (res && res.success && res.data) {
          this.completeAuthentication(res.data);
        }
      })
    );
  }

  /**
   * Resend OTP for active login challenge.
   */
  resendLoginOtp(challengeId: string): Observable<{ success: boolean; data: OtpResponse }> {
    return this.http.post<{ success: boolean; data: OtpResponse }>(`${this.baseUrl}/resend-login-otp`, { challengeId });
  }

  /**
   * Cardholder Registration: Creates account and dispatches mobile OTP.
   * If OTP service is disabled on the backend, completes authentication directly.
   */
  registerUser(name: string, email: string, password: string, phone: string): Observable<{ success: boolean; data: LoginChallengeResponse }> {
    const payload = { name: name.trim(), email: email.trim(), password, phone: phone.trim(), location: 'India' };
    return this.http.post<{ success: boolean; data: LoginChallengeResponse }>(`${this.baseUrl}/register`, payload).pipe(
      tap(res => {
        if (res && res.success && res.data && res.data.authResponse) {
          this.completeAuthentication(res.data.authResponse);
        }
      })
    );
  }

  /**
   * Forgot Password: requests reset OTP challenge.
   */
  forgotPassword(emailOrPhone: string): Observable<{ success: boolean; data: LoginChallengeResponse }> {
    return this.http.post<{ success: boolean; data: LoginChallengeResponse }>(`${this.baseUrl}/forgot-password`, { emailOrPhone: emailOrPhone.trim() });
  }

  /**
   * Reset Password: verifies OTP and sets new password.
   */
  resetPassword(challengeId: string, otp: string, newPassword: string): Observable<{ success: boolean; data: AuthResponse }> {
    return this.http.post<{ success: boolean; data: AuthResponse }>(`${this.baseUrl}/reset-password`, { challengeId, otp: otp.trim(), newPassword }).pipe(
      tap(res => {
        if (res && res.success && res.data) {
          this.completeAuthentication(res.data);
        }
      })
    );
  }

  /**
   * Finalize authentication session in storage and broadcast update.
   */
  public completeAuthentication(auth: AuthResponse): UserProfile {
    const rawRole = (auth.role || '').toUpperCase();
    const role: 'ADMIN' | 'USER' = rawRole.includes('ADMIN') ? 'ADMIN' : 'USER';

    const profile: UserProfile = {
      id: auth.userId,
      name: auth.name || 'User',
      email: auth.email,
      role,
      phone: auth.phone,
      mobileVerified: auth.mobileVerified,
      location: 'India',
      bio: role === 'ADMIN' ? 'AI Shield Lead Fraud Operations' : 'Verified Cardholder'
    };

    try {
      localStorage.setItem(this.tokenKey, auth.token);
      localStorage.setItem('profile', JSON.stringify(profile));
      localStorage.setItem('user_role', role);
      this.currentUserSubject.next(profile);
      window.dispatchEvent(new Event('profileUpdated'));
    } catch (e) {
      console.warn('Failed to persist authentication token', e);
    }

    return profile;
  }

  public logout(): void {
    try {
      localStorage.removeItem(this.tokenKey);
      localStorage.removeItem('profile');
      localStorage.removeItem('user_role');
      this.currentUserSubject.next(null);
      window.dispatchEvent(new Event('profileUpdated'));
    } catch (e) {
      console.warn('Failed to clear auth session', e);
    }
  }

  public isAuthenticated(): boolean {
    try {
      const token = localStorage.getItem(this.tokenKey);
      return !!token && token.trim().length > 0 && !token.startsWith('auth-session-');
    } catch {
      return false;
    }
  }

  public getUser(): UserProfile | null {
    return this.currentUserSubject.value || this.getStoredUser();
  }

  public getRole(): 'ADMIN' | 'USER' {
    const user = this.getUser();
    if (user && user.role) {
      const r = (user.role as string).toUpperCase();
      if (r.includes('ADMIN')) return 'ADMIN';
      if (r.includes('USER') || r.includes('CUSTOMER')) return 'USER';
    }

    try {
      const raw = (localStorage.getItem('user_role') || '').toUpperCase();
      if (raw.includes('ADMIN')) return 'ADMIN';
      if (raw.includes('USER') || raw.includes('CUSTOMER')) return 'USER';
    } catch {}

    return 'USER';
  }

  public isAdmin(): boolean {
    return this.isAuthenticated() && this.getRole() === 'ADMIN';
  }

  public isUser(): boolean {
    return this.isAuthenticated() && this.getRole() === 'USER';
  }

  public getToken(): string | null {
    try {
      return localStorage.getItem(this.tokenKey);
    } catch {
      return null;
    }
  }

  private getStoredUser(): UserProfile | null {
    try {
      const raw = localStorage.getItem('profile');
      if (!raw) return null;
      return JSON.parse(raw) as UserProfile;
    } catch {
      return null;
    }
  }
}

