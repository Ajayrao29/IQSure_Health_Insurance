// Angular component for the education-center.component page
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../services/api.service';
import { AuthService } from '../../../services/auth.service';
import { EducationContent } from '../../../models/models';
import { marked } from 'marked';
@Component({
  selector: 'app-education-center',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './education-center.html',
  styleUrls: ['./education-center.scss']
})
export class EducationCenterComponent implements OnInit {
  languages = [
    { code: 'en', label: 'English' },
    { code: 'hi', label: 'Hindi' },
    { code: 'te', label: 'Telugu' },
    { code: 'es', label: 'Spanish' },
    { code: 'kn', label: 'Kannada' }
  ];
  isLangMenuOpen = false;
  selectedLanguageCode = 'en';
  contents: EducationContent[] = [];
  selectedTopic: EducationContent | null = null;
  loading = false;
  currentAudio: HTMLAudioElement | null = null;
  isPlaying = false;
  searchTopic = '';
  isGenerating = false;
  aiQuizQuestions: any[] = [];
  showAiQuiz = false;
  userAnswers: { [qIndex: number]: number } = {};
  quizPassingScore = 80;
  showScorecard = false;
  lastScore = 0;
  correctCount = 0;
  feedbackMessage = '';
  feedbackType: 'success' | 'error' | 'info' = 'info';
  followUpDoubt = '';
  followUpResponse = '';
  isAskingFollowUp = false;
  completedTopicTitles: string[] = [];
  curriculum = [
    { title: 'Health Insurance Foundations: Why do I need it?', topic: 'Skill 1: Ground Zero', icon: '🌱' },
    { title: 'The Cost Equation: Premiums, Deductibles & Co-pay', topic: 'Skill 2: Financials', icon: '💰' },
    { title: 'Hospital Networks: The Secret to Cashless Claims', topic: 'Skill 3: Access', icon: '🏥' },
    { title: 'Comparison Master: Critical Illness vs Comprehensive', topic: 'Skill 4: Analysis', icon: '⚖️' },
    { title: 'The Fine Print: Waiting Periods & Pre-existing T&Cs', topic: 'Skill 5: Legal', icon: '📝' },
    { title: 'Financial Fortress: Advanced Tax & Coverage Strategy', topic: 'Skill 6: Mastery', icon: '🏰' }
  ];
  
  // NEW PRO QUIZ FLOW
  quizStep: 'LESSON' | 'PRE_QUIZ' | 'QUIZ' | 'RESULT' = 'LESSON';
  canEarnRewards = true;
  detailedReport: any[] = [];
  rewardsClaimed = false;
  protected readonly Object = Object;
  constructor(private api: ApiService, public auth: AuthService) {}
  ngOnInit() {
    this.syncProgress();
    this.loadContent();
    if (window.speechSynthesis) {
        window.speechSynthesis.getVoices();
        window.speechSynthesis.onvoiceschanged = () => {
            window.speechSynthesis.getVoices();
        };
    }
  }
  syncProgress() {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getAttemptsByUser(userId).subscribe(attempts => {
        this.completedTopicTitles = attempts
          .filter(a => a.percentage >= this.quizPassingScore)
          .map(a => a.quizTitle?.trim()); // Use trimmed titles for comparison
      });
    }
  }
  onLanguageChange() {
    this.stopAudio();
    this.selectedTopic = null;
    this.loadContent();
  }
  get currentLanguageLabel() {
    return this.languages.find(l => l.code === this.selectedLanguageCode)?.label || 'English';
  }
  selectLanguage(lang: any) {
    this.selectedLanguageCode = lang.code;
    this.isLangMenuOpen = false;
    this.onLanguageChange();
  }
  stopAudio() {
    this.isPlaying = false;
    if (this.currentAudio) {
      this.currentAudio.pause();
      this.currentAudio = null;
    }
    window.speechSynthesis?.cancel();
  }
  getMarkdown(content: string): string {
    if (!content) return '';
    return marked(content) as string;
  }
  openLesson(content: EducationContent) {
    this.syncProgress();
    this.followUpDoubt = '';
    this.followUpResponse = '';
    this.rewardsClaimed = false;
    this.quizStep = 'LESSON';
    if (!content.content) {
      this.generateStageContent(content);
      return;
    }
    this.selectedTopic = content;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  generateStageContent(content: EducationContent) {
    this.loading = true;
    this.isGenerating = true;
    const langLabel = this.languages.find(l => l.code === this.selectedLanguageCode)?.label || 'English';
    this.api.generateAiLesson(content.title, langLabel).subscribe({
      next: (data) => {
        content.content = data.content;
        content.title = data.title;
        this.selectedTopic = content;
        this.loading = false;
        this.isGenerating = false;
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        console.error('Stage Generation failed', err);
        this.showMessage('The AI Oracle is recovering. Please click again.', 'error');
        this.loading = false;
        this.isGenerating = false;
      }
    });
  }
  closeLesson() {
    this.stopAudio();
    this.selectedTopic = null;
    this.showAiQuiz = false;
    this.showScorecard = false;
    this.quizStep = 'LESSON';
    this.aiQuizQuestions = [];
    this.userAnswers = {};
    this.rewardsClaimed = false;
    this.detailedReport = [];
  }
  showMessage(msg: string, type: 'success' | 'error' | 'info' = 'info') {
    this.feedbackMessage = msg;
    this.feedbackType = type;
    setTimeout(() => this.feedbackMessage = '', 6000);
  }
  async translateText(text: string, targetLang: string): Promise<string> {
    if (targetLang === 'en') return text;
    try {
      const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=${targetLang}&dt=t&q=${encodeURIComponent(text)}`;
      const res = await fetch(url);
      const data = await res.json();
      return data[0]?.map((item: any) => item[0])?.join('') || text;
    } catch (err) {
      console.error('Translation error:', err);
      return text;
    }
  }
  isTopicCompleted(title: string): boolean {
    if (!title) return false;
    return this.completedTopicTitles.some(t => t?.trim() === title.trim());
  }
  isLocked(index: number): boolean {
    if (index === 0) return false;
    const prevTopic = this.curriculum[index - 1];
    return !this.isTopicCompleted(prevTopic.title);
  }
  isNextToComplete(index: number): boolean {
    if (this.isTopicCompleted(this.curriculum[index].title)) return false;
    return index === 0 || this.isTopicCompleted(this.curriculum[index - 1].title);
  }
  get progressPercentage(): number {
    if (this.curriculum.length === 0) return 0;
    return Math.round((this.completedTopicTitles.length / this.curriculum.length) * 100);
  }
  loadContent() {
    this.contents = this.curriculum.map((item, idx) => ({
      ...item,
      id: idx + 1,
      language: this.selectedLanguageCode,
      content: ''
    }));
  }
  async listenToLesson(textContent: string) {
    if (this.isPlaying) {
      this.stopAudio();
      return;
    }
    this.stopAudio();
    const voices = window.speechSynthesis?.getVoices() || [];
    const hasNativeVoice = voices.some(v => v.lang.startsWith(this.selectedLanguageCode));
    if (window.speechSynthesis && (hasNativeVoice || this.selectedLanguageCode === 'en' || this.selectedLanguageCode === 'es')) {
      const speech = new SpeechSynthesisUtterance(textContent);
      const langMap: { [key: string]: string } = {
        'en': 'en-US',
        'hi': 'hi-IN',
        'te': 'te-IN',
        'es': 'es-ES',
        'kn': 'kn-IN'
      };
      const targetLang = langMap[this.selectedLanguageCode] || 'en-US';
      speech.lang = targetLang;
      const exactVoice = voices.find(v => v.lang === targetLang || v.lang.startsWith(this.selectedLanguageCode));
      if (exactVoice) {
         speech.voice = exactVoice;
      }
      speech.onend = () => this.isPlaying = false;
      window.speechSynthesis.speak(speech);
      this.isPlaying = true;
      return;
    }
    try {
      const chunks: string[] = [];
      const splitByNewlines = textContent.split('\n');
      for (const block of splitByNewlines) {
        if (!block.trim()) continue;
        const sentences = block.replace(/([.?!।])\s+/g, "$1|").split("|");
        for (let s of sentences) {
           s = s.trim();
           if (s.length > 200) s = s.substring(0, 197) + '...';
           if (s) chunks.push(s);
        }
      }
      const playSequentially = async (index: number) => {
        if (index >= chunks.length) return;
        const chunk = chunks[index];
        try {
          const blob = await new Promise<Blob>((resolve, reject) => {
             this.api.getTtsAudio(chunk, this.selectedLanguageCode).subscribe({
                next: (b: Blob) => resolve(b),
                error: (e: any) => reject(e)
             });
          });
          if (!blob) {
            playSequentially(index + 1);
            return;
          }
          const objectUrl = URL.createObjectURL(blob);
          this.currentAudio = new Audio(objectUrl);
          this.currentAudio.onended = () => {
            URL.revokeObjectURL(objectUrl);
            playSequentially(index + 1);
          };
          this.currentAudio.onerror = (err) => {
            console.error("Audio chunk strictly failed", err);
            URL.revokeObjectURL(objectUrl);
            playSequentially(index + 1);
          };
          await this.currentAudio.play();
          this.isPlaying = true;
        } catch (err) {
            console.error("Audio fetch or play interrupted", err);
            console.warn("Attempting naive string fallback for chunk due to fetch failure");
            const speech = new SpeechSynthesisUtterance(chunk);
            speech.lang = this.selectedLanguageCode === 'te' ? 'te-IN' : (this.selectedLanguageCode === 'kn' ? 'kn-IN' : 'en-US');
            window.speechSynthesis?.speak(speech);
            this.isPlaying = true;
            speech.onend = () => {
                this.isPlaying = false;
                playSequentially(index + 1);
            };
        }
      };
      await playSequentially(0);
    } catch (e) {
      console.error("Audio fallback playback error", e);
      alert("TTS audio playback failed in this browser for language: " + this.selectedLanguageCode);
      this.isPlaying = false;
    }
  }
  askOracle() {
    if (!this.searchTopic.trim()) return;
    this.loading = true;
    this.isGenerating = true;
    this.showAiQuiz = false;
    this.api.generateAiLesson(this.searchTopic, this.selectedLanguageCode).subscribe({
      next: (data) => {
        const newLesson = { ...data, id: Date.now() };
        this.contents.unshift(newLesson);
        this.selectedTopic = newLesson;
        this.loading = false;
        this.isGenerating = false;
        this.searchTopic = '';
      },
      error: (err) => {
        console.error('AI Generation failed', err);
        this.showMessage('The Insurance Oracle is currently busy. Please try again soon.', 'error');
        this.loading = false;
        this.isGenerating = false;
      }
    });
  }
  askFollowUp() {
    if (!this.selectedTopic || !this.followUpDoubt.trim()) return;
    this.isAskingFollowUp = true;
    const langLabel = this.languages.find(l => l.code === this.selectedLanguageCode)?.label || 'English';
    this.api.askOracleFollowUp(this.selectedTopic.content, this.followUpDoubt, langLabel).subscribe({
      next: (res) => {
        this.followUpResponse = res;
        this.isAskingFollowUp = false;
      },
      error: (err) => {
        console.error('Follow-up failed', err);
        this.isAskingFollowUp = false;
      }
    });
  }
  generateAiQuiz() {
    if (!this.selectedTopic) return;
    this.loading = true;
    this.userAnswers = {};
    this.showScorecard = false;
    this.api.generateAiQuiz(this.selectedTopic.content, this.selectedLanguageCode).subscribe({
      next: (questions) => {
        this.aiQuizQuestions = questions;
        this.quizStep = 'QUIZ';
        this.showAiQuiz = true;
        this.loading = false;
      },
      error: (err) => {
        console.error('AI Quiz Generation failed', err);
        this.showMessage('Could not calibrate assessment. Please review the material again.', 'error');
        this.loading = false;
      }
    });
  }
  startPreQuiz() {
    this.quizStep = 'PRE_QUIZ';
  }
  selectOption(qIndex: number, oIndex: number) {
    this.userAnswers[qIndex] = oIndex;
  }
  get canSubmitQuiz() {
    return this.aiQuizQuestions.length > 0 &&
           Object.keys(this.userAnswers).length === this.aiQuizQuestions.length;
  }
  submitAiQuiz() {
    this.correctCount = 0;
    this.aiQuizQuestions.forEach((q, idx) => {
      if (this.userAnswers[idx] === q.correctOptionIndex) this.correctCount++;
    });
    this.lastScore = Math.round((this.correctCount / (this.aiQuizQuestions.length || 1)) * 100);
    
    // Build report for UI
    this.detailedReport = this.aiQuizQuestions.map((q, idx) => ({
      questionText: q.text,
      selectedAnswer: q.options[this.userAnswers[idx]],
      correctAnswer: q.options[q.correctOptionIndex],
      explanation: q.explanation || 'Analyzed as correct by Oracle.',
      isCorrect: this.userAnswers[idx] === q.correctOptionIndex
    }));

    this.quizStep = 'RESULT';
    this.showScorecard = true;
    this.showAiQuiz = false;
  }
  claimRewards() {
    const user = this.auth.getUser();
    if (!user || !this.selectedTopic) return;
    this.loading = true;

    // Create a detailed question report
    const report = this.aiQuizQuestions.map((q, idx) => ({
      questionText: q.text,
      selectedAnswer: q.options[this.userAnswers[idx]],
      correctAnswer: q.options[q.correctOptionIndex],
      explanation: q.explanation || 'No detailed explanation provided.',
      isCorrect: this.userAnswers[idx] === q.correctOptionIndex
    }));
    const reportJson = JSON.stringify(report);

    this.api.completeAcademyLesson(
      user.userId,
      this.selectedTopic.title,
      this.correctCount,
      this.aiQuizQuestions.length || 5,
      reportJson
    ).subscribe({
      next: (updatedUser) => {
        this.auth.updateUserGamification(
          updatedUser.userPoints,
          updatedUser.totalQuizzesTaken,
          updatedUser.currentStreak,
          updatedUser.experiencePoints || 0,
          updatedUser.rank || 'NOVICE_GUARDIAN',
          updatedUser.fortressIntegrity || 50
        );
        this.showMessage(`Digital Fortress Strengthened! Points secured.`, 'success');
        this.syncProgress(); // Immediately update the local completion list
        this.loading = false;
        this.rewardsClaimed = true;
      },
      error: (err) => {
        console.error('Failed to claim points', err);
        this.showMessage('Oracle connection interrupted. Please RE-TRY securing your rewards.', 'error');
        this.loading = false;
      }
    });
  }
  retryQuiz() {
    this.showScorecard = false;
    this.showAiQuiz = false;
    this.userAnswers = {};
  }
}