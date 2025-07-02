import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Family, FullSchedule, TimeSlot, Trip, WeekDay, WeekType } from '../../../../../../modules/openapi';
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
}