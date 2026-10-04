import { Component, ElementRef, ViewChild, AfterViewChecked, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { GeminiCopilotService, CopilotResponse } from '../../services/gemini-copilot.service';
import { NotificationService } from '../../services/notification.service';

export interface ChatMessage {
  id: string;
  sender: 'user' | 'gemini';
  text: string;
  timestamp: string;
  actionRoute?: string;
  actionLabel?: string;
  quickReplies?: string[];
  isError?: boolean;
  canRetry?: boolean;
  originalPrompt?: string;
}

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.html',
  styleUrl: './chatbot.css'
})
export class Chatbot implements OnInit, AfterViewChecked {

  @ViewChild('chatScrollContainer') private scrollContainer?: ElementRef;

  isOpen = false;
  isListening = false;
  isThinking = false;
  userMessage = '';
  apiKey = '';
  showApiKeyInput = false;

  quickSuggestions = [
    '✦ What is the fraud detection flow?',
    '✦ Show high-risk transactions',
    '✦ What is Java?',
    '✦ Explain Geo-Velocity',
    '✦ How to freeze a compromised card?'
  ];

  messages: ChatMessage[] = [
    {
      id: '1',
      sender: 'gemini',
      text: 'Hello Pradeep 👋 I am **Gemini Fraud Copilot** ✦.\n\nAsk me general technical questions or inspect live transactions, fraud alerts, risk scores, and AI engine telemetry.',
      timestamp: this.getCurrentTime(),
      quickReplies: ['Check Alerts', 'Explain Fraud Flow', 'What is ML?']
    }
  ];

  constructor(
    private router: Router,
    private geminiCopilotService: GeminiCopilotService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.apiKey = this.geminiCopilotService.getApiKey();
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  toggleChat(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      setTimeout(() => this.scrollToBottom(), 50);
    }
  }

  saveApiKey(): void {
    this.geminiCopilotService.setApiKey(this.apiKey.trim());
    this.showApiKeyInput = false;
    this.addGeminiMessage(
      this.apiKey.trim()
        ? '✦ Gemini API Key configured successfully! Real-time generative responses are active.'
        : '✦ Gemini API Key removed. Using intelligent local engine.'
    );
  }

  useSuggestion(prompt: string): void {
    const cleaned = prompt.replace(/^✦\s*/, '').trim();
    this.userMessage = cleaned;
    this.sendMessage();
  }

  async sendMessage(): Promise<void> {
    const rawText = this.userMessage.trim();
    if (!rawText || this.isThinking) return;

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: rawText,
      timestamp: this.getCurrentTime()
    };
    this.messages.push(userMsg);
    this.userMessage = '';
    this.isThinking = true;

    try {
      const response: CopilotResponse = await this.geminiCopilotService.ask(rawText);
      this.isThinking = false;

      // Handle navigation route if specified
      if (response.actionRoute && response.intent === 'NAVIGATION') {
        this.router.navigate([response.actionRoute]);
      }

      this.addGeminiMessage(
        response.text,
        response.actionRoute,
        response.actionLabel,
        response.quickReplies
      );
    } catch (err: any) {
      this.isThinking = false;
      this.messages.push({
        id: Date.now().toString(),
        sender: 'gemini',
        text: 'Sorry, I encountered an issue processing your request. Please try again.',
        timestamp: this.getCurrentTime(),
        isError: true,
        canRetry: true,
        originalPrompt: rawText
      });
    }
  }

  retryMessage(originalPrompt?: string): void {
    if (originalPrompt) {
      this.userMessage = originalPrompt;
      this.sendMessage();
    }
  }

  formatMarkdown(text: string): string {
    if (!text) return '';
    let html = text;

    // Convert code blocks ```code```
    html = html.replace(/```([\s\S]*?)```/g, '<pre class="code-block"><code>$1</code></pre>');

    // Convert inline code `code`
    html = html.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');

    // Convert headers ### Header
    html = html.replace(/^### (.*$)/gim, '<div class="md-h3">$1</div>');
    html = html.replace(/^## (.*$)/gim, '<div class="md-h2">$1</div>');

    // Convert bold **text**
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // Convert bullet points * item
    html = html.replace(/^\* (.*$)/gim, '<div class="md-bullet">• $1</div>');

    // Convert new lines
    html = html.replace(/\n/g, '<br>');

    return html;
  }

  private addGeminiMessage(text: string, actionRoute?: string, actionLabel?: string, quickReplies?: string[]): void {
    this.messages.push({
      id: Date.now().toString(),
      sender: 'gemini',
      text,
      timestamp: this.getCurrentTime(),
      actionRoute,
      actionLabel,
      quickReplies
    });
  }

  startVoice(): void {
    const SpeechRecognition =
      (window as any).webkitSpeechRecognition ||
      (window as any).SpeechRecognition;

    if (!SpeechRecognition) {
      this.addGeminiMessage('⚠️ Voice recognition is not supported in this browser. Please type your message.');
      return;
    }

    if (this.isListening) {
      this.isListening = false;
      return;
    }

    // 1. Activate Push Notifications alongside Voice input
    this.notificationService.requestPermission().then(granted => {
      if (granted) {
        this.notificationService.sendPushNotification(
          '🎙️ Gemini Voice Input & Live Alerts Active',
          'Voice input listening for Gemini Copilot. Live alerts activated.'
        );
      }
    });

    // 2. Play Audio Speech Confirmation
    try {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance('Listening for your question.');
        utterance.rate = 1.1;
        window.speechSynthesis.speak(utterance);
      }
    } catch {}

    const recognition = new SpeechRecognition();
    recognition.lang = 'en-US';
    this.isListening = true;

    recognition.onresult = (event: any) => {
      this.isListening = false;
      this.userMessage = event.results[0][0].transcript;
      setTimeout(() => this.sendMessage(), 50);
    };

    recognition.onerror = () => {
      this.isListening = false;
    };

    recognition.onend = () => {
      this.isListening = false;
    };

    recognition.start();
  }

  clearChat(): void {
    this.geminiCopilotService.clearHistory();
    this.messages = [
      {
        id: Date.now().toString(),
        sender: 'gemini',
        text: 'Chat history cleared. How can I help you today ✦?',
        timestamp: this.getCurrentTime(),
        quickReplies: ['Check Alerts', 'Explain Fraud Rules', 'What is Python?']
      }
    ];
  }

  navigateToAction(route: string): void {
    this.router.navigate([route]);
  }

  private getCurrentTime(): string {
    const now = new Date();
    return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  private scrollToBottom(): void {
    try {
      if (this.scrollContainer) {
        this.scrollContainer.nativeElement.scrollTop = this.scrollContainer.nativeElement.scrollHeight;
      }
    } catch {
      // container not yet rendered
    }
  }
}