// Angular component for the claims page — IQsure Sentinel Intelligence System
import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, TitleCasePipe, SlicePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../services/api.service';
import { AuthService } from '../../../services/auth.service';
import { Claim } from '../../../models/models';

interface AIInvestigationReport {
  fraudRiskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  fraudRiskScore: number;
  coverageVerdict: 'ELIGIBLE' | 'PARTIAL' | 'INELIGIBLE';
  recommendedPayout: number;
  aiNarrative: string;
  redFlags: string[];
  positiveSignals: string[];
  recommendedAction: 'APPROVE' | 'PARTIAL_APPROVE' | 'INVESTIGATE' | 'REJECT';
  officerGuidance: string;
  confidenceScore: number;
}

@Component({
  selector: 'app-claims-officer-claims',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe, TitleCasePipe, SlicePipe, RouterLink],
  templateUrl: './claims.html',
  styleUrls: ['./claims.scss']
})
export class ClaimsOfficerClaimsComponent implements OnInit {
  claims: Claim[] = [];
  filteredClaims: Claim[] = [];
  loading = true;
  activeFilter = 'ALL';
  selectedClaim: Claim | null = null;
  processingStatus = '';
  remarks = '';
  approvedAmount: number = 0;
  isProcessing = false;
  notification: { message: string, type: 'success' | 'error' } | null = null;
  showSettlementModal = false;
  settlementAmount: number = 0;

  // AI Investigation state
  aiReport: AIInvestigationReport | null = null;
  aiInvestigating = false;
  aiInvestigated = false;

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.loading = true;
    this.api.getAllClaimsAdmin().subscribe({
      next: (c: Claim[]) => {
        this.claims = c;
        this.applyFilter(this.activeFilter);
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  applyFilter(filter: string): void {
    this.activeFilter = filter;
    if (filter === 'ALL') {
      this.filteredClaims = this.claims;
    } else {
      this.filteredClaims = this.claims.filter(c => c.status === filter);
    }
  }

  openProcessModal(claim: Claim): void {
    this.selectedClaim = claim;
    this.processingStatus = claim.status === 'SUBMITTED' ? 'UNDER_REVIEW' : claim.status;
    this.remarks = claim.reviewerRemarks || '';
    this.approvedAmount = claim.approvedAmount || claim.amount || 0;
    this.aiReport = null;
    this.aiInvestigated = false;
    this.aiInvestigating = false;
    // Auto-trigger AI investigation when opening a claim
    this.runAiInvestigation();
  }

  runAiInvestigation(): void {
    if (!this.selectedClaim) return;
    this.aiInvestigating = true;
    this.aiReport = null;
    this.api.aiInvestigateClaim(this.selectedClaim.id!).subscribe({
      next: (report: AIInvestigationReport) => {
        this.aiReport = report;
        this.aiInvestigating = false;
        this.aiInvestigated = true;
        // Auto-fill suggested values from AI
        if (report.recommendedPayout && !this.approvedAmount) {
          this.approvedAmount = report.recommendedPayout;
        }
        if (report.recommendedAction === 'APPROVE') {
          this.processingStatus = 'APPROVED';
        } else if (report.recommendedAction === 'PARTIAL_APPROVE') {
          this.processingStatus = 'PARTIAL_APPROVED';
          this.approvedAmount = report.recommendedPayout;
        } else if (report.recommendedAction === 'REJECT') {
          this.processingStatus = 'REJECTED';
        } else {
          this.processingStatus = 'UNDER_REVIEW';
        }
      },
      error: () => {
        this.aiInvestigating = false;
        this.aiInvestigated = true;
        this.showNotification('AI investigation unavailable. Proceeding with manual review.', 'error');
      }
    });
  }

  applyAiRecommendation(): void {
    if (!this.aiReport) return;
    this.approvedAmount = this.aiReport.recommendedPayout;
    if (this.aiReport.recommendedAction === 'APPROVE') this.processingStatus = 'APPROVED';
    else if (this.aiReport.recommendedAction === 'PARTIAL_APPROVE') this.processingStatus = 'PARTIAL_APPROVED';
    else if (this.aiReport.recommendedAction === 'REJECT') this.processingStatus = 'REJECTED';
    else this.processingStatus = 'UNDER_REVIEW';
    this.remarks = this.aiReport.officerGuidance || '';
    this.showNotification('AI recommendation applied.', 'success');
  }

  submitProcess(): void {
    if (!this.selectedClaim) return;
    this.isProcessing = true;
    this.api.processClaim(this.selectedClaim.id!, this.processingStatus, this.remarks, this.approvedAmount).subscribe({
      next: () => {
        this.showNotification('Claim adjudicated successfully!', 'success');
        this.selectedClaim = null;
        this.aiReport = null;
        this.loadClaims();
        this.isProcessing = false;
      },
      error: () => {
        this.showNotification('Error processing claim. Please check details.', 'error');
        this.isProcessing = false;
      }
    });
  }

  openSettleModal(claim: Claim): void {
    this.selectedClaim = claim;
    this.settlementAmount = claim.approvedAmount || 0;
    this.showSettlementModal = true;
    this.aiReport = null;
    this.aiInvestigated = false;
  }

  confirmSettlement(): void {
    if (!this.selectedClaim) return;
    this.isProcessing = true;
    this.api.settleClaim(this.selectedClaim.id!, this.settlementAmount).subscribe({
      next: () => {
        this.showNotification('Claim settled! Coverage updated.', 'success');
        this.showSettlementModal = false;
        this.selectedClaim = null;
        this.loadClaims();
        this.isProcessing = false;
      },
      error: (err) => {
        this.showNotification('Settlement failed: ' + (err.error?.message || 'Server error'), 'error');
        this.isProcessing = false;
      }
    });
  }

  showNotification(message: string, type: 'success' | 'error'): void {
    this.notification = { message, type };
    setTimeout(() => this.notification = null, 5000);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      case 'SUBMITTED': return 'status-submitted';
      case 'UNDER_REVIEW': return 'status-review';
      case 'SETTLED': return 'status-settled';
      case 'PARTIAL_APPROVED': return 'status-partial';
      default: return '';
    }
  }

  getRiskClass(level: string): string {
    switch (level) {
      case 'LOW': return 'risk-low';
      case 'MEDIUM': return 'risk-medium';
      case 'HIGH': return 'risk-high';
      case 'CRITICAL': return 'risk-critical';
      default: return '';
    }
  }

  getRiskIcon(level: string): string {
    switch (level) {
      case 'LOW': return '🟢';
      case 'MEDIUM': return '🟡';
      case 'HIGH': return '🟠';
      case 'CRITICAL': return '🔴';
      default: return '⚪';
    }
  }

  getVerdictClass(verdict: string): string {
    switch (verdict) {
      case 'ELIGIBLE': return 'verdict-eligible';
      case 'PARTIAL': return 'verdict-partial';
      case 'INELIGIBLE': return 'verdict-ineligible';
      default: return '';
    }
  }
}