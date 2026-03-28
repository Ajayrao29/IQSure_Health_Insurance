// Angular component for the dashboard page
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
  loading = true;
  private destroy$ = new Subject<void>();
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
          if (this.user) {
            this.user.userPoints = user.userPoints || 0;
            this.user.totalQuizzesTaken = user.totalQuizzesTaken || 0;
            this.user.currentStreak = user.currentStreak || 0;
          }
        }
      });
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
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
  get recentAttempts(): AttemptResponse[] {
    return this.myAttempts;
  }
}