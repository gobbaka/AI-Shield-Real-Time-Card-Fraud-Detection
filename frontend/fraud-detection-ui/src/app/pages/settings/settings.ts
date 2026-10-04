import { Component, OnInit } from '@angular/core';
import { Sidebar } from '../../shared/sidebar/sidebar';
import { Navbar } from '../../shared/navbar/navbar';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Chatbot } from '../../shared/chatbot/chatbot';
import { FraudDataService, FraudSettings } from '../../services/fraud-data.service';

export interface FraudRuleSetting {
  id: string;
  name: string;
  description: string;
  category: string;
  threshold: number;
  weight: number;
  enabled: boolean;
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [Sidebar, Navbar, CommonModule, FormsModule, Chatbot],
  templateUrl: './settings.html',
  styleUrl: './settings.css'
})
export class Settings implements OnInit {
  settings: FraudSettings;
  saved = false;

  rules: FraudRuleSetting[] = [
    { id: 'RULE_HIGH_AMOUNT', name: 'Critical Amount Threshold', description: 'Flags single transactions exceeding ₹1,00,000 or 3.5x customer baseline', category: 'AMOUNT', threshold: 100000, weight: 1.0, enabled: true },
    { id: 'RULE_VELOCITY_SPIKE', name: 'Rapid Transaction Velocity', description: 'Flags >3 consecutive transactions in less than 10 minutes', category: 'VELOCITY', threshold: 3, weight: 1.0, enabled: true },
    { id: 'RULE_IMPOSSIBLE_TRAVEL', name: 'Impossible Travel Anomaly', description: 'Flags transactions across different cities/countries within 60 minutes', category: 'GEO', threshold: 60, weight: 1.2, enabled: true },
    { id: 'RULE_UNKNOWN_DEVICE', name: 'Unrecognized Device Fingerprint', description: 'Flags logins or purchases from unknown hardware footprints or TOR IPs', category: 'DEVICE', threshold: 1, weight: 0.8, enabled: true },
    { id: 'RULE_RISKY_MERCHANT', name: 'High-Risk Merchant Category', description: 'Flags Crypto Exchanges, Wire Forex, or Gambling merchant codes', category: 'MERCHANT', threshold: 1, weight: 1.0, enabled: true }
  ];

  constructor(private readonly data: FraudDataService) {
    this.settings = data.getSettings();
  }

  ngOnInit(): void {
    this.data.fetchFraudRules().subscribe(dbRules => {
      if (dbRules && dbRules.length > 0) {
        this.rules = dbRules;
      }
    });
  }

  save(): void {
    this.data.saveSettings(this.settings);
    this.data.saveFraudRules(this.rules).subscribe();
    this.saved = true;
    setTimeout(() => this.saved = false, 2500);
  }

  reset(): void {
    this.settings = this.data.resetSettings();
    this.rules.forEach(r => {
      r.enabled = true;
      if (r.id === 'RULE_IMPOSSIBLE_TRAVEL') r.weight = 1.2;
      else if (r.id === 'RULE_UNKNOWN_DEVICE') r.weight = 0.8;
      else r.weight = 1.0;
    });
    this.data.saveFraudRules(this.rules).subscribe();
  }
}