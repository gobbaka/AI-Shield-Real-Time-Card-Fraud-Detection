import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Sidebar } from '../../shared/sidebar/sidebar';
import { Navbar } from '../../shared/navbar/navbar';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, Sidebar, Navbar, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  name = 'Pradeep';
  email = 'pradeep@example.com';
  role = 'Administrator';
  phone = '+91 98765 43210';
  location = 'Hyderabad, India';
  bio = 'Monitoring fraud activity across all transactions and tuning AI detection rules for the team.';
  loading = false;
  feedbackMessage = '';
  feedbackType: 'error' | 'success' = 'error';

  private readonly profileApiUrl = 'http://localhost:8080/api/v1/auth/profile';

  constructor(private router: Router, private http: HttpClient) {
    try {
      const saved = localStorage.getItem('profile');
      if (saved) Object.assign(this, JSON.parse(saved));
    } catch { }
  }

  ngOnInit(): void {
    this.http.get<any>(this.profileApiUrl).subscribe({
      next: (res) => {
        if (res && res.data) {
          if (res.data.name) this.name = res.data.name;
          if (res.data.email) this.email = res.data.email;
          if (res.data.role) this.role = res.data.role;
          if (res.data.phone) this.phone = res.data.phone;
          if (res.data.location) this.location = res.data.location;
          if (res.data.bio) this.bio = res.data.bio;
        }
      },
      error: () => {
        // Fallback to local profile cache
      }
    });
  }

  saveProfile() {
    if (!this.name || !this.email) {
      this.feedbackType = 'error';
      this.feedbackMessage = 'Please enter your name and email.';
      return;
    }

    this.loading = true;

    const payload = {
      name: this.name,
      email: this.email,
      role: this.role,
      phone: this.phone,
      location: this.location,
      bio: this.bio
    };

    // 1. Update local storage
    try {
      localStorage.setItem('profile', JSON.stringify(payload));
      window.dispatchEvent(new Event('profileUpdated'));
    } catch (e) {
      console.warn('Unable to save profile to localStorage', e);
    }

    // 2. Persist to Spring Boot backend
    this.http.put<any>(this.profileApiUrl, payload).subscribe({
      next: (res) => {
        this.loading = false;
        this.feedbackType = 'success';
        this.feedbackMessage = 'Profile updated and synced successfully.';
      },
      error: () => {
        this.loading = false;
        this.feedbackType = 'success';
        this.feedbackMessage = 'Profile updated locally.';
      }
    });
  }
}
