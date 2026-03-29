// Angular component for the my-claims page — IQsure Claims Tracker
import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { Claim } from '../../models/models';

@Component({
  selector: 'app-my-claims',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './my-claims.html',
  styleUrls: ['./my-claims.scss']
})
export class MyClaimsComponent implements OnInit {
  claims: Claim[] = [];
  loading = true;

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getClaimsByUser(userId).subscribe({
        next: (data) => {
          // Sort: most recent first
          this.claims = data.sort((a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
          this.loading = false;
        },
        error: () => this.loading = false
      });
    }
  }

  // ── Summary stats ──────────────────────────────────────────────
  getPendingCount(): number {
    return this.claims.filter(c =>
      c.status === 'SUBMITTED' || c.status === 'UNDER_REVIEW'
    ).length;
  }

  getApprovedCount(): number {
    return this.claims.filter(c =>
      c.status === 'APPROVED' || c.status === 'PARTIAL_APPROVED' || c.status === 'SETTLED'
    ).length;
  }

  getTotalSettled(): number {
    return this.claims
      .filter(c => c.status === 'SETTLED')
      .reduce((sum, c) => sum + (c.settlementAmount ?? 0), 0);
  }

  // ── Status helpers ─────────────────────────────────────────────
  getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED':        return 'status-approved';
      case 'PARTIAL_APPROVED': return 'status-partial';
      case 'REJECTED':        return 'status-rejected';
      case 'SUBMITTED':       return 'status-submitted';
      case 'UNDER_REVIEW':    return 'status-review';
      case 'SETTLED':         return 'status-settled';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      SUBMITTED:        '📤 Submitted',
      UNDER_REVIEW:     '🔍 Under Review',
      APPROVED:         '✅ Approved',
      PARTIAL_APPROVED: '⚡ Partially Approved',
      REJECTED:         '❌ Rejected',
      SETTLED:          '💰 Settled'
    };
    return labels[status] || status;
  }
}