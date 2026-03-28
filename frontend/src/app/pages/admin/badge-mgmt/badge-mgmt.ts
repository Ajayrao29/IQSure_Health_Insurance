// Angular component for the badge-mgmt page
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../services/api.service';
import { Badge } from '../../../models/models';
@Component({
  selector: 'app-badge-mgmt',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './badge-mgmt.html',
  styleUrls: ['./badge-mgmt.scss']
})
export class BadgeMgmtComponent implements OnInit {
  badges: Badge[] = [];
  loading = true;
  showForm = false;
  editingBadge: Badge | null = null;
  form: Partial<Badge> = this.getEmptyForm();
  constructor(private api: ApiService) {}
  ngOnInit(): void {
    this.loadBadges();
  }
  loadBadges(): void {
    this.loading = true;
    this.api.getAllBadges().subscribe({
      next: (data) => {
        this.badges = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load badges:', err);
        this.loading = false;
      }
    });
  }
  openCreate(): void {
    this.editingBadge = null;
    this.form = this.getEmptyForm();
    this.showForm = true;
  }
  openEdit(badge: Badge): void {
    this.editingBadge = badge;
    this.form = { ...badge };
    this.showForm = true;
  }
  save(): void {
    this.loading = true;
    const request = this.editingBadge
      ? this.api.updateBadge(this.editingBadge.badgeId, this.form)
      : this.api.createBadge(this.form);
    request.subscribe({
      next: () => {
        this.showForm = false;
        this.loadBadges();
      },
      error: (err) => {
        console.error('Failed to save badge:', err);
        this.loading = false;
        alert('Could not save badge. Check console for details.');
      }
    });
  }
  delete(id: number): void {
    if (!confirm('Are you sure you want to delete this badge? This cannot be undone.')) {
      return;
    }
    this.api.deleteBadge(id).subscribe({
      next: () => this.loadBadges(),
      error: (err) => console.error('Failed to delete badge:', err)
    });
  }
  getBadgeIcon(index: number): string {
    const icons = ['\u2606', '\u2736', '\u2666', '\u265B', '\u2605', '\u2764'];
    return icons[index % icons.length];
  }
  getBadgeLevel(index: number): string {
    const levels = ['bronze', 'silver', 'gold', 'diamond', 'platinum', 'ruby'];
    return levels[index % levels.length];
  }
  private getEmptyForm(): Partial<Badge> {
    return { name: '', description: '', reqPoints: 0, icon: '' };
  }
}