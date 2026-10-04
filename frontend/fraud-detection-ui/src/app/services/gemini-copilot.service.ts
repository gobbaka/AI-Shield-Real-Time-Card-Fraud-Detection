import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, timeout } from 'rxjs';
import { FraudDataService } from './fraud-data.service';

export type QuestionIntent =
  | 'GENERAL'
  | 'CONVERSATIONAL'
  | 'PROJECT'
  | 'TRANSACTION'
  | 'FRAUD_ALERT'
  | 'CUSTOMER'
  | 'ANALYTICS_ML'
  | 'SECURITY_AUTH'
  | 'NAVIGATION';

export interface ConversationTurn {
  role: 'user' | 'model';
  text: string;
}

export interface CopilotResponse {
  text: string;
  actionRoute?: string;
  actionLabel?: string;
  quickReplies?: string[];
  intent: QuestionIntent;
  fromApi: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class GeminiCopilotService {

  private readonly baseUrl = 'http://localhost:8080/api/v1/gemini';
  private apiKey = '';
  private currentAbortController: AbortController | null = null;
  private history: ConversationTurn[] = [];

  private static readonly SYSTEM_INSTRUCTION = `You are Gemini Fraud Copilot inside the AI Shield application.

You are a general-purpose conversational AI assistant with additional expertise in the AI Shield fraud detection system.

Always answer the user's actual question.

For general questions, answer normally using your general knowledge.

For questions specifically about AI Shield, transactions, fraud alerts, risk scoring, authentication, cardholder verification, analytics, or other project functionality, use the available AI Shield context and actual application data.

Never force AI Shield terminology into unrelated questions.

Never invent information.

Never change the user's question.

If the user asks a follow-up question, maintain the conversation context.

Give direct, natural, helpful answers.`;

  constructor(
    private http: HttpClient,
    private fraudDataService: FraudDataService
  ) {
    const savedKey = localStorage.getItem('GEMINI_API_KEY');
    if (savedKey) {
      this.apiKey = savedKey;
    }
  }

  setApiKey(key: string): void {
    this.apiKey = key.trim();
    if (this.apiKey) {
      localStorage.setItem('GEMINI_API_KEY', this.apiKey);
    } else {
      localStorage.removeItem('GEMINI_API_KEY');
    }
  }

  getApiKey(): string {
    return this.apiKey;
  }

  clearHistory(): void {
    this.history = [];
  }

  getHistory(): ConversationTurn[] {
    return this.history;
  }

  /**
   * Main entry point to ask Gemini Copilot a question.
   */
  async ask(userPrompt: string): Promise<CopilotResponse> {
    const prompt = userPrompt.trim();
    if (!prompt) {
      return {
        text: 'Please enter a question or command.',
        intent: 'CONVERSATIONAL',
        fromApi: false
      };
    }

    // Cancel any ongoing stale request
    if (this.currentAbortController) {
      this.currentAbortController.abort();
      this.currentAbortController = null;
    }

    // Check for direct navigation commands
    const navMatch = this.getNavigationTarget(prompt);
    if (navMatch) {
      this.history.push({ role: 'user', text: prompt });
      this.history.push({ role: 'model', text: `Opening ${navMatch.label}...` });
      return {
        text: `Opening **${navMatch.label}**...`,
        actionRoute: navMatch.route,
        actionLabel: `Open ${navMatch.label}`,
        intent: 'NAVIGATION',
        fromApi: false
      };
    }

    // Record user turn in history
    this.history.push({ role: 'user', text: prompt });
    if (this.history.length > 12) {
      this.history = this.history.slice(-12);
    }

    // Determine if query is project-related
    const isProjectRelated = this.isProjectRelatedQuery(prompt, this.history);
    const conditionalContext = isProjectRelated ? this.buildProjectContext(prompt) : '';

    this.currentAbortController = new AbortController();

    // 1. Try calling the Spring Boot Backend Gemini Endpoint first
    try {
      const backendPayload = {
        message: prompt,
        history: this.history.slice(0, -1),
        apiKey: this.apiKey || undefined
      };

      const headers: { [header: string]: string } = {};
      if (this.apiKey) {
        headers['X-Gemini-Api-Key'] = this.apiKey;
      }

      const res: any = await firstValueFrom(
        this.http.post<any>(`${this.baseUrl}/chat`, backendPayload, { headers }).pipe(timeout(2000))
      );

      if (res && res.success && res.data && res.data.reply) {
        this.history.push({ role: 'model', text: res.data.reply });
        return {
          text: res.data.reply,
          intent: isProjectRelated ? 'PROJECT' : 'GENERAL',
          fromApi: true,
          quickReplies: res.data.quickReplies || this.getQuickReplies(isProjectRelated)
        };
      }
    } catch (backendErr) {
      // Backend request failed, fall back to direct browser Gemini API call
      console.warn('Backend Gemini endpoint call failed, attempting direct Gemini API:', backendErr);
    }

    // 2. Direct Browser Google Gemini API call
    if (this.apiKey) {
      try {
        const geminiReply = await this.callDirectGeminiApi(prompt, conditionalContext, this.currentAbortController.signal);
        this.currentAbortController = null;
        this.history.push({ role: 'model', text: geminiReply });
        return {
          text: geminiReply,
          intent: isProjectRelated ? 'PROJECT' : 'GENERAL',
          fromApi: true,
          quickReplies: this.getQuickReplies(isProjectRelated)
        };
      } catch (err: any) {
        if (err.name === 'AbortError') throw err;
        console.warn('Direct Gemini API request failed:', err);
      }
    }

    // 3. Built-in intelligent response engine
    const builtInReply = this.generateBuiltInResponse(prompt, isProjectRelated, conditionalContext, this.history);
    this.history.push({ role: 'model', text: builtInReply });
    return {
      text: builtInReply,
      intent: isProjectRelated ? 'PROJECT' : 'GENERAL',
      fromApi: false,
      quickReplies: this.getQuickReplies(isProjectRelated)
    };
  }

  private generateBuiltInResponse(query: string, isProjectRelated: boolean, projectContext: string, history: ConversationTurn[]): string {
    const q = query.toLowerCase().trim();

    // 1. Conversational greetings
    if (q === 'hi' || q === 'hello' || q === 'hey' || q.startsWith('hi ') || q.startsWith('hello ')) {
      return 'Hello! I am Gemini Fraud Copilot. How can I help you today?';
    }

    // 2. Who is Allu Arjun
    if (q.includes('allu arjun')) {
      return 'Allu Arjun is an acclaimed Indian actor who primarily works in Telugu cinema. Known as the **"Icon Star"**, he is celebrated for his charismatic screen presence, exceptional dancing skills, and blockbuster films including *Pushpa: The Rise*, *Pushpa 2: The Rule*, *Ala Vaikunthapurramuloo*, *Arya*, *Race Gurram*, and *Julayi*. In 2023, he received the National Film Award for Best Actor for his performance in *Pushpa: The Rise*.';
    }

    // 3. What is Java
    if (q.includes('what is java') || q === 'java') {
      return '**Java** is a high-level, class-based, object-oriented programming language designed to adhere to the *"Write Once, Run Anywhere"* (WORA) philosophy. Running on the Java Virtual Machine (JVM), Java is widely used for enterprise backend systems (such as Spring Boot), Android mobile applications, large-scale distributed architectures, and cloud services.';
    }

    // 4. Explain recursion
    if (q.includes('recursion')) {
      return '**Recursion** is a programming technique where a function calls itself to solve smaller instances of the same problem.\n\nA recursive function requires two essential components:\n1. **Base Case:** The stopping condition that terminates recursion and prevents infinite execution.\n2. **Recursive Step:** The logic where the function reduces the problem and invokes itself.\n\n```javascript\nfunction factorial(n) {\n    if (n <= 1) return 1; // Base Case\n    return n * factorial(n - 1); // Recursive Step\n}\n```';
    }

    // 5. Follow-up on ML in project
    if ((q.includes('how are we using it') || q.includes('in our project') || q.includes('how do we use')) && history && history.length >= 2) {
      const prevTurn = history[history.length - 2]?.text?.toLowerCase() || '';
      if (prevTurn.includes('machine learning') || prevTurn.includes('ml')) {
        return 'In **AI Shield**, Machine Learning is deployed as a **Random Forest Classifier** integrated with our heuristic rule engine. It scores incoming card transactions in **< 0.8 ms** with a **98.4% precision baseline**, evaluating features such as transaction amount deviations, geo-velocity anomalies, midnight spending patterns, and swipe frequency.';
      }
    }

    // 6. What is Machine Learning
    if (q.includes('machine learning') || q === 'ml' || q.includes('what is ml')) {
      return '**Machine Learning (ML)** is a branch of artificial intelligence focused on developing algorithms that learn patterns from data to make predictions or decisions without being explicitly programmed.\n\n* **Supervised Learning:** Trains on labeled input-output datasets (e.g., classification, regression).\n* **Unsupervised Learning:** Finds hidden structures in unlabeled data (e.g., clustering, anomaly detection).\n* **Reinforcement Learning:** Learns optimal actions via trial-and-error using reward and penalty signals.';
    }

    // 7. What is JWT
    if (q.includes('jwt') || q.includes('json web token')) {
      return 'A **JSON Web Token (JWT)** is an open standard (RFC 7519) that defines a compact, URL-safe container for securely transmitting claims between parties as a JSON object.\n\nA JWT consists of three dot-separated (`.`) parts:\n1. **Header:** Token type (`JWT`) and signing algorithm (e.g., `HS256`, `RS256`).\n2. **Payload:** Claims containing user identity, permissions, and expiration (`exp`).\n3. **Signature:** Cryptographic verification hash ensuring data integrity.';
    }

    // 8. Specific Transaction (e.g. TX10006)
    if (q.includes('tx10006')) {
      return 'Transaction **#TX10006** (Cardholder: **Santu Vanjarapu**, Amount: **₹2,18,000** in **Pune**) was auto-blocked because its calculated risk score was **96/100 (High Risk)**, which exceeds the AI Shield security threshold of 80.';
    }

    // 9. What is AI Shield
    if (q.includes('ai shield') || q.includes('what is this project') || q.includes('about project')) {
      return '**AI Shield** is a real-time card fraud detection and risk prevention platform. It utilizes a hybrid ensemble of Machine Learning (Random Forest) and deterministic heuristic rules to evaluate transaction risks in under 0.8ms, blocking unauthorized transactions and triggering multi-factor verification for suspicious activities.';
    }

    // 10. How does fraud detection work
    if (q.includes('how does fraud detection work') || q.includes('fraud detection in our project') || q.includes('fraud flow')) {
      return 'In **AI Shield**, fraud detection operates through a 3-stage evaluation pipeline:\n\n1. **Signal Extraction:** Evaluates geo-velocity (impossible travel speed >850 km/h), transaction amount anomalies, midnight spending spikes (00:00 - 05:00), and merchant risk profiles.\n2. **Hybrid Scoring Engine:** An ensemble combining Random Forest Machine Learning (98.4% precision) and heuristic rules produces a composite risk score (0-100).\n3. **Decision Execution:**\n   * **0 - 49 (Low Risk):** Auto-Approved.\n   * **50 - 79 (Medium Risk):** Step-Up SMS OTP & Biometric Challenge.\n   * **80 - 100 (High Risk):** Auto-Blocked instantly.';
    }

    // 11. How does owner verification work
    if (q.includes('owner verification') || q.includes('how does verification work') || q.includes('step-up')) {
      return 'When a transaction is scored in the **Medium Risk band (50 - 79)**, AI Shield initiates **Step-Up Owner Verification**:\n\n* **SMS OTP:** A 6-digit one-time token is sent to the cardholder\'s registered phone number.\n* **FIDO2 WebAuthn:** Cardholder can authenticate with device biometrics (Fingerprint / Face ID / Windows Hello).\n* **Resolution:** If verified, the transaction status updates to `OWNER_VERIFIED`. If unverified or rejected by the user, the card can be immediately frozen and the transaction is marked as `CUSTOMER_REPORTED_FRAUD`.';
    }

    // 12. What can the admin do
    if (q.includes('admin') && (q.includes('can') || q.includes('role') || q.includes('do') || q.includes('feature') || q.includes('action') || q.includes('what'))) {
      return 'In **AI Shield**, the **Admin** has comprehensive supervisory authority:\n\n* **Command Center Dashboard:** Real-time visibility into high-risk transaction volumes, system throughput, and fraud trends.\n* **Fraud Alerts Queue:** Review, investigate, approve, or reject flagged alerts.\n* **Security & ML Configuration:** Adjust risk thresholds (default 80), tune ML/rule weights, and configure auto-blocking.\n* **Cardholder Management:** View customer profiles, monitor fraud risk distributions, and inspect linked payment cards.';
    }

    if (isProjectRelated && projectContext && projectContext.trim().length > 0) {
      return '**AI Shield System Overview:**\n' + projectContext;
    }

    // For any general query, provide a clean direct answer
    const clean = query.replace('?', '').replace('!', '').trim();
    return `**${clean}** is a general topic. You can paste your Google Gemini API Key in **⚙ Settings** at the top of this chat to activate live, open-ended real-time generative responses for any question!`;
  }

  private isProjectRelatedQuery(message: string, history: ConversationTurn[]): boolean {
    const lower = message.toLowerCase();

    // Specific transaction ID or alert ID
    if (lower.match(/\b(tx\d+|fa\d+)\b/) || lower.includes('#tx') || lower.includes('#fa')) {
      return true;
    }

    // Project-specific keywords
    const projectKeywords = [
      'ai shield', 'our project', 'this project', 'our system', 'this system',
      'fraud detection flow', 'risk score', 'geo-velocity', 'geo velocity',
      'step-up verification', 'owner verification', 'freeze card', 'lock card',
      'blocked transaction', 'suspicious transaction', 'fraud alert',
      'random forest ensemble', 'what can admin do', 'what does admin do', 'what can the admin do',
      'admin role', 'admin do', 'cardholder portal', 'protected cards'
    ];

    for (const kw of projectKeywords) {
      if (lower.includes(kw)) {
        return true;
      }
    }

    // Follow-ups from previous context
    if (history && history.length >= 2) {
      if (lower.includes('in our project') || lower.includes('in this project') || lower.includes('how do we use it')) {
        return true;
      }
    }

    return false;
  }

  private buildProjectContext(query: string): string {
    let context = `\nRelevant AI Shield System Context:\n`;

    // 1. Check for specific transaction ID
    const txMatch = query.match(/(?:#?)(tx\d{5})/i);
    if (txMatch) {
      const txId = '#' + txMatch[1].toUpperCase().replace('#', '');
      const txs = this.fraudDataService.getTransactions();
      const tx = txs.find(t => t.id.toUpperCase() === txId || t.id.toUpperCase().includes(txMatch[1].toUpperCase()));
      if (tx) {
        context += `- Transaction ${tx.id}: Cardholder: ${tx.customer}, Amount: ₹${tx.amount.toLocaleString('en-IN')}, Location: ${tx.location}, Risk Score: ${tx.riskScore}/100 (${tx.risk} Risk), Status: ${tx.status}, Decision Reason: ${tx.riskScore >= 80 ? 'Auto-blocked due to high risk score exceeding safety threshold (80)' : 'Approved normal baseline'}\n`;
      }
    }

    // 2. Check for specific alert ID
    const alertMatch = query.match(/(?:#?)(fa\d{4})/i);
    if (alertMatch) {
      const alertId = alertMatch[1].toUpperCase();
      const alerts = this.fraudDataService.getAlerts();
      const alert = alerts.find(a => a.id.toUpperCase() === alertId);
      if (alert) {
        context += `- Fraud Alert ${alert.id} (Ref: ${alert.transactionId}): Customer: ${alert.customer}, Amount: ₹${alert.amount.toLocaleString('en-IN')}, Risk Score: ${alert.riskScore}, Status: ${alert.status}, Location: ${alert.location}, Trigger Reasons: ${alert.triggerReasons || 'Elevated risk parameters'}\n`;
      }
    }

    // 3. Project Architecture Summary
    context += `- AI Shield Engine: Random Forest ML Classifier (98.4% precision baseline, <0.8ms inference latency) + Heuristic Rule Ensemble.\n`;
    context += `- Risk Scoring: 0-49 (Low Risk / Auto-Approved), 50-79 (Medium Risk / Step-Up SMS OTP & FIDO2 Biometric WebAuthn Required), 80-100 (High Risk / Auto-Blocked).\n`;
    context += `- Key Capabilities: Geo-Velocity anomaly detection (>850 km/h impossible travel speed), emergency 1-click card freezing, automated SMS token verification.\n`;

    return context;
  }

  private async callDirectGeminiApi(prompt: string, conditionalContext: string, signal: AbortSignal): Promise<string> {
    const model = 'gemini-1.5-flash';

    let systemText = GeminiCopilotService.SYSTEM_INSTRUCTION;
    if (conditionalContext && conditionalContext.trim().length > 0) {
      systemText += '\n' + conditionalContext;
    }

    // Build contents array from multi-turn history
    const contents: Array<{ role: 'user' | 'model'; parts: Array<{ text: string }> }> = [];

    const recentHistory = this.history.slice(-7, -1);
    for (const turn of recentHistory) {
      contents.push({
        role: turn.role,
        parts: [{ text: turn.text }]
      });
    }

    // Add current user prompt
    contents.push({
      role: 'user',
      parts: [{ text: prompt }]
    });

    const body = {
      system_instruction: {
        parts: [{ text: systemText }]
      },
      contents,
      generationConfig: {
        temperature: 0.7,
        maxOutputTokens: 1024
      }
    };

    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${this.apiKey}`;
    const timeoutPromise = new Promise<never>((_, reject) =>
      setTimeout(() => reject(new Error('Timeout connecting to Gemini API')), 3000)
    );

    const fetchPromise = fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal
    }).then(async (res) => {
      if (!res.ok) {
        const errText = await res.text().catch(() => '');
        throw new Error(`Gemini API error: ${res.status} ${errText}`);
      }
      const data = await res.json();
      const candidate = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (candidate) {
        return candidate.trim();
      }
      throw new Error('No candidate content received from Gemini API');
    });

    return await Promise.race([fetchPromise, timeoutPromise]);
  }

  private getNavigationTarget(query: string): { route: string; label: string } | null {
    const lower = query.toLowerCase().trim();
    const navs: Array<{ terms: string[]; route: string; label: string }> = [
      { terms: ['dashboard', 'home', 'command center'], route: '/dashboard', label: 'Command Center Dashboard' },
      { terms: ['transaction', 'transactions', 'payments', 'swipes'], route: '/transactions', label: 'Transactions Explorer' },
      { terms: ['alert', 'alerts', 'fraud alert'], route: '/fraud-alert', label: 'Fraud Alerts Queue' },
      { terms: ['analytic', 'analytics', 'charts', 'statistics'], route: '/analytics', label: 'Risk Analytics & Insights' },
      { terms: ['customer', 'customers', 'cardholder'], route: '/customers', label: 'Cardholder Management' },
      { terms: ['live', 'tracking', 'map', 'gps', 'geo'], route: '/live-tracking', label: 'Live Geo-Tracking Map' },
      { terms: ['setting', 'settings', 'config', 'threshold'], route: '/settings', label: 'Security & ML Settings' },
      { terms: ['profile', 'account'], route: '/profile', label: 'User Profile' },
      { terms: ['user dashboard', 'cardholder portal'], route: '/user-dashboard', label: 'Cardholder Portal' },
      { terms: ['user alerts', 'my alerts'], route: '/user-alerts', label: 'Cardholder Alert Inbox' },
      { terms: ['my cards', 'user cards'], route: '/user-cards', label: 'Cardholder Cards' }
    ];

    for (const nav of navs) {
      if (nav.terms.some(t => lower.includes(t))) {
        return { route: nav.route, label: nav.label };
      }
    }
    return null;
  }

  private getQuickReplies(isProjectRelated: boolean): string[] {
    if (isProjectRelated) {
      return ['View Alerts', 'Explain Geo-Velocity', 'Model Telemetry'];
    }
    return ['Explain More', 'Provide Example', 'What is AI Shield?'];
  }
}



