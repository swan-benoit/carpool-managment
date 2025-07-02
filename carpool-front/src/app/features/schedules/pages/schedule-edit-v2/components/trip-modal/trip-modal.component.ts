import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Child, Family, FullSchedule, TimeSlot, Trip, WeekDay, WeekType } from '../../../../../../modules/openapi';
import { TripModalData } from '../../schedule-edit-v2.component';

interface WeekDayOption {
  value: WeekDay;
  label: string;
}

interface TimeSlotOption {
  value: TimeSlot;
  label: string;
}

interface ChildSelectionState {
  child: Child;
  isSelected: boolean;
  isDisabled: boolean;
  disabledReason?: string;
}

@Component({
  selector: 'app-trip-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './trip-modal.component.html',
  styleUrl: './trip-modal.component.css'
})
export class TripModalComponent implements OnInit {
  @Input() modalData: TripModalData | null = null;
  @Input() families: Family[] = [];
  @Input() schedule: FullSchedule | null = null;

  @Output() close = new EventEmitter<void>();
  @Output() tripSaved = new EventEmitter<void>();

  tripForm!: FormGroup;
  editingTrip: Trip | null = null;
  childrenSelectionState: ChildSelectionState[] = [];

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

  constructor(private fb: FormBuilder) {
    this.initForm();
  }

  ngOnInit(): void {
    if (this.modalData) {
      this.setupForm();
      this.updateChildrenSelectionState();
    }
  }

  private initForm(): void {
    this.tripForm = this.fb.group({
      weekDay: ['', Validators.required],
      timeSlot: ['', Validators.required],
      driverId: ['', Validators.required],
      childrenIds: [[]]
    });
  }

  private setupForm(): void {
    if (!this.modalData) return;

    if (this.modalData.tripIndex !== undefined) {
      // Mode édition
      const trips = this.getTripsForSlot(this.modalData.weekDay, this.modalData.timeSlot);
      this.editingTrip = trips[this.modalData.tripIndex];

      this.tripForm.patchValue({
        weekDay: this.editingTrip.weekDay,
        timeSlot: this.editingTrip.timeSlot,
        driverId: this.editingTrip.driver?.id,
        childrenIds: this.editingTrip.children?.map(child => child.id) || []
      });
    } else {
      // Mode création
      this.tripForm.patchValue({
        weekDay: this.modalData.weekDay,
        timeSlot: this.modalData.timeSlot,
        driverId: '',
        childrenIds: []
      });
    }
  }

  private getTripsForSlot(weekDay: WeekDay, timeSlot: TimeSlot): Trip[] {
    if (!this.schedule || !this.modalData) return [];

    const currentSchedule = this.modalData.weekType === WeekType.Even 
      ? this.schedule.evenSchedule 
      : this.schedule.oddSchedule;
    
    return currentSchedule?.trips?.filter(trip => 
      trip.weekDay === weekDay && trip.timeSlot === timeSlot
    ) || [];
  }

  private updateChildrenSelectionState(): void {
    const allChildren = this.getAllChildren();
    const selectedChildrenIds = this.tripForm.get('childrenIds')?.value || [];
    const driverId = this.tripForm.get('driverId')?.value;
    const driver = this.families.find(f => f.id === +driverId);

    this.childrenSelectionState = allChildren.map(child => {
      const isSelected = selectedChildrenIds.includes(child.id);
      const isAlreadyAssigned = this.isChildAlreadyAssigned(child);
      const capacityReached = driver && selectedChildrenIds.length >= driver.carCapacity! && !isSelected;

      let isDisabled = false;
      let disabledReason = '';

      if (isAlreadyAssigned && !isSelected) {
        isDisabled = true;
        disabledReason = 'Déjà assigné à un autre trajet';
      } else if (capacityReached) {
        isDisabled = true;
        disabledReason = 'Capacité atteinte';
      }

      return {
        child,
        isSelected,
        isDisabled,
        disabledReason
      };
    });
  }

  private getAllChildren(): Child[] {
    return this.families.flatMap(family => family.children || []);
  }

  private isChildAlreadyAssigned(child: Child): boolean {
    if (!this.modalData) return false;

    const currentTrips = this.getTripsForSlot(this.modalData.weekDay, this.modalData.timeSlot);
    
    return currentTrips.some(trip => 
      trip.id !== this.editingTrip?.id && 
      trip.children?.some(assignedChild => assignedChild.id === child.id)
    );
  }

  onDriverChange(): void {
    const driverId = this.tripForm.get('driverId')?.value;
    if (driverId) {
      const driver = this.families.find(f => f.id === +driverId);
      if (driver) {
        // Pré-sélectionner les enfants de la famille conductrice
        const driverChildrenIds = driver.children?.map(child => child.id) || [];
        this.tripForm.patchValue({
          childrenIds: driverChildrenIds
        });
      }
    }
    this.updateChildrenSelectionState();
  }

  onChildSelectionChange(childId: number, isChecked: boolean): void {
    const currentIds = this.tripForm.get('childrenIds')?.value || [];
    
    if (isChecked) {
      this.tripForm.patchValue({
        childrenIds: [...currentIds, childId]
      });
    } else {
      this.tripForm.patchValue({
        childrenIds: currentIds.filter((id: number) => id !== childId)
      });
    }
    
    this.updateChildrenSelectionState();
  }

  getRemainingCapacity(): number {
    const driverId = this.tripForm.get('driverId')?.value;
    if (!driverId) return 0;

    const driver = this.families.find(f => f.id === +driverId);
    if (!driver) return 0;

    const selectedChildrenCount = this.tripForm.get('childrenIds')?.value?.length || 0;
    return driver.carCapacity! - selectedChildrenCount;
  }

  getDriverCapacity(): number {
    const driverId = this.tripForm.get('driverId')?.value;
    return this.families.find(f => f.id === +driverId)?.carCapacity || 0;
  }

  onSubmit(): void {
    if (this.tripForm.valid && this.schedule && this.modalData) {
      const formValue = this.tripForm.value;
      const driver = this.families.find(f => f.id === +formValue.driverId);
      const children = this.getAllChildren().filter(child =>
        formValue.childrenIds.includes(child.id)
      );

      if (!driver) return;

      // Validation finale
      if (children.length > driver.carCapacity!) {
        alert(`Erreur : ${children.length} enfants sélectionnés mais la voiture ne peut transporter que ${driver.carCapacity} enfants maximum.`);
        return;
      }

      const trip: Trip = {
        id: this.editingTrip?.id,
        weekDay: formValue.weekDay,
        timeSlot: formValue.timeSlot,
        driver: driver,
        children: children
      };

      // Mettre à jour le planning local
      const currentSchedule = this.modalData.weekType === WeekType.Even 
        ? this.schedule.evenSchedule 
        : this.schedule.oddSchedule;

      if (currentSchedule) {
        if (!currentSchedule.trips) {
          currentSchedule.trips = [];
        }

        if (this.editingTrip && this.modalData.tripIndex !== undefined) {
          // Modifier le trajet existant
          const tripIndex = currentSchedule.trips.findIndex(t =>
            t.weekDay === this.modalData!.weekDay &&
            t.timeSlot === this.modalData!.timeSlot &&
            t.id === this.editingTrip!.id
          );
          if (tripIndex !== -1) {
            currentSchedule.trips[tripIndex] = trip;
          }
        } else {
          // Ajouter un nouveau trajet
          trip.id = undefined;
          currentSchedule.trips.push(trip);
        }
      }

      this.tripSaved.emit();
    }
  }

  onClose(): void {
    this.close.emit();
  }

  get isEditMode(): boolean {
    return this.editingTrip !== null;
  }
}