import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { Family, WeekDay, WeekType, TimeSlot, AbsenceDays, Requirement } from '../../../../modules/openapi';
import { FamilyService } from '../../services/family.service';

@Component({
  selector: 'app-family-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './family-list.component.html',
  styleUrl: './family-list.component.css'
})
export class FamilyListComponent implements OnInit {
  families$!: Observable<Family[]>;

  constructor(private familyService: FamilyService) {}

  ngOnInit(): void {
    this.loadFamilies();
  }

  loadFamilies(): void {
    this.families$ = this.familyService.getFamilies();
  }

  deleteFamily(family: Family): void {
    if (family.id && confirm(`Êtes-vous sûr de vouloir supprimer la famille "${family.name}" ?`)) {
      this.familyService.deleteFamily(family.id).subscribe({
        next: () => {
          this.loadFamilies();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          alert('Erreur lors de la suppression de la famille');
        }
      });
    }
  }

  getShortDay(weekDay?: WeekDay): string {
    switch (weekDay) {
      case WeekDay.Monday: return 'Lun';
      case WeekDay.Tuesday: return 'Mar';
      case WeekDay.Thursday: return 'Jeu';
      case WeekDay.Friday: return 'Ven';
      default: return '';
    }
  }

  getShortWeekType(weekType?: WeekType): string {
    switch (weekType) {
      case WeekType.Even: return 'PAIR';
      case WeekType.Odd: return 'IMPAIR';
      default: return '';
    }
  }

  getShortTimeSlot(timeSlot?: TimeSlot): string {
    switch (timeSlot) {
      case TimeSlot.Morning: return 'AM';
      case TimeSlot.Evening: return 'PM';
      default: return '';
    }
  }
}
