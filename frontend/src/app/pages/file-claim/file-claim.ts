// Angular component for the file-claim page — IQsure AI Claim Studio
import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, SlicePipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { UserPolicy, User } from '../../models/models';

interface GeneratedDocument {
  referenceId: string;
  documentTitle: string;
  documentText: string;
  aiSummary: string;
  generatedAt: string;
}

@Component({
  selector: 'app-file-claim',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe, SlicePipe, DatePipe, RouterLink],
  templateUrl: './file-claim.html',
  styleUrls: ['./file-claim.scss']
})
export class FileClaimComponent implements OnInit {
  policies: UserPolicy[] = [];
  userProfile: User | null = null;
  loading = true;
  today: string = new Date().toISOString().split('T')[0];
  claimStep: number = 1;

  formData = {
    userPolicyId: null as number | null,
    type: 'CASHLESS',
    amount: null as number | null,
    hospitalName: '',
    incidentDate: '',
    diagnosis: '',
    description: '',
    bankAccountNo: '',
    bankIFSC: '',
    declarationChecked: false
  };

  successMessage = '';
  errorMessage = '';
  submitting = false;

  // AI Claim Document state
  generatingDoc = false;
  claimDocument: GeneratedDocument | null = null;
  showDocPreview = false;
  docError = '';

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getUserPolicies(userId).subscribe({
        next: (data) => {
          this.policies = data.filter(p => p.status === 'ACTIVE');
          this.loading = false;
        },
        error: () => this.loading = false
      });
      this.api.getProfile(userId).subscribe(u => this.userProfile = u);
    }
  }

  getSelectedPolicy(): UserPolicy | null {
    return this.policies.find(p => p.id === this.formData.userPolicyId) || null;
  }

  nextStep(): void {
    if (this.validateStep()) {
      this.claimStep++;
      window.scrollTo(0, 0);
      // Auto-generate AI claim letter when reaching step 3
      if (this.claimStep === 3) {
        this.generateClaimDocument();
      }
    }
  }

  prevStep(): void {
    if (this.claimStep > 1) {
      this.claimStep--;
      window.scrollTo(0, 0);
    }
  }

  validateStep(): boolean {
    this.errorMessage = '';
    if (this.claimStep === 1) {
      if (!this.formData.userPolicyId || !this.formData.amount || !this.formData.incidentDate || !this.formData.type) {
        this.errorMessage = 'Please fill all incident details (Policy, Type, Amount, Date).';
        return false;
      }
      if (this.formData.amount != null && this.formData.amount <= 0) {
        this.errorMessage = 'Claim amount must be greater than zero.';
        return false;
      }
    }
    if (this.claimStep === 2) {
      if (!this.formData.diagnosis) {
        this.errorMessage = 'Please provide the primary medical diagnosis.';
        return false;
      }
    }
    return true;
  }

  /**
   * Calls backend AI to generate a formal claim submission letter
   * from the form data collected in steps 1 and 2.
   */
  generateClaimDocument(): void {
    const selectedPolicy = this.getSelectedPolicy();
    this.generatingDoc = true;
    this.claimDocument = null;
    this.docError = '';

    const request = {
      claimantName: this.userProfile?.name || this.auth.getUser()?.name || 'Claimant',
      policyTitle: selectedPolicy?.policyTitle || 'Health Insurance Policy',
      claimType: this.formData.type,
      amount: this.formData.amount,
      hospitalName: this.formData.hospitalName || 'Not specified',
      incidentDate: this.formData.incidentDate,
      diagnosis: this.formData.diagnosis,
      description: this.formData.description || 'See additional documentation.'
    };

    this.api.generateClaimLetter(request).subscribe({
      next: (doc: GeneratedDocument) => {
        this.claimDocument = doc;
        this.generatingDoc = false;
        this.showDocPreview = true;
      },
      error: () => {
        this.generatingDoc = false;
        this.docError = 'AI letter generation unavailable. Your claim will still be submitted successfully.';
      }
    });
  }

  regenerateDocument(): void {
    this.generateClaimDocument();
  }

  toggleDocPreview(): void {
    this.showDocPreview = !this.showDocPreview;
  }

  submitClaim(): void {
    if (!this.formData.declarationChecked) {
      this.errorMessage = 'Please confirm the declaration of truth before submitting.';
      return;
    }
    if (!this.formData.userPolicyId || !this.formData.amount || !this.formData.incidentDate || !this.formData.diagnosis) {
      this.errorMessage = 'Incomplete claim. Please go back and fill all required fields.';
      return;
    }
    const userId = this.auth.getUserId()!;
    this.submitting = true;
    this.errorMessage = '';

    const executeSubmit = (claimDocPath: string | null) => {
      const payload = {
        userPolicyId: this.formData.userPolicyId,
        type: this.formData.type,
        amount: this.formData.amount,
        hospitalName: this.formData.hospitalName,
        incidentDate: this.formData.incidentDate,
        diagnosis: this.formData.diagnosis,
        description: this.formData.description,
        bankAccountNo: this.formData.bankAccountNo,
        bankIFSC: this.formData.bankIFSC,
        // Attach the uploaded AI document path, or fallback to ref if not possible
        claimDocumentRef: claimDocPath
      };

      this.api.fileClaim(userId, this.formData.userPolicyId!, payload).subscribe({
        next: () => {
          this.submitting = false;
          this.successMessage = `Claim filed! Reference: ${this.claimDocument?.referenceId || 'CLM-' + Math.floor(Math.random() * 90000 + 10000)}. Assigned for review.`;
          this.errorMessage = '';
          setTimeout(() => this.router.navigate(['/my-claims']), 3500);
        },
        error: (err) => {
          this.submitting = false;
          this.errorMessage = 'Error filing claim: ' + (err.error?.message || err.message || 'Unknown error');
        }
      });
    };

    if (this.claimDocument) {
      const htmlContent = `
        <!DOCTYPE html>
        <html>
        <head>
          <title>${this.claimDocument.documentTitle}</title>
          <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px; color: #1e293b; background: #f8fafc; line-height: 1.6; }
            .doc-container { max-width: 800px; margin: 0 auto; background: white; padding: 50px; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.05); }
            pre { white-space: pre-wrap; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; font-size: 15px; }
            h2 { color: #8B003F; font-weight: 800; border-bottom: 2px solid #8B003F; padding-bottom: 10px; }
          </style>
        </head>
        <body>
          <div class="doc-container">
            <h2>${this.claimDocument.documentTitle}</h2>
            <pre>${this.claimDocument.documentText}</pre>
          </div>
        </body>
        </html>
      `;
      const blob = new Blob([htmlContent], { type: 'text/html' });
      const file = new File([blob], `${this.claimDocument.referenceId}.html`, { type: 'text/html' });
      
      this.api.uploadFile(file).subscribe({
        next: (resp) => executeSubmit(resp.filePath),
        error: (err) => {
          this.submitting = false;
          this.errorMessage = 'AI Letter upload failed: ' + (err.error?.message || 'Check connection');
        }
      });
    } else {
      executeSubmit(null);
    }
  }

  getClaimTypeLabel(): string {
    const labels: Record<string, string> = {
      CASHLESS: 'Cashless (Integrated Hospital)',
      REIMBURSEMENT: 'Reimbursement (Out-of-pocket)',
      ACCIDENTAL: 'Accidental / Trauma'
    };
    return labels[this.formData.type] || this.formData.type;
  }

  getStepLabel(s: number): string {
    const labels: Record<number, string> = {
      1: 'Incident',
      2: 'Medical',
      3: 'AI Document',
      4: 'Settlement'
    };
    return labels[s] || '';
  }
}