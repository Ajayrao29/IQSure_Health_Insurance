// IQsure Risk Simulator — Actuarial-grade Monte Carlo Engine
import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

// ── Preset risk scenarios ─────────────────────────────────────────
interface RiskScenario {
  emoji: string;
  name: string;
  category: string;
  minCost: number;
  maxCost: number;
  description: string;
  policyTypeHint: string; // suggested policy type
}

// ── Full actuarial result ─────────────────────────────────────────
interface SimulationResult {
  policy: any;
  scenarioName: string;
  eventCost: number;
  // Cost breakdown
  deductiblePaid: number;
  copayPaid: number;
  oopMaxReached: boolean;
  excessAboveCoverage: number; // cost that exceeds policy max coverage
  insuranceCovers: number;
  outOfPocket: number;       // total user pays WITHOUT annual premium
  annualPremium: number;
  totalWithInsurance: number; // outOfPocket + premium
  savings: number;           // cost - totalWithInsurance
  savingsPercent: number;
  // Monte Carlo
  financialRuinProbWithout: number; // % chance of ruin WITHOUT insurance
  financialRuinProbWith: number;    // % chance of ruin WITH insurance
  ruinReduction: number;            // how much risk is eliminated
  peaceOfMindScore: number;
  coverageRatio: number;            // what % of the event insurance actually covers
  // Distribution data for histogram
  distributionBins: { label: string; countWith: number; countWithout: number }[];
  breakEvenYears: number; // years of premiums until insurance "pays off"
}

@Component({
  selector: 'app-risk-simulator',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DecimalPipe],
  templateUrl: './risk-simulator.html',
  styleUrls: ['./risk-simulator.scss']
})
export class RiskSimulatorComponent implements OnInit {
  // ── State ───────────────────────────────────────────────────────
  policies: any[] = [];
  loading = true;
  simulating = false;

  // ── Form inputs ─────────────────────────────────────────────────
  selectedPolicyId: number | '' = '';
  scenarioName = '';
  eventCost: number | null = null;
  ageFactor = 35;         // user's age (affects life expectancy risk factor)
  riskTolerance: 'LOW' | 'MEDIUM' | 'HIGH' = 'MEDIUM'; // lifestyle risk appetite

  // ── Scenario presets ────────────────────────────────────────────
  readonly presets: RiskScenario[] = [
    { emoji: '🏥', name: 'Major Surgery',       category: 'HEALTH',    minCost: 200000, maxCost: 500000, description: 'Cardiac, orthopedic, or oncological surgery + ICU stay', policyTypeHint: 'HEALTH' },
    { emoji: '🚑', name: 'Emergency Hospitalization', category: 'HEALTH', minCost: 80000, maxCost: 200000, description: 'Critical care admission for 5–10 days + diagnostics', policyTypeHint: 'HEALTH' },
    { emoji: '🦴', name: 'Accidental Fracture', category: 'ACCIDENT',  minCost: 40000,  maxCost: 120000, description: 'Bone fracture + surgery + physiotherapy recovery', policyTypeHint: 'ACCIDENT' },
    { emoji: '💊', name: 'Chronic Illness',     category: 'HEALTH',    minCost: 100000, maxCost: 300000, description: 'Diabetes, hypertension, or cancer treatment (annual)', policyTypeHint: 'HEALTH' },
    { emoji: '🧠', name: 'Critical Illness',    category: 'HEALTH',    minCost: 500000, maxCost: 1500000, description: 'Stroke, bypass, or organ failure — extended treatment', policyTypeHint: 'HEALTH' },
    { emoji: '👶', name: 'Maternity & NICU',    category: 'HEALTH',    minCost: 50000,  maxCost: 150000, description: 'Delivery complications + neonatal intensive care', policyTypeHint: 'HEALTH' },
    { emoji: '☠️', name: 'Life Event (Family)', category: 'LIFE',      minCost: 1000000,maxCost: 5000000, description: 'Family income replacement upon breadwinner\'s death', policyTypeHint: 'LIFE' },
    { emoji: '🦷', name: 'Dental Emergency',    category: 'HEALTH',    minCost: 15000,  maxCost: 60000,   description: 'Root canal, implants, or complex orthodontics', policyTypeHint: 'HEALTH' },
  ];

  selectedPreset: RiskScenario | null = null;

  // ── Result ──────────────────────────────────────────────────────
  result: SimulationResult | null = null;
  showResult = false;
  simulationsRun = 0;

  constructor(private api: ApiService, public auth: AuthService) {}

  ngOnInit(): void {
    this.api.getActivePolicies().subscribe({
      next: (p) => { this.policies = p; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  // ── Preset selection ────────────────────────────────────────────
  selectPreset(preset: RiskScenario): void {
    this.selectedPreset = preset;
    this.scenarioName = preset.name;
    // Use midpoint as default cost
    this.eventCost = Math.round((preset.minCost + preset.maxCost) / 2);
    // Auto-select a matching policy type if available
    const match = this.policies.find(p => p.policyType === preset.policyTypeHint);
    if (match) this.selectedPolicyId = match.policyId;
  }

  clearPreset(): void {
    this.selectedPreset = null;
    this.scenarioName = '';
    this.eventCost = null;
  }

  // ── Core actuarial simulation ───────────────────────────────────
  simulate(): void {
    if (!this.isFormValid) return;
    const policy = this.policies.find(p => p.policyId === Number(this.selectedPolicyId));
    if (!policy) return;

    this.simulating = true;
    this.showResult = false;
    this.result = null;

    // Simulate with slight delay to show the loading animation
    setTimeout(() => {
      const result = this.runActuarialEngine(policy, this.eventCost!);
      this.result = result;
      this.simulating = false;
      this.showResult = true;
      this.simulationsRun++;
    }, 1400);
  }

  /**
   * Full actuarial engine — uses a log-normal distribution Monte Carlo
   * (more realistic than uniform ±40%) and computes all insurance mechanics.
   */
  private runActuarialEngine(policy: any, baseCost: number): SimulationResult {
    const deductible   = policy.deductibleAmount  ?? 0;
    const copayRate    = policy.copayPercentage    ?? 0; // 0..1
    const oopMax       = policy.outOfPocketMax     ?? Infinity;
    const coverageLimit = policy.coverageAmount;
    const annualPremium = policy.basePremium;

    // Age risk multiplier: older → higher variance
    const ageMultiplier = 1 + Math.max(0, (this.ageFactor - 30) / 100);
    // Risk tolerance → affects financial ruin threshold
    const ruinThresholdMultiplier = this.riskTolerance === 'LOW' ? 0.6 : this.riskTolerance === 'HIGH' ? 1.5 : 1.0;
    const ruinThreshold = 50000 * ruinThresholdMultiplier;

    // ── Deterministic calculation for base cost ──────────────────
    const deductiblePaid = Math.min(baseCost, deductible);
    const afterDeductible = Math.max(0, baseCost - deductible);
    const copayPaid = afterDeductible * copayRate;
    let userPaysBase = deductiblePaid + copayPaid;
    let oopMaxReached = false;
    if (userPaysBase > oopMax) { userPaysBase = oopMax; oopMaxReached = true; }
    const excessAboveCoverage = Math.max(0, baseCost - coverageLimit);
    userPaysBase += excessAboveCoverage;
    const insuranceCovers = Math.max(0, baseCost - userPaysBase);
    const totalWithInsurance = userPaysBase + annualPremium;
    const savings = baseCost - totalWithInsurance;
    const savingsPercent = baseCost > 0 ? (savings / baseCost) * 100 : 0;
    const coverageRatio = baseCost > 0 ? (insuranceCovers / baseCost) * 100 : 0;

    // ── Break-even years ─────────────────────────────────────────
    const breakEvenYears = annualPremium > 0
      ? +(insuranceCovers / annualPremium).toFixed(1)
      : 0;

    // ── Log-normal Monte Carlo (10,000 trials) ───────────────────
    const TRIALS = 10000;
    const mu = Math.log(baseCost) + (ageMultiplier - 1) * 0.1;
    const sigma = 0.4 * ageMultiplier; // std dev in log space

    let ruinCountWithout = 0;
    let ruinCountWith = 0;

    // Build histogram bins (6 ranges)
    const binEdges = [0, 0.5, 0.8, 1.0, 1.5, 2.0, Infinity];
    const binLabels = ['<50%', '50–80%', '80–100%', '100–150%', '150–200%', '>200%'];
    const binCountsWithout = new Array(binLabels.length).fill(0);
    const binCountsWith    = new Array(binLabels.length).fill(0);

    for (let i = 0; i < TRIALS; i++) {
      // Box-Muller log-normal sample
      const u1 = Math.random(), u2 = Math.random();
      const z  = Math.sqrt(-2 * Math.log(u1 + 1e-10)) * Math.cos(2 * Math.PI * u2);
      const simCost = Math.exp(mu + sigma * z);

      // Without insurance
      if (simCost > ruinThreshold) ruinCountWithout++;
      const ratio = simCost / baseCost;
      const binIdx = binEdges.findIndex((_, j) => ratio >= binEdges[j] && ratio < binEdges[j + 1]);
      if (binIdx >= 0 && binIdx < binLabels.length) binCountsWithout[binIdx]++;

      // With insurance
      const simDed   = Math.min(simCost, deductible);
      const simAfter = Math.max(0, simCost - deductible);
      const simCopay = simAfter * copayRate;
      let simUserPays = simDed + simCopay;
      if (simUserPays > oopMax) simUserPays = oopMax;
      simUserPays += Math.max(0, simCost - coverageLimit);
      if (simUserPays > ruinThreshold) ruinCountWith++;
      if (binIdx >= 0 && binIdx < binLabels.length) binCountsWith[binIdx]++;
    }

    const financialRuinProbWithout = Math.round((ruinCountWithout / TRIALS) * 100);
    const financialRuinProbWith    = Math.round((ruinCountWith    / TRIALS) * 100);
    const ruinReduction = Math.max(0, financialRuinProbWithout - financialRuinProbWith);

    // ── Peace of Mind Score (0–100) ──────────────────────────────
    // Weighted: 50% ruin reduction, 30% coverage ratio, 20% OOP relief
    const ruinFactor     = (1 - ruinCountWith / TRIALS) * 50;
    const coverageFactor = (coverageRatio / 100) * 30;
    const oopFactor      = oopMaxReached ? 20 : Math.min(20, (1 - userPaysBase / baseCost) * 20);
    const peaceOfMindScore = Math.round(Math.min(100, Math.max(0, ruinFactor + coverageFactor + oopFactor)));

    // ── Histogram bins (normalised to %) ────────────────────────
    const distributionBins = binLabels.map((label, i) => ({
      label,
      countWith:    Math.round((binCountsWith[i]    / TRIALS) * 100),
      countWithout: Math.round((binCountsWithout[i] / TRIALS) * 100)
    }));

    return {
      policy,
      scenarioName: this.scenarioName || 'Critical Risk Event',
      eventCost: baseCost,
      deductiblePaid,
      copayPaid,
      oopMaxReached,
      excessAboveCoverage,
      insuranceCovers,
      outOfPocket: userPaysBase,
      annualPremium,
      totalWithInsurance,
      savings,
      savingsPercent,
      financialRuinProbWithout,
      financialRuinProbWith,
      ruinReduction,
      peaceOfMindScore,
      coverageRatio,
      distributionBins,
      breakEvenYears
    };
  }

  resetSimulation(): void {
    this.showResult = false;
    this.simulating = false;
    setTimeout(() => { this.result = null; }, 300);
  }

  // ── Helpers ─────────────────────────────────────────────────────
  get isFormValid(): boolean {
    return this.selectedPolicyId !== '' && !!this.eventCost && this.eventCost > 0;
  }

  get activePolicy(): any {
    if (this.selectedPolicyId === '') return null;
    return this.policies.find(p => p.policyId === Number(this.selectedPolicyId)) || null;
  }

  formatCurrency(amount: number): string {
    if (amount >= 100000) return '₹' + (amount / 100000).toFixed(2) + 'L';
    if (amount >= 1000)   return '₹' + (amount / 1000).toFixed(1) + 'K';
    return '₹' + Math.round(amount).toLocaleString('en-IN');
  }

  formatCurrencyFull(amount: number): string {
    return '₹' + Math.round(amount).toLocaleString('en-IN');
  }

  getPolicyIcon(type: string): string {
    switch (type?.toUpperCase()) {
      case 'HEALTH':   return '🏥';
      case 'LIFE':     return '💛';
      case 'ACCIDENT': return '⚡';
      default:         return '🛡️';
    }
  }

  getPeaceLabel(score: number): string {
    if (score >= 80) return 'Antifragile';
    if (score >= 60) return 'Well Protected';
    if (score >= 40) return 'Moderate Shield';
    return 'Vulnerable';
  }

  getPeaceColor(score: number): string {
    if (score >= 80) return '#10B981';
    if (score >= 60) return '#3B82F6';
    if (score >= 40) return '#F59E0B';
    return '#EF4444';
  }

  getRuinLabel(prob: number): string {
    if (prob <= 5)  return 'Negligible';
    if (prob <= 20) return 'Low';
    if (prob <= 40) return 'Moderate';
    if (prob <= 60) return 'High';
    return 'Critical';
  }

  getMaxBinCount(): number {
    if (!this.result) return 100;
    return Math.max(...this.result.distributionBins.map(b => Math.max(b.countWith, b.countWithout)), 1);
  }
}