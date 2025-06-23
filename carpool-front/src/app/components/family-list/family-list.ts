import {Component, inject, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {
  Child,
  Family,
  FamilyResourceService,
  Requirement
} from '../../modules/openapi';
import {map, Observable} from 'rxjs';

interface FamilyWithDetails extends Family{
  id: number;
  name: string;
  carCapacity: number;
  children?: Child[];
  requirements?: Requirement[];
}

@Component({
  selector: 'app-family-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './family-list.html',
  styleUrl: './family-list.css'
})
export class FamilyList {
  private familyService = inject(FamilyResourceService);

  families: Observable<FamilyWithDetails[]> = this.familyService.familyGet().pipe(
    map(families => families.map(family => ({
      id: family.id!, 
      name: family.name ?? '',
      carCapacity: family.carCapacity ?? 0,
      children: family.children || [],
      requirements: Array.from(family.requirements || []),
    } as FamilyWithDetails))));

  deleteFamily(family: Family) {
    if (confirm(`Êtes-vous sûr de vouloir supprimer la famille "${family.name}" ?`)) {
      this.familyService.familyIdDelete(family.id!).subscribe({
        next: () => {
          window.location.reload();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          alert('Erreur lors de la suppression de la famille');
        }
      });
    }
  }

  getTimeSlotLabel(timeSlot: string): string {
    return timeSlot === 'MORNING' ? 'Matin' : 'Soir';
  }

  getWeekDayLabel(weekDay: string): string {
    const days: { [key: string]: string } = {
      'MONDAY': 'Lundi',
      'TUESDAY': 'Mardi',
      'THURSDAY': 'Jeudi',
      'FRIDAY': 'Vendredi'
    };
    return days[weekDay] || weekDay;
  }

  getWeekTypeLabel(weekType: string): string {
    return weekType === 'EVEN' ? 'Semaine paire' : 'Semaine impaire';
  }
}