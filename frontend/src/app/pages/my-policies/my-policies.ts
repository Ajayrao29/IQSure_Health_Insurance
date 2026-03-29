// Angular component for the my-policies page — IQsure Premium Policy Wallet
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { UserPolicy } from '../../models/models';

@Component({
  selector: 'app-my-policies',
  standalone: true,
  imports: [CommonModule, RouterLink, DecimalPipe, DatePipe],
  templateUrl: './my-policies.html',
  styleUrls: ['./my-policies.scss']
})
export class MyPoliciesComponent implements OnInit {
  policies: UserPolicy[] = [];
  loading = true;

  // Modal states
  showPaymentConfirm  = false;
  showProcessing      = false;   // renamed from `processing` for clarity
  showSuccessCard     = false;
  showCertificate     = false;
  showQuoteDetail     = false;   // NEW — full quote detail drawer

  selectedPolicy: UserPolicy | null = null;
  paymentNotice: { message: string; type: 'success' | 'error' } | null = null;

  // Animated payment step (0=idle,1=initiating,2=securing,3=confirming,4=done)
  payStep = 0;
  
  today = new Date();

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit(): void { this.loadPolicies(); }

  loadPolicies(): void {
    this.api.getUserPolicies(this.auth.getUserId()!).subscribe(p => {
      // Sort: QUOTES_SENT first (action needed), then ACTIVE, then rest
      this.policies = p.sort((a, b) => {
        const order = ['QUOTES_SENT', 'ACTIVE', 'UNDER_EVALUATION', 'PENDING_UNDERWRITING', 'EXPIRED', 'REJECTED', 'CANCELLED'];
        return order.indexOf(a.status) - order.indexOf(b.status);
      });
      this.loading = false;
    });
  }

  // ── Stats ─────────────────────────────────────────────────────
  get activePolicies():   UserPolicy[] { return this.policies.filter(p => p.status === 'ACTIVE'); }
  get pendingPolicies():  UserPolicy[] { return this.policies.filter(p => ['PENDING_UNDERWRITING', 'UNDER_EVALUATION'].includes(p.status)); }
  get actionNeeded():     UserPolicy[] { return this.policies.filter(p => p.status === 'QUOTES_SENT'); }
  get totalCoverage():    number       { return this.activePolicies.reduce((s, p) => s + (p.coverageAmount || 0), 0); }
  get totalSaved():       number       { return this.policies.reduce((s, p) => s + (p.savedAmount || 0), 0); }

  // ── Policy card helpers ────────────────────────────────────────
  getPolicyIcon(type: string): string {
    return type === 'HEALTH' ? '❤️' : type === 'LIFE' ? '🌿' : type === 'ACCIDENT' ? '⚡' : '🛡️';
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE:               'active',
      PENDING_UNDERWRITING: 'pending',
      UNDER_EVALUATION:     'evaluating',
      QUOTES_SENT:          'quote-sent',
      EXPIRED:              'expired',
      REJECTED:             'rejected',
      CANCELLED:            'cancelled'
    };
    return map[status] || 'cancelled';
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      ACTIVE:               '✅ Active',
      PENDING_UNDERWRITING: '⏳ Awaiting Assignment',
      UNDER_EVALUATION:     '🔍 Under Evaluation',
      QUOTES_SENT:          '💬 Quote Ready — Action Needed',
      EXPIRED:              '⌛ Expired',
      REJECTED:             '❌ Rejected',
      CANCELLED:            '🚫 Cancelled'
    };
    return labels[status] || status;
  }

  getStatusSteps(status: string): { label: string; done: boolean; active: boolean }[] {
    const allSteps = [
      { key: 'applied',    label: 'Applied' },
      { key: 'review',     label: 'Under Review' },
      { key: 'quoted',     label: 'Quote Received' },
      { key: 'active',     label: 'Active' }
    ];

    const doneMap: Record<string, number> = {
      PENDING_UNDERWRITING: 0,
      UNDER_EVALUATION:     1,
      QUOTES_SENT:          2,
      ACTIVE:               3,
      EXPIRED:              3,
      REJECTED:             1,
      CANCELLED:            0
    };
    const doneUpto = doneMap[status] ?? 0;

    return allSteps.map((step, i) => ({
      label:  step.label,
      done:   i < doneUpto,
      active: i === doneUpto && !['ACTIVE', 'EXPIRED', 'REJECTED', 'CANCELLED'].includes(status)
    }));
  }

  getPolicyNumber(id: number): string {
    return 'IQ-' + (1000 + id) + '-' + (100000 + id * 7).toString().substring(0, 6);
  }

  // ── Quote detail ───────────────────────────────────────────────
  openQuoteDetail(policy: UserPolicy): void {
    this.selectedPolicy = policy;
    this.showQuoteDetail = true;
  }

  // ── Payment flow ───────────────────────────────────────────────
  payNow(policy: UserPolicy): void {
    this.selectedPolicy = policy;
    this.showPaymentConfirm = true;
  }

  confirmPayment(): void {
    if (!this.selectedPolicy) return;
    this.showPaymentConfirm = false;
    this.showProcessing = true;
    this.payStep = 1;

    // Simulate multi-step payment processing animation
    setTimeout(() => this.payStep = 2, 900);
    setTimeout(() => this.payStep = 3, 1800);

    this.api.payPolicy(this.auth.getUserId()!, this.selectedPolicy.id).subscribe({
      next: () => {
        this.payStep = 4;
        setTimeout(() => {
          this.showProcessing = false;
          this.showSuccessCard = true;
          this.playSuccessSound();
          this.loadPolicies();
        }, 700);
      },
      error: (err: any) => {
        this.showProcessing = false;
        this.payStep = 0;
        this.showNotification('Payment failed: ' + (err.error?.message || 'Transaction declined'), 'error');
      }
    });
  }

  playSuccessSound(): void {
    try {
      const ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const play = (freq: number, start: number, dur: number) => {
        const osc  = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, ctx.currentTime + start);
        gain.gain.setValueAtTime(0.08, ctx.currentTime + start);
        gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + start + dur);
        osc.connect(gain); gain.connect(ctx.destination);
        osc.start(ctx.currentTime + start); osc.stop(ctx.currentTime + start + dur);
      };
      play(523.25, 0,    0.35);
      play(659.25, 0.18, 0.4);
      play(783.99, 0.36, 0.6);
    } catch { /* silent fallback */ }
  }

  viewCertificate(policy: UserPolicy): void {
    this.selectedPolicy = policy;
    this.showCertificate = true;
  }

  closeAll(): void {
    this.showPaymentConfirm  = false;
    this.showSuccessCard     = false;
    this.showCertificate     = false;
    this.showQuoteDetail     = false;
    this.showProcessing      = false;
    this.payStep = 0;
  }

  showNotification(message: string, type: 'success' | 'error'): void {
    this.paymentNotice = { message, type };
    setTimeout(() => this.paymentNotice = null, 5000);
  }

  getPayStepLabel(): string {
    const labels = ['', 'Initiating secure connection…', 'Encrypting payment data…', 'Confirming with server…', 'Payment confirmed!'];
    return labels[this.payStep] || '';
  }
}