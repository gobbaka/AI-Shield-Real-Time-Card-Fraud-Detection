import { Component } from '@angular/core';
import { Sidebar } from '../../shared/sidebar/sidebar';
import { Navbar } from '../../shared/navbar/navbar';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chatbot } from '../../shared/chatbot/chatbot';
import { Customer, FraudDataService } from '../../services/fraud-data.service';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [Sidebar, Navbar, CommonModule, FormsModule, Chatbot],
  templateUrl: './customers.html',
  styleUrl: './customers.css'
})
export class Customers {
  search = '';
  risk = 'All';
  sortDirection: 'asc' | 'desc' = 'asc';
  selected: Customer | null = null;
  page = 1;
  readonly pageSize = 6;
  actionMessage = '';

  constructor(private readonly data: FraudDataService) {}

  get filteredCustomers(): Customer[] {
    const query = this.search.toLowerCase().trim();
    return this.data.getCustomers()
      .filter(item => (!query || `${item.name} ${item.email} ${item.card}`.toLowerCase().includes(query)) && (this.risk === 'All' || item.status === this.risk))
      .sort((a, b) => this.sortDirection === 'asc' ? a.name.localeCompare(b.name) : b.name.localeCompare(a.name));
  }

  get pagedCustomers(): Customer[] {
    return this.filteredCustomers.slice((this.page - 1) * this.pageSize, this.page * this.pageSize);
  }

  get pageCount(): number {
    return Math.max(1, Math.ceil(this.filteredCustomers.length / this.pageSize));
  }

  changePage(delta: number): void {
    this.page = Math.min(this.pageCount, Math.max(1, this.page + delta));
  }

  applyFilters(): void {
    this.page = 1;
  }

  toggleSort(): void {
    this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
  }

  toggleCardStatus(customer: Customer): void {
    if (customer.status === 'High') {
      customer.status = 'Low';
      customer.fraudScore = 12;
      this.actionMessage = `Card for ${customer.name} unblocked & marked ACTIVE.`;
    } else {
      customer.status = 'High';
      customer.fraudScore = 95;
      this.actionMessage = `Card for ${customer.name} frozen & marked BLOCKED.`;
    }

    const cardDigits = customer.card ? customer.card.replace(/\D/g, '') : '1';
    this.data.toggleCardLock(cardDigits).subscribe();
    setTimeout(() => this.actionMessage = '', 2500);
  }
}