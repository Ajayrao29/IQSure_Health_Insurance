// Angular component for the apply-policy page — IQsure AI Document Studio
import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import {
  Policy, InsuredMember, User, UserPolicy,
  Reward, DiscountRule, PremiumBreakdown, UserRewardResponse
} from '../../models/models';

interface GeneratedDocument {
  referenceId: string;
  documentTitle: string;
  documentText: string;
  aiSummary: string;
  generatedAt: string;
}

@Component({
  selector: 'app-apply-policy',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe, RouterLink],
  templateUrl: './apply-policy.html',
  styleUrls: ['./apply-policy.scss']
})
export class ApplyPolicyComponent implements OnInit {
  today: string = new Date().toISOString().split('T')[0];
  plans: Policy[] = [];
  filteredPlans: Policy[] = [];
  userPolicies: UserPolicy[] = [];
  loading = true;
  step: 'PLANS' | 'FORM' = 'PLANS';
  formStep: number = 1;
  selectedPlan: Policy | null = null;

  formData = {
    nomineeName: '',
    nomineeRelationship: '',
    healthReport: null as File | null,
    members: [] as InsuredMember[],
    rewardIds: [] as number[],
    declarations: {
      hospitalizedLastYear: false,
      chronicConditions: false,
      smokeOrAlcohol: false,
      surgicalHistory: false
    }
  };

  availableRewards: UserRewardResponse[] = [];
  discountRules: DiscountRule[] = [];
  userProfile: User | null = null;
  preview: PremiumBreakdown | null = null;
  loadingPreview = false;
  successMessage = '';
  errorMessage = '';
  activeCategory = 'ALL';
  submitting = false;

  // AI Document Generation state
  generatingDoc = false;
  generatedDocument: GeneratedDocument | null = null;
  showDocPreview = false;
  docGenerationError = '';

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loadCatalog();
    this.loadUserData();
    this.handleQueryParams();
  }

  private loadCatalog(): void {
    this.api.getActivePolicies().subscribe({
      next: (data) => {
        this.plans = data;
        this.filteredPlans = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load portfolio:', err);
        this.loading = false;
      }
    });
  }

  private loadUserData(): void {
    const userId = this.auth.getUserId()!;
    this.api.getAvailableRewardsForUser(userId).subscribe(r => this.availableRewards = r);
    this.api.getAllDiscountRules().subscribe(rules => this.discountRules = rules.filter(r => r.isActive));
    this.api.getProfile(userId).subscribe(u => this.userProfile = u);
    this.api.getUserPolicies(userId).subscribe(p => this.userPolicies = p);
  }

  private handleQueryParams(): void {
    this.route.queryParams.subscribe(params => {
      const pid = params['policyId'];
      if (!pid) return;
      const rids = params['rewardIds'];
      if (rids) {
        this.formData.rewardIds = Array.isArray(rids) ? rids.map(Number) : [Number(rids)];
      }
      const checkInterval = setInterval(() => {
        if (!this.loading && this.plans.length > 0) {
          const plan = this.plans.find(p => p.policyId == pid);
          if (plan) this.selectPlan(plan);
          clearInterval(checkInterval);
        }
      }, 100);
    });
  }

  filterPlans(category: string): void {
    this.activeCategory = category;
    this.filteredPlans = category === 'ALL'
      ? this.plans
      : this.plans.filter(p => p.policyType === category);
  }

  isPlanDisabled(policyId: number): boolean {
    return this.userPolicies.some(p => p.policyId === policyId &&
      ['PENDING_UNDERWRITING', 'UNDER_EVALUATION', 'QUOTES_SENT', 'ACTIVE'].includes(p.status));
  }

  selectPlan(plan: Policy): void {
    if (this.isPlanDisabled(plan.policyId)) return;
    this.selectedPlan = plan;
    this.step = 'FORM';
    this.formStep = 1;
    this.generatedDocument = null;
    this.showDocPreview = false;
    const user = this.auth.getUser();
    this.formData.members = [{
      fullName: user?.name || '',
      relationship: 'Self',
      dateOfBirth: '',
      gender: '',
      preExistingConditions: ''
    }];
    window.scrollTo(0, 0);
  }

  backToPlans(): void {
    this.step = 'PLANS';
    this.selectedPlan = null;
    this.generatedDocument = null;
    window.scrollTo(0, 0);
  }

  addMember(): void {
    this.formData.members.push({
      fullName: '',
      relationship: '',
      dateOfBirth: '',
      gender: '',
      preExistingConditions: ''
    });
  }

  removeMember(index: number): void {
    this.formData.members.splice(index, 1);
  }

  onFileSelected(event: any): void {
    if (event.target.files.length > 0) {
      this.formData.healthReport = event.target.files[0];
    }
  }

  nextFormStep(): void {
    if (this.validateCurrentStep()) {
      this.formStep++;
      window.scrollTo(0, 0);
      if (this.formStep === 3) {
        // Auto-generate AI health declaration when reaching declarations step
        this.generateAIDocument();
      }
      if (this.formStep === 5) this.calculatePreview();
    }
  }

  prevFormStep(): void {
    if (this.formStep > 1) {
      this.formStep--;
      window.scrollTo(0, 0);
    }
  }

  private validateCurrentStep(): boolean {
    this.errorMessage = '';
    if (this.formStep === 1) {
      if (!this.formData.nomineeName || !this.formData.nomineeRelationship) {
        this.errorMessage = 'Please provide nominee name and relationship.';
        return false;
      }
    }
    if (this.formStep === 2) {
      if (this.formData.members.length === 0) {
        this.errorMessage = 'At least one insured member is required.';
        return false;
      }
      for (let m of this.formData.members) {
        if (!m.fullName || !m.dateOfBirth || !m.gender) {
          this.errorMessage = 'Please complete all required member fields (Name, DOB, Gender).';
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Calls the backend AI endpoint to generate a personalized health declaration
   * from the form data entered so far.
   */
  generateAIDocument(): void {
    if (!this.selectedPlan || !this.formData.nomineeName) return;
    this.generatingDoc = true;
    this.generatedDocument = null;
    this.docGenerationError = '';

    const request = {
      applicantName: this.userProfile?.name || this.auth.getUser()?.name || 'Applicant',
      policyTitle: this.selectedPlan.title,
      policyType: this.selectedPlan.policyType,
      coverageAmount: this.selectedPlan.coverageAmount,
      hospitalizedLastYear: this.formData.declarations.hospitalizedLastYear,
      chronicConditions: this.formData.declarations.chronicConditions,
      smokeOrAlcohol: this.formData.declarations.smokeOrAlcohol,
      surgicalHistory: this.formData.declarations.surgicalHistory,
      nomineeName: this.formData.nomineeName,
      nomineeRelationship: this.formData.nomineeRelationship,
      members: this.formData.members.map(m => ({
        fullName: m.fullName,
        relationship: m.relationship,
        dateOfBirth: m.dateOfBirth,
        gender: m.gender,
        preExistingConditions: m.preExistingConditions || 'None'
      }))
    };

    this.api.generateHealthDeclaration(request).subscribe({
      next: (doc: GeneratedDocument) => {
        this.generatedDocument = doc;
        this.generatingDoc = false;
        this.showDocPreview = true;
      },
      error: () => {
        this.generatingDoc = false;
        this.docGenerationError = 'Document generation unavailable. You may proceed and upload a file instead.';
      }
    });
  }

  regenerateDocument(): void {
    this.generateAIDocument();
  }

  toggleDocPreview(): void {
    this.showDocPreview = !this.showDocPreview;
  }

  toggleReward(rewardId: number): void {
    const idx = this.formData.rewardIds.indexOf(rewardId);
    if (idx >= 0) {
      this.formData.rewardIds = [];
    } else {
      this.formData.rewardIds = [rewardId];
    }
    this.calculatePreview();
  }

  calculatePreview(): void {
    if (!this.selectedPlan) return;
    this.loadingPreview = true;
    this.api.calculatePremium(this.auth.getUserId()!, this.selectedPlan.policyId, this.formData.rewardIds).subscribe({
      next: (data) => {
        this.preview = data;
        this.loadingPreview = false;
      },
      error: () => this.loadingPreview = false
    });
  }

  getRuleStatus(rule: DiscountRule): boolean {
    if (!this.userProfile || !this.preview) return false;
    const meetsPoints = !rule.minUserPoints || this.userProfile.userPoints >= rule.minUserPoints;
    const meetsScore = !rule.minQuizScorePercent || this.preview.bestQuizScorePercent >= rule.minQuizScorePercent;
    const meetsBadges = !rule.minBadgesEarned || this.preview.badgesEarned >= rule.minBadgesEarned;
    return meetsPoints && meetsScore && meetsBadges;
  }

  submitRequest(): void {
    if (!this.selectedPlan) return;
    const userId = this.auth.getUserId()!;
    this.successMessage = '';
    this.errorMessage = '';
    this.submitting = true;

    // If user uploaded a file, use that. Otherwise use generated doc reference.
    if (this.formData.healthReport) {
      this.api.uploadFile(this.formData.healthReport).subscribe({
        next: (resp) => this.executePurchase(userId, resp.filePath),
        error: (err) => {
          this.submitting = false;
          this.errorMessage = 'File upload failed: ' + (err.error?.message || 'Check connection');
        }
      });
    } else if (this.generatedDocument) {
      // Save AI doc as an HTML file to display beautifully for the underwriter
      const htmlContent = `
        <!DOCTYPE html>
        <html>
        <head>
          <title>${this.generatedDocument.documentTitle}</title>
          <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px; color: #1e293b; background: #f8fafc; line-height: 1.6; }
            .doc-container { max-width: 800px; margin: 0 auto; background: white; padding: 50px; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.05); }
            pre { white-space: pre-wrap; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; font-size: 15px; }
            h2 { color: #8B003F; font-weight: 800; border-bottom: 2px solid #8B003F; padding-bottom: 10px; }
          </style>
        </head>
        <body>
          <div class="doc-container">
            <h2>${this.generatedDocument.documentTitle}</h2>
            <pre>${this.generatedDocument.documentText}</pre>
          </div>
        </body>
        </html>
      `;
      const blob = new Blob([htmlContent], { type: 'text/html' });
      const file = new File([blob], `${this.generatedDocument.referenceId}.html`, { type: 'text/html' });
      
      this.api.uploadFile(file).subscribe({
        next: (resp) => this.executePurchase(userId, resp.filePath),
        error: (err) => {
          this.submitting = false;
          this.errorMessage = 'AI Document upload failed: ' + (err.error?.message || 'Check connection');
        }
      });
    } else {
      this.executePurchase(userId, '');
    }
  }

  private executePurchase(userId: number, path: string): void {
    const request = {
      policyId: this.selectedPlan!.policyId,
      nomineeName: this.formData.nomineeName,
      nomineeRelationship: this.formData.nomineeRelationship,
      healthReportPath: path,
      insuredMembers: this.formData.members,
      rewardIds: this.formData.rewardIds
    };
    this.api.purchasePolicy(userId, request, this.formData.rewardIds).subscribe({
      next: () => {
        this.submitting = false;
        this.successMessage = 'Application submitted! Reference: ' + (this.generatedDocument?.referenceId || 'N/A') + '. Underwriter review initiated.';
        setTimeout(() => this.router.navigate(['/dashboard']), 3500);
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = 'Submission error: ' + (err.error?.message || 'Unknown error');
      }
    });
  }

  getStepLabel(s: number): string {
    const labels: Record<number, string> = {
      1: 'Nominee Details',
      2: 'Insured Members',
      3: 'Health Declaration',
      4: 'Savings & Rewards',
      5: 'Final Review'
    };
    return labels[s] || '';
  }

  getNextLabel(): string {
    const next: Record<number, string> = {
      1: 'Add Members',
      2: 'Generate Declaration',
      3: 'Apply Savings',
      4: 'Review & Submit'
    };
    return next[this.formStep] || '';
  }
}