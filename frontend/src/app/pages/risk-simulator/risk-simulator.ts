// Angular component for the risk-simulator page

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
interface SimulationResult {
  policy: any;
  scenarioName: string;
  eventCost: number;
  insuranceCovers: number;
  outOfPocket: number;
  annualPremium: number;
  totalWithInsurance: number;
  savings: number;
  savingsPercent: number;
  deductiblePaid: number;
  copayPaid: number;
  oopMaxReached: boolean;
  peaceOfMindScore: number;
  financialRuinProb: number;
}
@Component({
  selector: 'app-risk-simulator',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './risk-simulator.html',
  styleUrls: ['./risk-simulator.scss']
})
export class RiskSimulatorComponent implements OnInit {
  policies: any[] = [];
  loading = true;
  selectedPolicyId: number | '' = '';
  scenarioName = '';
  eventCost: number | null = null;
  result: SimulationResult | null = null;
  showResult = false;
  animateNumbers = false;
  simulationsRun = 0;
  constructor(private api: ApiService, public auth: AuthService) {}
  ngOnInit(): void {
    this.api.getActivePolicies().subscribe({
      next: (policies) => {
        this.policies = policies;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }
  simulate(): void {
    if (!this.isFormValid) return;
    const policy = this.policies.find(p => p.policyId === Number(this.selectedPolicyId));
    if (!policy) return;
    const cost = this.eventCost!;
    const deductible = policy.deductibleAmount || 0;
    const copayRate = policy.copayPercentage || 0;
    const oopMax = policy.outOfPocketMax || Infinity;
    const coverageLimit = policy.coverageAmount;
    let userPays = Math.min(cost, deductible);
    const deductiblePaid = userPays;
    let remaining = Math.max(0, cost - deductible);
    const copayPaid = remaining * copayRate;
    userPays += copayPaid;
    let oopMaxReached = false;
    if (userPays > oopMax) {
      userPays = oopMax;
      oopMaxReached = true;
    }
    const exceededLimit = Math.max(0, cost - coverageLimit);
    userPays += exceededLimit;
    const insuranceCovers = Math.max(0, cost - userPays);
    const annualPremium = policy.basePremium;
    const totalWithInsurance = userPays + annualPremium;
    const savings = cost - totalWithInsurance;
    const savingsPercent = cost > 0 ? (savings / cost) * 100 : 0;
    const financialRuinThreshold = 50000;
    let ruinTrialsWithoutInsurance = 0;
    let ruinTrialsWithInsurance = 0;
    const trials = 1000;
    for (let i = 0; i < trials; i++) {
        const variation = (Math.random() * 0.8) + 0.6;
        const simulatedCost = cost * variation;
        if (simulatedCost > financialRuinThreshold) ruinTrialsWithoutInsurance++;
        let simUserPays = Math.min(simulatedCost, deductible) + (Math.max(0, simulatedCost - deductible) * copayRate);
        if (simUserPays > oopMax) simUserPays = oopMax;
        simUserPays += Math.max(0, simulatedCost - coverageLimit);
        if (simUserPays > financialRuinThreshold) ruinTrialsWithInsurance++;
    }
    const financialRuinProb = Math.round((ruinTrialsWithInsurance / trials) * 100);
    const coverageConfidence = (insuranceCovers / (cost || 1));
    const peaceOfMindScore = Math.max(0, Math.min(100, Math.round(((1 - (ruinTrialsWithInsurance / trials)) * 80) + (coverageConfidence * 20))));
    this.result = {
      policy,
      scenarioName: this.scenarioName || 'Critical Risk Event',
      eventCost: cost,
      insuranceCovers,
      outOfPocket: userPays,
      annualPremium,
      totalWithInsurance,
      savings,
      savingsPercent,
      deductiblePaid,
      copayPaid,
      oopMaxReached,
      peaceOfMindScore,
      financialRuinProb
    };
    this.showResult = false;
    this.animateNumbers = false;
    setTimeout(() => {
      this.showResult = true;
      this.simulationsRun++;
    }, 100);
    setTimeout(() => {
      this.animateNumbers = true;
    }, 600);
  }
  resetSimulation(): void {
    this.showResult = false;
    this.animateNumbers = false;
    setTimeout(() => {
      this.result = null;
    }, 300);
  }
  formatCurrency(amount: number): string {
    return '₹' + amount.toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
  }
  get isFormValid(): boolean {
    return this.selectedPolicyId !== '' && !!this.eventCost && this.eventCost > 0;
  }
  getPolicyIcon(type: string): string {
    switch (type?.toUpperCase()) {
      case 'HEALTH': return '🏥';
      case 'LIFE': return '💼';
      case 'AUTO': return '🚗';
      case 'HOME': return '🏠';
      case 'DENTAL': return '🦷';
      default: return '🛡️';
    }
  }
  get activePolicy() {
    if (this.selectedPolicyId === '') return null;
    return this.policies.find(p => p.policyId === Number(this.selectedPolicyId)) || null;
  }
}