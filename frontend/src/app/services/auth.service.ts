// Service containing business logic for auth.service

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { AuthResponse } from '../models/models';
@Injectable({ providedIn: 'root' })
export class AuthService {
  private KEY = 'iqsure_user';
  private userSub = new BehaviorSubject<AuthResponse | null>(this.getStoredUser());
  currentUser$ = this.userSub.asObservable();
  private getStoredUser(): AuthResponse | null {
    const data = localStorage.getItem(this.KEY);
    return data ? JSON.parse(data) : null;
  }
  saveUser(user: AuthResponse): void {
    localStorage.setItem(this.KEY, JSON.stringify(user));
    this.userSub.next(user);
  }
  getUser(): AuthResponse | null {
    return this.userSub.value;
  }
  updateUserGamification(points: number, quizzes: number, streak: number, xp: number, rank: string, integrity: number): void {
    const user = this.getUser();
    if (user) {
      const updatedUser = { 
        ...user, 
        userPoints: points, 
        totalQuizzesTaken: quizzes, 
        currentStreak: streak, 
        experiencePoints: xp, 
        rank: rank, 
        fortressIntegrity: integrity 
      };
      this.saveUser(updatedUser);
    }
  }
  isLoggedIn(): boolean {
    return this.getUser() !== null;
  }
  isAdmin(): boolean {
    return this.getUser()?.role === 'ROLE_ADMIN';
  }
  getUserId(): number | null {
    return this.getUser()?.userId ?? null;
  }
  getToken(): string | null {
    return this.getUser()?.token ?? null;
  }
  logout(): void {
    localStorage.removeItem(this.KEY);
    this.userSub.next(null);
  }
}