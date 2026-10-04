import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Sidebar } from '../../../shared/sidebar/sidebar';
import { Navbar } from '../../../shared/navbar/navbar';
import { Chatbot } from '../../../shared/chatbot/chatbot';
import { AuthService } from '../../../services/auth.service';
import { FraudDataService, UserCard, TravelNotice } from '../../../services/fraud-data.service';

@Component({
  selector: 'app-user-cards',
  standalone: true,
  imports: [CommonModule, FormsModule, Sidebar, Navbar, Chatbot],
  templateUrl: './user-cards.html',
  styleUrl: './user-cards.css'
})
export class UserCards implements OnInit {
  userName = 'Priya Singh';
  userEmail = 'priya@example.com';
  cards: UserCard[] = [];
  selectedCard: UserCard | null = null;
  statusMessage = '';
  isLoading = false;
  errorMessage = '';

  // Travel Mode state
  travelNotices: TravelNotice[] = [];
  showTravelForm = false;
  newTravelCountry = 'United Arab Emirates';
  newTravelCity = 'Dubai';
  newTravelStart = new Date().toISOString().split('T')[0];
  newTravelEnd = new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0];

  constructor(
    private auth: AuthService,
    private fraudData: FraudDataService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const user = this.auth.getUser();
    if (user) {
      if (user.name) this.userName = user.name;
      if (user.email) this.userEmail = user.email;
    }
    this.loadCards();
    this.loadTravelNotices();
  }

  loadCards(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.fraudData.fetchUserCards(this.userEmail, this.userName).subscribe({
      next: (cards) => {
        this.cards = cards;
        if (!this.selectedCard || !this.cards.some(c => c.id === this.selectedCard?.id)) {
          this.selectedCard = this.cards[0] || null;
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Unable to load protected cards from banking server.';
        this.cdr.detectChanges();
      }
    });
  }

  loadTravelNotices(): void {
    this.fraudData.fetchTravelNotices(this.userEmail).subscribe(notices => {
      this.travelNotices = notices;
      this.cdr.detectChanges();
    });
  }

  toggleLock(card: UserCard): void {
    this.fraudData.toggleCardLock(card.id).subscribe(res => {
      this.statusMessage = card.status === 'BLOCKED'
        ? `🔒 Card ending in ${card.cardNumberMasked.slice(-4)} has been temporarily frozen.`
        : `✅ Card ending in ${card.cardNumberMasked.slice(-4)} has been unlocked & activated.`;
      this.cdr.detectChanges();
      setTimeout(() => {
        this.statusMessage = '';
        this.cdr.detectChanges();
      }, 3000);
    });
  }

  selectCard(card: UserCard): void {
    this.selectedCard = card;
    this.cdr.detectChanges();
  }

  saveLimit(): void {
    if (!this.selectedCard) return;
    this.fraudData.updateCardLimit(this.selectedCard.id, this.selectedCard.dailyLimit).subscribe(() => {
      this.statusMessage = `✅ Daily limit for card ending ${this.selectedCard?.cardNumberMasked.slice(-4)} updated to ₹${this.selectedCard?.dailyLimit.toLocaleString('en-IN')}.`;
      this.cdr.detectChanges();
      setTimeout(() => {
        this.statusMessage = '';
        this.cdr.detectChanges();
      }, 2500);
    });
  }

  toggleTravelForm(): void {
    this.showTravelForm = !this.showTravelForm;
  }

  addTravelNotice(): void {
    if (!this.newTravelCity || !this.newTravelStart || !this.newTravelEnd) return;

    this.fraudData.createTravelNotice(this.userEmail, {
      cardMasked: this.selectedCard ? this.selectedCard.cardNumberMasked : '**** **** **** 8821',
      destinationCountry: this.newTravelCountry,
      destinationCity: this.newTravelCity,
      startDate: this.newTravelStart,
      endDate: this.newTravelEnd
    }).subscribe(notice => {
      this.statusMessage = `✈️ Travel Mode activated for ${notice.destinationCity}! Geo-velocity false declines are now suppressed.`;
      this.showTravelForm = false;
      this.loadTravelNotices();
      setTimeout(() => this.statusMessage = '', 4000);
    });
  }

  cancelNotice(id: number): void {
    this.fraudData.cancelTravelNotice(id).subscribe(() => {
      this.statusMessage = `✈️ Travel notice cancelled. Standard geo-rules restored.`;
      this.loadTravelNotices();
      setTimeout(() => this.statusMessage = '', 3000);
    });
  }
}
