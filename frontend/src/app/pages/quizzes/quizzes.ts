// Angular component for the quizzes page

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
@Component({ selector: 'app-quizzes', standalone: true, imports: [CommonModule], templateUrl: './quizzes.html', styleUrls: ['./quizzes.scss'] })
export class QuizzesComponent implements OnInit {
  quizzes: any[] = []; loading = true;
  constructor(private api: ApiService, private router: Router) {}
  ngOnInit(): void { this.api.getAllQuizzes().subscribe(q => { this.quizzes = q; this.loading = false; }); }
  takeQuiz(quizId: number): void { this.router.navigate(['/take-quiz', quizId]); }
  getDifficultyClass(diff: string): string { return diff === 'EASY' ? 'easy' : diff === 'MEDIUM' ? 'medium' : 'hard'; }
}