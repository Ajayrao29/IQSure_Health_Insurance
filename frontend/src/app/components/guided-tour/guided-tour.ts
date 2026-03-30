import { Component, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { driver } from 'driver.js';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-guided-tour',
  standalone: true,
  imports: [CommonModule],
  template: `<!-- Guided Tour is managed via Driver.js overlay -->`,
  styleUrls: ['./guided-tour.scss']
})
export class GuidedTourComponent implements AfterViewInit {

  constructor(private auth: AuthService) {}

  ngAfterViewInit(): void {
    if (typeof window !== 'undefined') {
      const userId = this.auth.getUserId();
      // Use user-specific key so multiple registrations on the same browser all see the tour
      const tourKey = `iqsure_tour_completed_${userId}`;
      const tourStatus = localStorage.getItem(tourKey);
      
      if (!tourStatus) {
        // Wait briefly for DOM to fully settle
        setTimeout(() => {
          this.startTour(tourKey);
        }, 800);
      }
    }
  }

  startTour(tourKey: string): void {
    const driverObj = driver({
      showProgress: true,
      animate: true,
      allowClose: false,
      progressText: 'STEP {{current}} OF {{total}}',
      nextBtnText: 'Next',
      prevBtnText: 'Back',
      doneBtnText: 'Get Started',
      popoverClass: 'iqsure-driver-theme',
      steps: [
        {
          popover: {
            title: 'Welcome to IQsure! ✨',
            description: 'The award-winning Guardian Ecosystem carefully combines risk education, gamification, and insurance policies to keep you safe and save you money.<br><br>Let us show you around!',
            side: 'top',
            popoverClass: 'iqsure-driver-welcome'
          }
        },
        {
          element: '#tour-step-history',
          popover: {
            title: '1. The Academy 📚',
            description: 'Your journey starts with Risk Literacy. Participate in lessons and quizzes to learn about safety. As you complete missions, your activity and scores will appear here.',
            side: 'top'
          }
        },
        {
          element: '#tour-step-gamification',
          popover: {
            title: '2. Guardian Rank & Integrity 🛡️',
            description: 'Learning earns you Experience Points (XP) and boosts your Digital Fortress Integrity. Higher ranks unlock better rewards and privileges!',
            side: 'left'
          }
        },
        {
          element: '#tour-step-badges',
          popover: {
            title: '3. Unlock Badges 🏆',
            description: 'Specific milestones grant you unique FinTech badges to show off your achievements and increase your overall standing.',
            side: 'top'
          }
        },
        {
          element: '#tour-step-rewards',
          popover: {
            title: '4. Savings & Rewards 💰',
            description: 'The points you earn can be redeemed for cashback or applied as direct discounts to lower your premium payments!',
            side: 'top'
          }
        },
        {
          element: '#tour-step-browse-plans',
          popover: {
            title: '5. Apply for Policies 📝',
            description: 'Ready to use your discounts? Browse smart, responsive insurance plans perfectly tailored to your risk profile.',
            side: 'bottom'
          }
        },
        {
          element: '#tour-step-claims',
          popover: {
            title: '6. Seamless Claims ⚡',
            description: 'If an incident occurs, you can easily file and track your claims right here. Our intelligent system works fast to settle disputes.',
            side: 'bottom'
          }
        }
      ],
      onDestroyStarted: () => {
        if (!driverObj.hasNextStep()) {
          driverObj.destroy();
          localStorage.setItem(tourKey, 'true');
        } else {
          // If they try to exit early by clicking background
          driverObj.destroy();
          localStorage.setItem(tourKey, 'true');
        }
      }
    });

    driverObj.drive();
  }
}
