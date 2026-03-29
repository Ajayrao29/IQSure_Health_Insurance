// Angular component for the savings-calculator page
import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-savings',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  templateUrl: './savings-calculator.html',
  styleUrls: ['./savings-calculator.scss']
})
export class SavingsCalculatorComponent implements OnInit {
  totalSavings = 0;       // Actual discounts earned on purchased policies
  potentialSavings = 0;   // How much more the user could save with their current rewards
  policies: any[] = [];
  userPoints = 0;
  availableRewards: any[] = [];
  projectedYears = [5, 10, 20, 30];
  expectedReturn = 0.08;  // 8% annual return assumption
  wealthData: { years: number; wealth: number }[] = [];
  maxWealth = 0;

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit(): void {
    const userId = this.auth.getUserId()!;

    this.api.getUserPolicies(userId).subscribe(policies => {
      this.policies = policies;
      // totalSavings = sum of premium discounts earned (difference between base and final premium)
      this.totalSavings = policies.reduce((sum, p) => sum + (p.savedAmount || 0), 0);
      // Generate projection after loading savings
      this.generateWealthProjection();
    });

    this.api.getProfile(userId).subscribe(u => {
      this.userPoints = u.userPoints || 0;
    });

    // Load outstanding unused rewards (potential additional savings)
    this.api.getEarnedRewardsByUser(userId).subscribe(rewards => {
      this.availableRewards = (rewards || []).filter((r: any) => !r.used && !r.isExpired);
      this.potentialSavings = this.availableRewards.reduce(
        (sum, r) => sum + (r.discountValue || 0), 0
      );
    });
  }

  /**
   * Projects the compound growth of the user's realized premium savings
   * if they were invested at the expected annual return rate.
   * This is the correct interpretation: "how much would your insurance savings
   * be worth if you invested them?"
   */
  generateWealthProjection(): void {
    // If no savings yet, show projection on a minimum of ₹2,000 to make the chart meaningful
    const principal = Math.max(this.totalSavings, 2000);

    this.wealthData = this.projectedYears.map(year => {
      // Compound interest: P * (1 + r)^n
      const wealth = Math.round(principal * Math.pow(1 + this.expectedReturn, year));
      return { years: year, wealth };
    });

    this.maxWealth = Math.max(...this.wealthData.map(d => d.wealth));
  }

  getBarHeight(wealth: number): number {
    if (this.maxWealth === 0) return 0;
    return Math.round((wealth / this.maxWealth) * 100);
  }
}