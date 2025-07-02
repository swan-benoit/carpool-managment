import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Family, FullSchedule, TimeSlot, Trip, WeekDay, WeekType, Child } from '../../../../../../modules/openapi';
import { TripModalData } from '../../schedule-edit-v2.component';

interface WeekDayOption {
  value: WeekDay;
  label: string;
}

interface TimeSlotOption {
  value: TimeSlot;
  label: string;
}

@Component({
  selector: 'app-schedule-grid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './schedule-grid.component.html',
  styleUrl: './schedule-grid.component.css'
})
export class ScheduleGridComponent {
  @Input() schedule: FullSchedule | null = null;
  @Input() families: Family[] = [];
  @Input() selectedWeekType: WeekType = WeekType.Even;

  @Output() openTripModal = new EventEmitter<TripModalData>();
  @Output() tripDeleted = new EventEmitter<void>();

  readonly weekDays: WeekDayOption[] = [
    { value: WeekDay.Monday, label: 'Lundi' },
    { value: WeekDay.Tuesday, label: 'Mardi' },
    { value: WeekDay.Thursday, label: 'Jeudi' },
    { value: WeekDay.Friday, label: 'Vendredi' }
  ];

  readonly timeSlots: TimeSlotOption[] = [
    { value: TimeSlot.Morning, label: 'Matin' },
    { value: TimeSlot.Evening, label: 'Soir' }
  ];

  getTripsForSlot(weekDay: WeekDay, timeSlot: TimeSlot): Trip[] {
    const currentSchedule = this.selectedWeekType === WeekType.Even 
      ? this.schedule?.evenSchedule 
      : this.schedule?.oddSchedule;
    
    return currentSchedule?.trips?.filter(trip => 
      trip.weekDay === weekDay && trip.timeSlot === timeSlot
    ) || [];
  }

  onOpenTripModal(weekDay: WeekDay, timeSlot: TimeSlot, tripIndex?: number): void {
    this.openTripModal.emit({
      weekDay,
      timeSlot,
      weekType: this.selectedWeekType,
      tripIndex
    });
  }

  onDeleteTrip(weekDay: WeekDay, timeSlot: TimeSlot, tripIndex: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce trajet ?')) {
      const currentSchedule = this.selectedWeekType === WeekType.Even 
        ? this.schedule?.evenSchedule 
        : this.schedule?.oddSchedule;
      
      if (currentSchedule?.trips) {
        const trips = this.getTripsForSlot(weekDay, timeSlot);
        const tripToDelete = trips[tripIndex];

        currentSchedule.trips = currentSchedule.trips.filter(trip =>
          !(trip.weekDay === weekDay &&
            trip.timeSlot === timeSlot &&
            trip.id === tripToDelete.id)
        );

        this.tripDeleted.emit();
      }
    }
  }

  canAddMoreTrips(weekDay: WeekDay, timeSlot: TimeSlot): boolean {
    const existingTrips = this.getTripsForSlot(weekDay, timeSlot);
    const usedFamilyIds = existingTrips.map(trip => trip.driver?.id);
    const availableFamilies = this.families.filter(family => !usedFamilyIds.includes(family.id));
    return availableFamilies.length > 0;
  }

  /**
   * Vérifie si un trajet est plein (capacité maximale atteinte)
   */
  isTripFull(trip: Trip): boolean {
    if (!trip.driver || !trip.children) return false;
    return trip.children.length >= trip.driver.carCapacity!;
  }

  /**
   * Vérifie si un trajet est proche de la capacité maximale (80% ou plus)
   */
  isNearCapacity(trip: Trip): boolean {
    if (!trip.driver || !trip.children) return false;
    const capacity = trip.driver.carCapacity!;
    const occupancy = trip.children.length;
    return occupancy >= Math.ceil(capacity * 0.8) && occupancy < capacity;
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Obtient tous les enfants qui doivent être transportés pour un créneau donné
   * Exclut automatiquement les enfants absents
   */
  private getAllChildrenForSlot(weekDay: WeekDay, timeSlot: TimeSlot): Child[] {
    // Récupérer tous les enfants de toutes les familles
    const allChildren = this.families.flatMap(family => family.children || []);
    
    // ✅ MODIFICATION : Filtrer les enfants qui ne sont pas absents pour ce créneau
    return allChildren.filter(child => {
      if (!child.absenceDays) return true;
      
      // Vérifier si l'enfant est absent pour ce jour et ce type de semaine
      const isAbsent = child.absenceDays.some(absence => 
        absence.weekDay === weekDay && 
        absence.weekType === this.selectedWeekType
      );
      
      return !isAbsent; // ✅ Retourner seulement les enfants NON absents
    });
  }

  /**
   * Obtient le nombre total d'enfants qui doivent être transportés pour un créneau
   * ✅ MODIFICATION : Exclut automatiquement les enfants absents
   */
  getTotalChildrenForSlot(weekDay: WeekDay, timeSlot: TimeSlot): number {
    return this.getAllChildrenForSlot(weekDay, timeSlot).length;
  }

  /**
   * Obtient le nombre d'enfants actuellement transportés pour un créneau
   */
  getTransportedChildrenCount(weekDay: WeekDay, timeSlot: TimeSlot): number {
    const trips = this.getTripsForSlot(weekDay, timeSlot);
    const transportedChildren = new Set<number>();
    
    trips.forEach(trip => {
      trip.children?.forEach(child => {
        if (child.id) {
          transportedChildren.add(child.id);
        }
      });
    });
    
    return transportedChildren.size;
  }

  /**
   * Vérifie si tous les enfants sont transportés pour un créneau donné
   * ✅ MODIFICATION : Prend en compte seulement les enfants non absents
   */
  isSlotComplete(weekDay: WeekDay, timeSlot: TimeSlot): boolean {
    const totalChildren = this.getTotalChildrenForSlot(weekDay, timeSlot);
    const transportedChildren = this.getTransportedChildrenCount(weekDay, timeSlot);
    
    return totalChildren > 0 && transportedChildren >= totalChildren;
  }

  /**
   * Obtient la liste des enfants non transportés pour un créneau
   * ✅ MODIFICATION : Exclut automatiquement les enfants absents
   */
  getMissingChildren(weekDay: WeekDay, timeSlot: TimeSlot): Child[] {
    const allChildren = this.getAllChildrenForSlot(weekDay, timeSlot); // ✅ Déjà filtrés (non absents)
    const trips = this.getTripsForSlot(weekDay, timeSlot);
    const transportedChildrenIds = new Set<number>();
    
    trips.forEach(trip => {
      trip.children?.forEach(child => {
        if (child.id) {
          transportedChildrenIds.add(child.id);
        }
      });
    });
    
    return allChildren.filter(child => 
      child.id && !transportedChildrenIds.has(child.id)
    );
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Obtient le nombre d'enfants absents pour un créneau
   */
  getAbsentChildrenCount(weekDay: WeekDay, timeSlot: TimeSlot): number {
    const allChildren = this.families.flatMap(family => family.children || []);
    
    return allChildren.filter(child => {
      if (!child.absenceDays) return false;
      
      return child.absenceDays.some(absence => 
        absence.weekDay === weekDay && 
        absence.weekType === this.selectedWeekType
      );
    }).length;
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Obtient la liste des enfants absents pour un créneau
   */
  getAbsentChildren(weekDay: WeekDay, timeSlot: TimeSlot): Child[] {
    const allChildren = this.families.flatMap(family => family.children || []);
    
    return allChildren.filter(child => {
      if (!child.absenceDays) return false;
      
      return child.absenceDays.some(absence => 
        absence.weekDay === weekDay && 
        absence.weekType === this.selectedWeekType
      );
    });
  }
}