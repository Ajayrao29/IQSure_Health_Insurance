import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { 
  User, LeaderboardEntry, Quiz, Claim, Badge, 
  AttemptResponse, UserPolicy, PremiumBreakdown 
} from '../../models/models';
import { forkJoin, Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {

  /* ───── Shared ───── */
  loading = true;
  private destroy$ = new Subject<void>();

  /* ───── User-specific ───── */
  user: User | null = null;
  myBadges: Badge[] = [];
  myAttempts: AttemptResponse[] = [];
  myPolicies: UserPolicy[] = [];
  totalSavings: number = 0;
  welcomeImgUrl = 'assets/welcome_hero.png';
  
  userStats = {
    totalPolicies: 0,
    activePolicies: 0,
    awaitingQuote: 0,
    pendingClaims: 0
  };

  aiInsight: string = '';
  cognitiveLevel: 'STANDARD' | 'PRO' | 'ELITE' = 'STANDARD';

  /* ───── Guardian System (Expert Mode) ───── */
  guardianRank = {
    name: 'Risk Observer',
    level: 1,
    icon: '🛡️',
    color: '#64748b',
    nextThreshold: 100,
    progress: 0
  };

  fortressMetrics = {
    knowledge: 0,    // Based on Quizzes Attempted
    proficiency: 0,  // Based on Avg Score
    protection: 0,   // Based on Active Policies
    resilience: 0    // Based on Badges
  };

  /* ───── Admin-specific ───── */
  recentUsers: User[] = [];
  stats = {
    totalUsers: 0,
    totalAdmins: 0,
    totalCustomers: 0,
    totalQuizzes: 0,
    totalPolicies: 0,
    activePolicies: 0,
    inactivePolicies: 0,
    totalRewards: 0,
    discountRules: 0
  };

  constructor(
    public auth: AuthService, 
    private api: ApiService
  ) {}

  ngOnInit(): void {
    if (this.auth.isAdmin()) {
      this.loadAdminDashboard();
    } else {
      this.loadUserDashboard();
      this.subscribeToUserChanges();
    }
  }

  private subscribeToUserChanges(): void {
    this.auth.currentUser$.pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        if (user) {
          // Sync local points and stats if they changed in the session
          if (this.user) {
            this.user.userPoints = user.userPoints || 0;
            this.user.totalQuizzesTaken = user.totalQuizzesTaken || 0;
            this.user.currentStreak = user.currentStreak || 0;
            
            // Re-run calculations that depend on points
            this.generateAiInsights(this.user);
            this.calculateGuardianRank(this.user, this.myAttempts, this.myBadges, this.myPolicies);
          }
        }
      });
  }

  ngOnDestroy(): void {
    // Best Practice: Unsubscribe from all observables to avoid memory leaks
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Loads high-level platform stats for the admin view.
   * Best Practice: Use forkJoin to fetch multiple related datasets concurrently.
   */
  private loadAdminDashboard(): void {
    forkJoin({
      users: this.api.getAllUsers(),
      quizzes: this.api.getAllQuizzes(),
      rewards: this.api.getAllRewards()
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ users, quizzes, rewards }) => {
          this.stats.totalUsers = users.length;
          this.stats.totalAdmins = users.filter(u => u.role === 'ROLE_ADMIN').length;
          this.stats.totalCustomers = users.filter(u => u.role !== 'ROLE_ADMIN').length;
          this.stats.totalQuizzes = quizzes.length;
          this.stats.totalRewards = rewards.length;
          this.recentUsers = [...users].reverse().slice(0, 5);
          this.loading = false;
        },
        error: (err) => {
          console.error('Failed to load admin stats:', err);
          this.loading = false;
        }
      });
  }

  /**
   * Loads personal portfolio data for the customer view.
   * Best Practice: Consolidate data into a single view model for the UI.
   */
  private loadUserDashboard(): void {
    const userId = this.auth.getUserId()!;
    
    forkJoin({
      profile: this.api.getProfile(userId),
      badges: this.api.getBadgesByUser(userId),
      attempts: this.api.getAttemptsByUser(userId),
      policies: this.api.getUserPolicies(userId),
      claims: this.api.getClaimsByUser(userId)
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ profile, badges, attempts, policies, claims }) => {
          this.user = profile;
          this.myBadges = badges;
          this.myAttempts = [...attempts].reverse().slice(0, 5);
          this.myPolicies = policies;
          this.totalSavings = policies.reduce((sum, p) => sum + (p.totalClaimedAmount || 0), 0);
          
          this.updateMetrics(policies, claims);
          this.generateAiInsights(profile);
          this.calculateGuardianRank(profile, attempts, badges, policies);
          
          this.loading = false;
        },
        error: (err) => {
          console.error('Failed to load user dashboard:', err);
          this.loading = false;
        }
      });
  }

  private updateMetrics(policies: UserPolicy[], claims: Claim[]): void {
    this.userStats.totalPolicies = policies.length;
    this.userStats.activePolicies = policies.filter(p => p.status === 'ACTIVE').length;
    this.userStats.awaitingQuote = policies.filter(p => p.status === 'PENDING_UNDERWRITING').length;
    this.userStats.pendingClaims = claims.filter(c => c.status === 'SUBMITTED' || c.status === 'UNDER_REVIEW').length;
  }

  /**
   * SIMULATED AI AGENT: Predictive risk analysis based on user quiz performance.
   */
  private generateAiInsights(profile: User): void {
    const points = profile.userPoints || 0;
    this.cognitiveLevel = points >= 500 ? 'ELITE' : points >= 200 ? 'PRO' : 'STANDARD';
    
    // Actuarial hazard mitigation calculation
    const riskMitigationValue = points * 150; 

    if (points >= 500) {
      this.aiInsight = `Superior Cognitive Status! Your projected liability mitigation is ₹${riskMitigationValue.toLocaleString()}. You've reached a 98th-percentile safety quotient.`;
    } else {
      const remaining = 500 - points;
      this.aiInsight = `Hazard Alert: You've identified ₹${riskMitigationValue.toLocaleString()} in potential risks. Earning ${remaining} more points will unlock the 'ELITE' priority settlement status.`;
    }
  }

  get recentAttempts(): AttemptResponse[] {
    return this.myAttempts;
  }


  private calculateGuardianRank(user: User, attempts: AttemptResponse[], badges: Badge[], policies: UserPolicy[]): void {
    const points = user.userPoints || 0;
    
    // 1. Calculate Rank
    if (points >= 5000) {
      this.guardianRank = { name: 'Insurance Oracle', level: 5, icon: '💎', color: '#7c3aed', nextThreshold: 10000, progress: 100 };
    } else if (points >= 2001) {
      this.guardianRank = { name: 'Wealth Sentinel', level: 4, icon: '🥇', color: '#fbbf24', nextThreshold: 5000, progress: ((points - 2001) / 2999) * 100 };
    } else if (points >= 501) {
      this.guardianRank = { name: 'Risk Strategist', level: 3, icon: '🌟', color: '#10b981', nextThreshold: 2000, progress: ((points - 501) / 1499) * 100 };
    } else if (points >= 101) {
      this.guardianRank = { name: 'Asset Protector', level: 2, icon: '⚔️', color: '#3b82f6', nextThreshold: 500, progress: ((points - 101) / 399) * 100 };
    } else {
      this.guardianRank = { name: 'Risk Observer', level: 1, icon: '🛡️', color: '#64748b', nextThreshold: 100, progress: (points / 100) * 100 };
    }

    // 2. Calculate Fortress Metrics (0-100 scale)
    this.fortressMetrics.knowledge = Math.min(100, (attempts.length / 10) * 100);
    
    const avgScore = attempts.length > 0 ? attempts.reduce((acc, cr) => acc + cr.score, 0) / attempts.length : 0;
    this.fortressMetrics.proficiency = avgScore;
    
    this.fortressMetrics.protection = Math.min(100, (policies.filter(p => p.status === 'ACTIVE').length / 3) * 100);
    this.fortressMetrics.resilience = Math.min(100, (badges.length / 5) * 100);
  }

  shareAchievement(): void {
    if (!this.user) return;
    
    const text = `I've earned ${this.user.userPoints} points and reached the rank of ${this.guardianRank.name} on IQsure! Join me in maximizing health savings.`;
    if (navigator.share) {
      navigator.share({ title: 'IQsure Achievement', text });
    } else {
      navigator.clipboard.writeText(text);
      alert('Achievement link copied!');
    }
  }

  /* ───── Expert Gamification Actions ───── */
  verifyCoverage(): void {
    alert('Scanning your active policies for Water Damage riders... \n\nOracle Result: Verification successful. You are currently protected under Section 4.2 of your Home Policy.');
  }

  skipPulse(): void {
    alert('Scenario dismissed. Staying vigilant is key to maintaining your Resilience score!');
  }
}
