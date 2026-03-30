
export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  name: string;
  email: string;
  role: string;
  userPoints: number;
  totalQuizzesTaken?: number;
  currentStreak?: number;
  licenseNumber?: string;
  employeeId?: string;
  department?: string;
  approvalLimit?: number;
  experiencePoints?: number;
  rank?: string;
  fortressIntegrity?: number;
}
export interface RegisterRequest { name: string; email: string; password: any; phone?: string; }
export interface LoginRequest { email: string; password: any; }
export interface ResetPasswordRequest { otp: string; newPassword: any; }
export interface User {
  userId: number; name: string; email: string; phone: string; userPoints: number; role: string;
  totalQuizzesTaken: number; currentStreak: number;
  experiencePoints: number; rank: string; fortressIntegrity: number;
  licenseNumber?: string; specialization?: string; commissionPercentage?: number; totalQuotesSent?: number;
  employeeId?: string; department?: string; approvalLimit?: number; totalClaimsProcessed?: number;
  totalClaimsApproved?: number; totalClaimsRejected?: number;
  city?: string; state?: string; zipCode?: string; status?: string;
}
export interface LeaderboardEntry { rank: number; userId: number; name: string; userPoints: number; quizzesAttempted: number; }
export interface Quiz { quizId: number; title: string; category: string; difficulty: string; totalQuestions: number; }
export interface Question { questionId: number; quizId: number; text: string; options: string[]; explanation?: string; }
export interface QuestionReport { questionText: string; selectedAnswer: string; correctAnswer: string; explanation: string; isCorrect: boolean; }
export interface AttemptResponse { attemptId: number; userId: number; quizId: number; quizTitle: string; score: number; totalQuestions: number; percentage: number; pointsEarned: number; attemptDate: string; newBadgesUnlocked: Badge[]; questions?: QuestionReport[]; questionReportJson?: string; }
export interface Badge { badgeId: number; name: string; description: string; reqPoints: number; icon?: string; }
export interface Reward {
  rewardId: number;
  rewardType: string;
  discountValue: number;
  expiryDate: string;
  userRewardId?: number;
}
export interface UserRewardResponse {
  userRewardId: number;
  rewardTitle: string;
  rewardType: string;
  discountValue: number;
  expiryDate: string;
  earnedOn: string;
  used: boolean;
  isExpired: boolean;
}
export interface UnderwriterStats {
  pendingAssignments: number;
  quotesSent: number;
  activePolicies: number;
  customersServed: number;
  totalPremium: number;
  commissionEarned: number;
}
export interface Policy {
  policyId: number; title: string; description: string; policyType: string;
  basePremium: number; coverageAmount: number; durationMonths: number;
  isActive: boolean;
  ageRange?: string;
  planType?: string;
  waitingPeriod?: string;
  hasMaternityCover?: boolean;
  hasPreExistingCover?: boolean;
  deductibleAmount?: number;
  outOfPocketMax?: number;
  copayPercentage?: number;
}
export interface UserPolicy {
  id: number; userId: number; userName: string; policyId: number; policyTitle: string;
  policyType: string; basePremium: number; coverageAmount: number; durationMonths: number;
  finalPremium: number; discountApplied: number; purchaseDate: string; expiryDate?: string; status: string;
  savedAmount: number;
  assignedUnderwriterId?: number; assignedUnderwriterName?: string; assignedAt?: string;
  quoteAmount?: number; underwriterRemarks?: string; totalClaimedAmount?: number;
  remainingCoverage?: number;
  nomineeName?: string;
  nomineeRelationship?: string;
  healthReportPath?: string;
  insuredMembers?: InsuredMember[];
}
export interface InsuredMember {
  id?: number;
  fullName: string;
  relationship: string;
  dateOfBirth: string;
  gender: string;
  preExistingConditions: string;
}
export interface PremiumBreakdown { policyId: number; policyTitle: string; policyType: string; basePremium: number; durationMonths: number; coverageAmount: number; userId: number; userPoints: number; badgesEarned: number; bestQuizScorePercent: number; appliedDiscounts: AppliedDiscount[]; totalDiscountPercent: number; discountedAmount: number; finalPremium: number; calculatedAt: string; }
export interface AppliedDiscount { ruleName: string; discountPercentage: number; reason: string; }
export interface DiscountRule { ruleId: number; ruleName: string; description: string; minQuizScorePercent: number; minUserPoints: number; minBadgesEarned: number; discountPercentage: number; applicablePolicyType: string | null; isActive: boolean; }
export interface EducationContent { id: number; topic: string; language: string; title: string; content: string; icon?: string; }
export interface Claim {
  id: number;
  claimNumber: string;
  userId: number;
  userPolicyId: number;
  policyTitle?: string;
  type: string;
  amount: number;
  approvedAmount?: number;
  settlementAmount?: number;
  hospitalName?: string;
  incidentDate?: string;
  diagnosis?: string;
  status: string;
  rejectionReason?: string;
  assignedOfficerId?: number;
  assignedOfficerName?: string;
  reviewStartedAt?: string;
  reviewedAt?: string;
  reviewerRemarks?: string;
  settlementDate?: string;
  aiAuditSummary?: string;
  fraudRiskScore?: number;
  createdAt: string;
}
export interface PremiumCalculationLog {
  logId: number;
  userId: number;
  policyId: number;
  policyTitle: string;
  basePremium: number;
  totalDiscountPercent: number;
  finalPremium: number;
  userPointsSnapshot: number;
  badgeCountSnapshot: number;
  bestQuizScoreSnapshot: number;
  appliedRuleNames: string;
  calculatedAt: string;
}