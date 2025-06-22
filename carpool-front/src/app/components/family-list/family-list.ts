import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FamilyResourceService, ChildResourceService, RequirementResourceService, Family, Child, Requirement } from '../../modules/openapi';
import { Observable, forkJoin, map } from 'rxjs';

interface FamilyWithDetails extends Family {
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
export class FamilyList implements OnInit {
  private familyService = inject(FamilyResourceService);
  private childService = inject(ChildResourceService);
  private requirementService = inject(RequirementResourceService);

  families: FamilyWithDetails[] = [];
  loading = true;
  error: string | null = null;

  ngOnInit() {
    this.loadFamilies();
  }

  loadFamilies() {
    this.loading = true;
    this.error = null;

    this.familyService.familyGet().subscribe({
      next: (families) => {
        // Pour chaque famille, charger ses enfants et exigences
        const familyDetails$ = families.map(family => 
          forkJoin({
            family: [family],
            children: this.childService.childGet(family.id),
            requirements: this.requirementService.requirementGet(family.id)
          }).pipe(
            map(({ family: [f], children, requirements }) => ({
              ...f,
              children,
              requirements
            }))
          )
        );

        if (familyDetails$.length > 0) {
          forkJoin(familyDetails$).subscribe({
            next: (familiesWithDetails) => {
              this.families = familiesWithDetails;
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur lors du chargement des détails des familles:', error);
              this.families = families;
              this.loading = false;
            }
          });
        } else {
          this.families = families;
          this.loading = false;
        }
      },
      error: (error) => {
        console.error('Erreur lors du chargement des familles:', error);
        this.error = 'Erreur lors du chargement des familles';
        this.loading = false;
      }
    });
  }

  deleteFamily(family: Family) {
    if (confirm(`Êtes-vous sûr de vouloir supprimer la famille "${family.name}" ?`)) {
      this.familyService.familyIdDelete(family.id!).subscribe({
        next: () => {
          this.loadFamilies();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          this.error = 'Erreur lors de la suppression de la famille';
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