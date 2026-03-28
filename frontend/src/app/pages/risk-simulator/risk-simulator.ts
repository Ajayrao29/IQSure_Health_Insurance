/*
 * FILE: risk-simulator.ts | LOCATION: pages/risk-simulator/
 * PURPOSE: Risk Awareness Simulator page.
 *          User selects a REAL active policy from the platform, enters a custom scenario
 *          (e.g., "Car crash", "House fire") and the estimated cost.
 *          The simulator calculates how much the selected policy would cover
 *          (up to its coverageAmount) vs paying entirely out of pocket.
 * TEMPLATE: risk-simulator.html | STYLES: risk-simulator.scss
 */
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
  
  // Actuarial details
  deductiblePaid: number;
  copayPaid: number;
  oopMaxReached: boolean;
  
  // Monte Carlo Insights
  peaceOfMindScore: number; // 0-100
  financialRuinProb: number; // Probability cost exceeds user's likely liquidity
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

  /* ── Form state ── */
  selectedPolicyId: number | '' = '';
  scenarioName = '';
  eventCost: number | null = null;

  /* ── Result state ── */
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

  /* ── Run the Advanced Actuarial Simulation ── */
  simulate(): void {
    if (!this.isFormValid) return;

    const policy = this.policies.find(p => p.policyId === Number(this.selectedPolicyId));
    if (!policy) return;

    const cost = this.eventCost!;
    const deductible = policy.deductibleAmount || 0;
    const copayRate = policy.copayPercentage || 0;
    const oopMax = policy.outOfPocketMax || Infinity;
    const coverageLimit = policy.coverageAmount;

    // 1. Calculate Out-of-Pocket (OOP) Actuarially
    // User always pays the first 'deductible' amount or the cost if cost < deductible
    let userPays = Math.min(cost, deductible);
    const deductiblePaid = userPays;

    let remaining = Math.max(0, cost - deductible);
    
    // User pays copay on the rest
    const copayPaid = remaining * copayRate;
    userPays += copayPaid;

    // 2. Apply Out-of-Pocket Maximum
    let oopMaxReached = false;
    if (userPays > oopMax) {
      userPays = oopMax;
      oopMaxReached = true;
    }

    // 3. Apply Coverage Limit
    // If the event cost is massive, the user might have to pay anything exceeding the coverage limit
    const exceededLimit = Math.max(0, cost - coverageLimit);
    userPays += exceededLimit;

    const insuranceCovers = Math.max(0, cost - userPays);
    const annualPremium = policy.basePremium;
    const totalWithInsurance = userPays + annualPremium;
    
    const savings = cost - totalWithInsurance;
    const savingsPercent = cost > 0 ? (savings / cost) * 100 : 0;

    // 4. Monte Carlo Logic: Calculate "Peace of Mind" and "Risk of Ruin"
    // We simulate 1000 variations of this cost to see how the policy holds up
    const financialRuinThreshold = 50000; 
    let ruinTrialsWithoutInsurance = 0;
    let ruinTrialsWithInsurance = 0;
    const trials = 1000;
    
    for (let i = 0; i < trials; i++) {
        // Variation +/- 40%
        const variation = (Math.random() * 0.8) + 0.6; // 0.6 to 1.4
        const simulatedCost = cost * variation;
        
        // Without Insurance
        if (simulatedCost > financialRuinThreshold) ruinTrialsWithoutInsurance++;
        
        // With Insurance (Apply same actuarial logic to simulated cost)
        let simUserPays = Math.min(simulatedCost, deductible) + (Math.max(0, simulatedCost - deductible) * copayRate);
        if (simUserPays > oopMax) simUserPays = oopMax;
        simUserPays += Math.max(0, simulatedCost - coverageLimit);
        
        if (simUserPays > financialRuinThreshold) ruinTrialsWithInsurance++;
    }

    // Probability of ruin is now based ONLY on what the USER actually pays
    const financialRuinProb = Math.round((ruinTrialsWithInsurance / trials) * 100);
    
    // Peace of Mind is 100% minus the remaining risk of ruin, 
    // also weighted by how much of the original cost was covered.
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

  /* ── Reset to try another scenario ── */
  resetSimulation(): void {
    this.showResult = false;
    this.animateNumbers = false;
    setTimeout(() => {
      this.result = null;
      // Keep form values so they can easily tweak the cost or policy and re-run
    }, 300);
  }

  /* ── Helpers ── */
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
