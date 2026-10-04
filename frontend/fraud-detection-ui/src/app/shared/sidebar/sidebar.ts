import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar implements OnInit {
  role: 'ADMIN' | 'USER' = 'ADMIN';

  constructor(private auth: AuthService) {
    this.updateRole();
  }

  ngOnInit(): void {
    this.updateRole();
    window.addEventListener('profileUpdated', () => this.updateRole());
    window.addEventListener('storage', () => this.updateRole());
  }

  updateRole(): void {
    this.role = this.auth.getRole();
  }

  get isAdmin(): boolean {
    return this.auth.getRole() === 'ADMIN';
  }

  get isUser(): boolean {
    return this.auth.getRole() === 'USER';
  }

  isSwitching: boolean = false;

  switchRole(): void {
    if (this.isSwitching) return;
    this.isSwitching = true;
    this.auth.logout();
    window.location.href = '/login';
  }
}