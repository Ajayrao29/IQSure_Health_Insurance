// Angular component for the users page

import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../../services/api.service';
@Component({ selector: 'app-admin-users', standalone: true, imports: [], templateUrl: './users.html', styleUrls: ['./users.scss'] })
export class AdminUsersComponent implements OnInit {
  users: any[] = []; loading = true;
  constructor(private api: ApiService) {}
  ngOnInit(): void {
    this.api.getAllUsers().subscribe(u => {
      this.users = u.filter(user => user.role !== 'ROLE_ADMIN');
      this.loading = false;
    });
  }
  deleteUser(userId: number): void {
    if (!confirm('Delete this user?')) return;
    this.api.deleteUser(userId).subscribe(() => this.users = this.users.filter(u => u.userId !== userId));
  }
}