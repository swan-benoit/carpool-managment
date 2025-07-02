import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-schedule-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './schedule-header.component.html',
  styleUrl: './schedule-header.component.css'
})
export class ScheduleHeaderComponent {
  @Input() scheduleName: string = '';
  @Input() isSaving: boolean = false;
  
  @Output() save = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onSave(): void {
    this.save.emit();
  }

  onCancel(): void {
    this.cancel.emit();
  }
}