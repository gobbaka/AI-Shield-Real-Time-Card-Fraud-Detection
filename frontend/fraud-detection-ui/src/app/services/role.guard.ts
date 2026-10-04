import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {

  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
    // 1. Unauthenticated users are redirected to login
    if (!this.auth.isAuthenticated()) {
      return this.router.parseUrl('/');
    }

    const expectedRoles: string[] = route.data['roles'] || [];
    const currentRole = this.auth.getRole();

    // 2. If no specific roles required, allow access
    if (!expectedRoles || expectedRoles.length === 0) {
      return true;
    }

    // 3. Check role match (case-insensitive)
    const isAuthorized = expectedRoles.some(r => {
      const exp = r.trim().toUpperCase();
      const curr = currentRole.trim().toUpperCase();
      return exp === curr ||
             (exp === 'ADMIN' && (curr.includes('ADMIN') || curr === 'ADMINISTRATOR')) ||
             (exp === 'USER' && (curr.includes('USER') || curr.includes('CUSTOMER')));
    });

    if (isAuthorized) {
      return true;
    }

    // 4. Role mismatch: Redirect user to their designated dashboard
    const isUser = currentRole === 'USER' || (currentRole as string) === 'CUSTOMER';
    if (isUser) {
      return this.router.parseUrl('/user/dashboard');
    } else {
      return this.router.parseUrl('/dashboard');
    }
  }
}
