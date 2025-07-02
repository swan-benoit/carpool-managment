import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WeekType } from '../../../../../../modules/openapi';

@Component({
  selector: 'app-week-selector',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './week-selector.component.html',
  styleUrl: './week-selector.component.css'
})
export class WeekSelectorComponent {
  @Input() selectedWeekType: WeekType = WeekType.Even;
  @Output() weekTypeChange = new EventEmitter<WeekType>();

  readonly WeekType = WeekType;

  onWeekTypeSelect(weekType: WeekType): void {
    this.weekTypeChange.emit(weekType);
  }
}