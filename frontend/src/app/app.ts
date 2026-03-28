
import { Component } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';
import { filter } from 'rxjs';
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    @if (showNavbar) {
      <app-navbar></app-navbar>
    }
    <main class="main-content">
      <router-outlet></router-outlet>
    </main>
    `,
  styles: [`.main-content { min-height: calc(100vh - 64px); }`]
})
export class App {
  showNavbar = false;
  constructor(
    public auth: AuthService,
    private router: Router,
    private notifications: NotificationService
  ) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      const hiddenRoutes = ['/', '/about', '/login', '/register', '/forgot-password', '/reset-password'];
      this.showNavbar = !hiddenRoutes.includes(event.url);
      const user = this.auth.getUser();
      if (user && user.userId) {
        this.notifications.subscribe(user.userId);
      }
    });
  }
}