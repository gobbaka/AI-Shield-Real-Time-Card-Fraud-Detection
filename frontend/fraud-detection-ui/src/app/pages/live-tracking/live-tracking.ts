import { Component, OnDestroy, OnInit } from '@angular/core';
import { Sidebar } from '../../shared/sidebar/sidebar';
import { Navbar } from '../../shared/navbar/navbar';
import { CommonModule } from '@angular/common';
import { Chatbot } from '../../shared/chatbot/chatbot';
import { FraudDataService, Transaction } from '../../services/fraud-data.service';

@Component({
  selector: 'app-live-tracking',
  standalone: true,
  imports: [Sidebar, Navbar, CommonModule, Chatbot],
  templateUrl: './live-tracking.html',
  styleUrl: './live-tracking.css'
})
export class LiveTracking implements OnInit, OnDestroy {
  selected!: Transaction;
  tracking = true;
  lastUpdated = new Date();
  private timer: any = null;

  constructor(private readonly data: FraudDataService) {
    const list = this.data.getTransactions();
    this.selected = list[1] || list[0];
  }

  ngOnInit(): void {
    this.startLiveSimulation();
  }

  ngOnDestroy(): void {
    this.stopLiveSimulation();
  }

  get transactions(): Transaction[] {
    return this.data.getTransactions();
  }

  select(transaction: Transaction): void {
    this.selected = transaction;
  }

  toggleTracking(): void {
    this.tracking = !this.tracking;
    this.lastUpdated = new Date();
    if (this.tracking) {
      this.startLiveSimulation();
    } else {
      this.stopLiveSimulation();
    }
  }

  blockSelected(): void {
    if (this.selected) {
      this.data.blockTransaction(this.selected.id);
    }
  }

  private startLiveSimulation(): void {
    this.stopLiveSimulation();
    this.timer = setInterval(() => {
      if (this.tracking) {
        this.lastUpdated = new Date();
      }
    }, 4000);
  }

  private stopLiveSimulation(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }
}