/*
 * FILE: achievements.ts | LOCATION: pages/achievements/
 * PURPOSE: Achievements page (URL: /achievements). Shows progress, level, and 8 achievement milestones.
 *          Checks user's quiz history, policies, badges, and leaderboard position to unlock achievements.
 * TEMPLATE: achievements.html | STYLES: achievements.scss
 * CALLS: api.service.ts → getProfile(), getAttemptsByUser(), getUserPolicies(), getBadgesByUser(), getLeaderboard()
 * BACKEND: Multiple controllers — UserController, AttemptController, UserPolicyController, BadgeController
 */
import { Component, OnInit } from '@angular/core';

import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-achievements',
  standalone: true,
  imports: [],
  templateUrl: './achievements.html',
  styleUrls: ['./achievements.scss']
})
export class AchievementsComponent implements OnInit {
  userPoints = 0;
  level = 1;
  levelProgress = 0;
  nextLevelPoints = 100;
  completionRate = 0;
  streak = 0;

  achievements = [
    { icon: '🎓', name: 'First Steps', description: 'Complete your first quiz', points: 10, unlocked: false },
    { icon: '💯', name: 'Perfect Score', description: 'Score 100% on any quiz', points: 50, unlocked: false },
    { icon: '🔥', name: 'On Fire', description: 'Complete 3 quizzes in one day', points: 30, unlocked: false },
    { icon: '📚', name: 'Knowledge Seeker', description: 'Complete all 3 quiz categories', points: 75, unlocked: false },
    { icon: '💎', name: 'Premium Hunter', description: 'Purchase your first policy', points: 100, unlocked: false },
    { icon: '🎯', name: 'Sharpshooter', description: 'Score 80%+ on 5 quizzes', points: 150, unlocked: false },
    { icon: '👑', name: 'Top 10', description: 'Reach top 10 on leaderboard', points: 200, unlocked: false },
    { icon: '🌟', name: 'Badge Collector', description: 'Unlock all badges', points: 250, unlocked: false },
  ];

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit(): void {
    const userId = this.auth.getUserId()!;
    this.api.getProfile(userId).subscribe(u => {
      this.userPoints = u.userPoints;
      this.streak = u.currentStreak || 0;
      this.calculateLevel();
      this.checkAchievements(userId, u);
    });
  }

  calculateLevel(): void {
    const pointsPerLevel = 100;
    this.level = Math.floor(this.userPoints / pointsPerLevel) + 1;
    this.nextLevelPoints = this.level * pointsPerLevel;
    this.levelProgress = ((this.userPoints % pointsPerLevel) / pointsPerLevel) * 100;
  }

  checkAchievements(userId: number, user: any): void {
    // Basic achievements from user profile
    if (user.totalQuizzesTaken > 0) this.achievements[0].unlocked = true;
    if (user.totalQuizzesTaken >= 10) this.achievements[3].unlocked = true; // Knowledge Master

    this.api.getAttemptsByUser(userId).subscribe(attempts => {
      // 1. Perfect Score
      if (attempts.some(a => a.percentage === 100)) this.achievements[1].unlocked = true;
      
      // 2. On Fire (3 in one day)
      const today = new Date().toDateString();
      if (attempts.filter(a => new Date(a.attemptDate).toDateString() === today).length >= 3)
        this.achievements[2].unlocked = true;

      // 3. Sharpshooter (5 at 80%+)
      if (attempts.filter(a => a.percentage >= 80).length >= 5) this.achievements[5].unlocked = true;

      // Completion Rate (relative to a goal, e.g., 20)
      const goal = 20;
      this.completionRate = Math.min(100, Math.round((attempts.length / goal) * 100));
    });

    this.api.getUserPolicies(userId).subscribe(policies => {
      if (policies.length > 0) this.achievements[4].unlocked = true;
    });

    this.api.getBadgesByUser(userId).subscribe(badges => {
      if (badges.length >= 4) this.achievements[7].unlocked = true;
    });

    this.api.getLeaderboard().subscribe(board => {
      const entry = board.find(e => e.userId === userId);
      if (entry && entry.rank <= 10) this.achievements[6].unlocked = true;
    });
  }
}
