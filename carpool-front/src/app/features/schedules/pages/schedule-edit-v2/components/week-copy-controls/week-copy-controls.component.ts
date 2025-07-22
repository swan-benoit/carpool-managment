import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FullSchedule, WeekType } from '../../../../../../modules/openapi';

export interface WeekCopyEvent {
  from: WeekType;
  to: WeekType;
}

@Component({
  selector: 'app-week-copy-controls',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './week-copy-controls.component.html',
  styleUrl: './week-copy-controls.component.css'
})
export class WeekCopyControlsComponent {
  @Input() schedule: FullSchedule | null = null;
  @Output() copyWeek = new EventEmitter<WeekCopyEvent>();

  readonly WeekType = WeekType;

  onCopyEvenToOdd(): void {
    if (this.confirmCopy('paire', 'impaire')) {
      this.copyWeek.emit({
        from: WeekType.Even,
        to: WeekType.Odd
      });
    }
  }

  onCopyOddToEven(): void {
    if (this.confirmCopy('impaire', 'paire')) {
      this.copyWeek.emit({
        from: WeekType.Odd,
        to: WeekType.Even
      });
    }
  }

  private confirmCopy(fromLabel: string, toLabel: string): boolean {
    const message = `⚠️ ATTENTION : Cette action va remplacer TOUS les trajets de la semaine ${toLabel} par ceux de la semaine ${fromLabel}.\n\nTous les trajets existants de la semaine ${toLabel} seront perdus.\n\nÊtes-vous sûr de vouloir continuer ?`;
    return confirm(message);
  }

  getEvenTripsCount(): number {
    return this.schedule?.evenSchedule?.trips?.length || 0;
  }

  getOddTripsCount(): number {
    return this.schedule?.oddSchedule?.trips?.length || 0;
  }

  hasEvenTrips(): boolean {
    return this.getEvenTripsCount() > 0;
  }

  hasOddTrips(): boolean {
    return this.getOddTripsCount() > 0;
  }
}