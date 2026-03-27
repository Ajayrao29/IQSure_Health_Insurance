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
  
  // Track currently playing fallback audio so we can stop it
  currentAudio: HTMLAudioElement | null = null;
  isPlaying = false;

  // AI-First State
  searchTopic = '';
  isGenerating = false;
  aiQuizQuestions: any[] = [];
  showAiQuiz = false;
  userAnswers: { [qIndex: number]: number } = {};
  quizPassingScore = 80;
  showScorecard = false;
  lastScore = 0;
  correctCount = 0;
  
  // Feedback system
  feedbackMessage = '';
  feedbackType: 'success' | 'error' | 'info' = 'info';
  
  // Follow-up interaction
  followUpDoubt = '';
  followUpResponse = '';
  isAskingFollowUp = false;

  constructor(private api: ApiService, public auth: AuthService) {}

  ngOnInit() {
    this.loadContent();
    // Pre-load voices for Chrome
    if (window.speechSynthesis) {
        window.speechSynthesis.getVoices();
        window.speechSynthesis.onvoiceschanged = () => {
            window.speechSynthesis.getVoices();
        };
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
    this.followUpDoubt = '';
    this.followUpResponse = '';
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
        content.title = data.title; // Update if AI gives better title
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
    this.aiQuizQuestions = [];
    this.userAnswers = {};
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
      console.error('Translation API error:', err);
      return text;   
    }
  }

  curriculum = [
    { id: 1, title: 'Health Insurance Foundations: Why do I need it?', topic: 'Level 1: Ground Zero', content: '' },
    { id: 2, title: 'The Cost Equation: Premiums, Deductibles & Co-pay', topic: 'Level 2: Financials', content: '' },
    { id: 3, title: 'Hospital Networks: The Secret to Cashless Claims', topic: 'Level 3: Access', content: '' },
    { id: 4, title: 'Comparison Master: Critical Illness vs Comprehensive', topic: 'Level 4: Analysis', content: '' },
    { id: 5, title: 'The Fine Print: Waiting Periods & Pre-existing T&Cs', topic: 'Level 5: Expertise', content: '' },
    { id: 6, title: 'Financial Fortress: Advanced Tax & Coverage Strategy', topic: 'Level 6: Mastery', content: '' }
  ];

  loadContent() {
    // Initial content setup from the curriculum
    this.contents = this.curriculum.map(item => ({
      ...item,
      language: this.selectedLanguageCode
    }));
    
    // If language is change, we'll want to translate titles/topics eventually
    // For now, names are simple enough for universal understanding
  }

  async listenToLesson(textContent: string) {
    if (this.isPlaying) {
      this.stopAudio();
      return;
    }
    
    // Stop anything else just in case
    this.stopAudio();

    const voices = window.speechSynthesis?.getVoices() || [];
    const hasNativeVoice = voices.some(v => v.lang.startsWith(this.selectedLanguageCode));

    // 2) If it's English or OS explicitly has a voice package for this language, use SpeechSynthesis
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

    // 3) Windows Desktop often natively lacks Te/Kn voices. Use pure HTTP audio buffer payload stream as fallback.
    try {
      // Free TTS restricts char limits per request to 200 items. Break down chunks properly.
      const chunks: string[] = [];
      const splitByNewlines = textContent.split('\n');
      for (const block of splitByNewlines) {
        if (!block.trim()) continue;
        const sentences = block.replace(/([.?!।])\s+/g, "$1|").split("|");
        for (let s of sentences) {
           s = s.trim();
           if (s.length > 200) s = s.substring(0, 197) + '...'; // Truncate safety buffer
           if (s) chunks.push(s);
        }
      }

      const playSequentially = async (index: number) => {
        if (index >= chunks.length) return;
        
        const chunk = chunks[index];
        // Stream text through our secure backend proxy to bypass browser restrictions
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
            // Ultimate fallback to literal string reading if completely offline or backend fails
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

  /* ───── AI-FIRST ACADEMY LOGIC ───── */

  /**
   * Generates a custom lesson using the AI Oracle
   */
  askOracle() {
    if (!this.searchTopic.trim()) return;
    
    this.loading = true;
    this.isGenerating = true;
    this.showAiQuiz = false;
    
    this.api.generateAiLesson(this.searchTopic, this.selectedLanguageCode).subscribe({
      next: (data) => {
        // Add the new AI-generated lesson to the journey path locally
        const newLesson = { ...data, id: Date.now() };
        this.contents.unshift(newLesson);
        this.selectedTopic = newLesson;
        this.loading = false;
        this.isGenerating = false;
        this.searchTopic = ''; // Clear search
      },
      error: (err) => {
        console.error('AI Generation failed', err);
        this.showMessage('The Insurance Oracle is currently busy. Please try again soon.', 'error');
        this.loading = false;
        this.isGenerating = false;
      }
    });
  }

  /**
   * Asks a follow-up doubt about the current lesson
   */
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

  /**
   * Generates a quiz based on the AI-generated lesson context
   */
  generateAiQuiz() {
    if (!this.selectedTopic) return;
    
    this.loading = true;
    this.userAnswers = {}; // Reset previous answers
    this.showScorecard = false;
    this.api.generateAiQuiz(this.selectedTopic.content, this.selectedLanguageCode).subscribe({
      next: (questions) => {
        this.aiQuizQuestions = questions;
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

  selectOption(qIndex: number, oIndex: number) {
    this.userAnswers[qIndex] = oIndex;
  }

  get canSubmitQuiz() {
    return this.aiQuizQuestions.length > 0 && 
           Object.keys(this.userAnswers).length === this.aiQuizQuestions.length;
  }

  /**
   * Submits the assessment and claims rewards
   */
  submitAiQuiz() {
    const user = this.auth.getUser();
    if (!user) return;

    // Calculate score
    this.correctCount = 0;
    this.aiQuizQuestions.forEach((q, idx) => {
      if (this.userAnswers[idx] === q.correctOptionIndex) this.correctCount++;
    });

    this.lastScore = Math.round((this.correctCount / this.aiQuizQuestions.length) * 100);
    this.showScorecard = true;
    this.showAiQuiz = false; // Transition to scorecard
  }

  claimRewards() {
    const user = this.auth.getUser();
    if (!user || !this.selectedTopic) return;

    this.loading = true;
    this.api.completeAcademyLesson(
      user.userId, 
      this.selectedTopic.title, 
      this.correctCount, 
      this.aiQuizQuestions.length || 5
    ).subscribe({
      next: (updatedUser) => {
        this.auth.updateUserPoints(updatedUser.userPoints);
        this.auth.updateUserStats(updatedUser.totalQuizzesTaken, updatedUser.currentStreak);
        this.showMessage(`Digital Fortress Strengthened! Points secured.`, 'success');
        this.loading = false;
        this.closeLesson();
      },
      error: (err) => {
        console.error('Failed to claim points', err);
        this.loading = false;
        this.closeLesson();
      }
    });
  }

  retryQuiz() {
    this.showScorecard = false;
    this.showAiQuiz = false; // Go back to lesson
    this.userAnswers = {};
  }
}
