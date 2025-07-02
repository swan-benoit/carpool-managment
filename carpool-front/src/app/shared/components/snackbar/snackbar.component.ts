import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface SnackbarData {
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration?: number;
}

@Component({
  selector: 'app-snackbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './snackbar.component.html',
  styleUrl: './snackbar.component.css'
})
export class SnackbarComponent implements OnInit {
  @Input() data: SnackbarData | null = null;
  @Input() show: boolean = false;

  ngOnInit(): void {
    if (this.show && this.data?.duration) {
      setTimeout(() => {
        this.show = false;
      }, this.data.duration);
    }
  }

  getIcon(): string {
    switch (this.data?.type) {
      case 'success': return '✅';
      case 'error': return '❌';
      case 'warning': return '⚠️';
      case 'info': return 'ℹ️';
      default: return '✅';
    }
  }
}