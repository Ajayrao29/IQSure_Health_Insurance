
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { PremiumCalculationLog, UserPolicy } from '../../models/models';
import { forkJoin, Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-payment-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './payment-history.html',
  styleUrls: ['./payment-history.scss']
})
export class PaymentHistoryComponent implements OnInit, OnDestroy {
  loading = true;
  private destroy$ = new Subject<void>();
  
  paymentHistory: PremiumCalculationLog[] = [];
  myPolicies: UserPolicy[] = [];
  showAllPayments = false;

  constructor(
    private api: ApiService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      this.loadData(userId);
    }
  }

  loadData(userId: number): void {
    this.loading = true;
    forkJoin({
      logs: this.api.getPremiumLogs(userId),
      policies: this.api.getUserPolicies(userId)
    })
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (res) => {
        // Deduplicate logs to avoid redundancy (same policy, same premium, same time)
        // Groups by policy and takes latest if identical or just filters out exact duplicates
        this.paymentHistory = this.deduplicateLogs(res.logs).sort((a, b) => 
          new Date(b.calculatedAt).getTime() - new Date(a.calculatedAt).getTime()
        );
        this.myPolicies = res.policies;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading payment history:', err);
        this.loading = false;
      }
    });
  }

  private deduplicateLogs(logs: PremiumCalculationLog[]): PremiumCalculationLog[] {
    // Keep only the latest calculation for each policyId
    const latestMap = new Map<number, PremiumCalculationLog>();
    
    logs.forEach(log => {
      const existing = latestMap.get(log.policyId);
      if (!existing || new Date(log.calculatedAt).getTime() > new Date(existing.calculatedAt).getTime()) {
        latestMap.set(log.policyId, log);
      }
    });
    
    return Array.from(latestMap.values());
  }

  get visiblePayments(): PremiumCalculationLog[] {
    return this.showAllPayments ? this.paymentHistory : this.paymentHistory.slice(0, 8);
  }

  get totalPremiumPaid(): number {
    return this.myPolicies
      .filter(p => p.status === 'ACTIVE')
      .reduce((sum, p) => sum + (p.finalPremium || 0), 0);
  }

  get totalDiscountSaved(): number {
    return this.paymentHistory.reduce((sum, log) => 
      sum + (log.basePremium - log.finalPremium), 0);
  }

  getPaymentStatusLabel(log: PremiumCalculationLog): string {
    const policy = this.myPolicies.find(p => p.id === log.policyId);
    return policy ? policy.status : 'QUOTE_ONLY';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
