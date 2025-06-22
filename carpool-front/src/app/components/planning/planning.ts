import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import {
  FamilyResourceService,
  ChildResourceService,
  RequirementResourceService,
  Family,
  Child,
  Requirement,
  TimeSlot,
  WeekDay,
  WeekType
} from '../../modules/openapi';
import { forkJoin, map, Observable } from 'rxjs';

interface PlanningSlot {
  weekDay: WeekDay;
  timeSlot: TimeSlot;
  weekType: WeekType;
  availableFamilies: FamilyWithChildren[];
  assignedFamily?: FamilyWithChildren;
}

interface FamilyWithChildren extends Family {
  children: Child[];
  requirements: Requirement[];
  isAvailable?: boolean;
}

@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './planning.html',
  styleUrl: './planning.css'
})
export class Planning implements OnInit {
  private familyService = inject(FamilyResourceService);
  private childService = inject(ChildResourceService);
  private requirementService = inject(RequirementResourceService);

  families: FamilyWithChildren[] = [];
  planningSlots: PlanningSlot[] = [];
  loading = true;
  error: string | null = null;

  weekDays = [
    { value: WeekDay.Monday, label: 'Lundi' },
    { value: WeekDay.Tuesday, label: 'Mardi' },
    { value: WeekDay.Thursday, label: 'Jeudi' },
    { value: WeekDay.Friday, label: 'Vendredi' }
  ];

  timeSlots = [
    { value: TimeSlot.Morning, label: 'Matin' },
    { value: TimeSlot.Evening, label: 'Soir' }
  ];

  weekTypes = [
    { value: WeekType.Even, label: 'Semaine paire' },
    { value: WeekType.Odd, label: 'Semaine impaire' }
  ];

  ngOnInit() {
    this.loadPlanningData();
  }

  loadPlanningData() {
    this.loading = true;
    this.error = null;

    this.familyService.familyGet().pipe(
      map(families => families.map(family => ({
        ...family,
        children: [],
        requirements: []
      } as FamilyWithChildren)))
    ).subscribe({
      next: (families) => {
        // Charger les enfants et indisponibilités pour chaque famille
        const familyDetailsRequests = families.map(family =>
          forkJoin({
            family: [family],
            children: this.childService.childGet(family.id),
            requirements: this.requirementService.requirementGet(family.id)
          }).pipe(
            map(({ family, children, requirements }) => ({
              name: family.name,
              id: family.id,
              children: children || [],
              requirements: requirements || []
            } as FamilyWithChildren))
          )
        );

        if (familyDetailsRequests.length > 0) {
          forkJoin(familyDetailsRequests).subscribe({
            next: (familiesWithDetails) => {
              this.families = familiesWithDetails;
              this.generatePlanningSlots();
              this.loading = false;
            },
            error: (error) => {
              console.error('Erreur lors du chargement des détails des familles:', error);
              this.error = 'Erreur lors du chargement des données';
              this.loading = false;
            }
          });
        } else {
          this.families = [];
          this.generatePlanningSlots();
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

  generatePlanningSlots() {
    this.planningSlots = [];

    // Générer tous les créneaux possibles
    for (const weekType of this.weekTypes) {
      for (const weekDay of this.weekDays) {
        for (const timeSlot of this.timeSlots) {
          const availableFamilies = this.getAvailableFamilies(weekDay.value, timeSlot.value, weekType.value);

          this.planningSlots.push({
            weekDay: weekDay.value,
            timeSlot: timeSlot.value,
            weekType: weekType.value,
            availableFamilies,
            assignedFamily: this.getOptimalFamily(availableFamilies)
          });
        }
      }
    }
  }

  getAvailableFamilies(weekDay: WeekDay, timeSlot: TimeSlot, weekType: WeekType): FamilyWithChildren[] {
    return this.families.filter(family => {
      // Vérifier si la famille a une indisponibilité pour ce créneau
      const hasConflict = family.requirements.some(req =>
        req.weekDay === weekDay &&
        req.timeSlot === timeSlot &&
        req.weekType === weekType
      );

      return !hasConflict && family.children.length > 0;
    }).map(family => ({
      ...family,
      isAvailable: true
    }));
  }

  getOptimalFamily(availableFamilies: FamilyWithChildren[]): FamilyWithChildren | undefined {
    if (availableFamilies.length === 0) return undefined;

    // Logique simple : prendre la famille avec la plus grande capacité
    // Dans un vrai système, on pourrait avoir une logique plus complexe
    return availableFamilies.reduce((best, current) =>
      (current.carCapacity || 0) > (best.carCapacity || 0) ? current : best
    );
  }

  assignFamily(slot: PlanningSlot, family: FamilyWithChildren) {
    slot.assignedFamily = family;
  }

  unassignFamily(slot: PlanningSlot) {
    slot.assignedFamily = undefined;
  }

  getWeekDayLabel(weekDay: WeekDay): string {
    return this.weekDays.find(d => d.value === weekDay)?.label || weekDay;
  }

  getTimeSlotLabel(timeSlot: TimeSlot): string {
    return this.timeSlots.find(t => t.value === timeSlot)?.label || timeSlot;
  }

  getWeekTypeLabel(weekType: WeekType): string {
    return this.weekTypes.find(w => w.value === weekType)?.label || weekType;
  }

  getSlotKey(slot: PlanningSlot): string {
    return `${slot.weekType}-${slot.weekDay}-${slot.timeSlot}`;
  }

  exportPlanning() {
    // Logique d'export (CSV, PDF, etc.)
    const planningData = this.planningSlots.map(slot => ({
      'Type de semaine': this.getWeekTypeLabel(slot.weekType),
      'Jour': this.getWeekDayLabel(slot.weekDay),
      'Créneau': this.getTimeSlotLabel(slot.timeSlot),
      'Famille assignée': slot.assignedFamily?.name || 'Non assigné',
      'Capacité': slot.assignedFamily?.carCapacity || 0,
      'Enfants': slot.assignedFamily?.children.map(c => c.name).join(', ') || ''
    }));

    console.log('Export planning:', planningData);
    alert('Fonctionnalité d\'export à implémenter');
  }
}
