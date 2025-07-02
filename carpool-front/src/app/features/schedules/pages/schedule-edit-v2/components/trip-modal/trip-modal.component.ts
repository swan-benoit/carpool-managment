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
  isInOtherTrip: boolean;
  otherTripInfo?: string;
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
  @Output() tripSaved = new EventEmitter<FullSchedule>();

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
      const otherTripInfo = this.getChildOtherTripInfo(child);
      const isInOtherTrip = otherTripInfo !== null;
      const capacityReached = driver && selectedChildrenIds.length >= driver.carCapacity! && !isSelected;

      let isDisabled = false;
      let disabledReason = '';

      if (capacityReached && !isInOtherTrip) {
        isDisabled = true;
        disabledReason = 'Capacité atteinte';
      }

      return {
        child,
        isSelected,
        isDisabled,
        disabledReason,
        isInOtherTrip,
        otherTripInfo: otherTripInfo || undefined
      };
    });
  }

  private getChildOtherTripInfo(child: Child): string | null {
    if (!this.modalData) return null;

    const currentTrips = this.getTripsForSlot(this.modalData.weekDay, this.modalData.timeSlot);
    
    const otherTrip = currentTrips.find(trip => 
      trip.id !== this.editingTrip?.id && 
      trip.children?.some(assignedChild => assignedChild.id === child.id)
    );

    if (otherTrip) {
      return `Avec ${otherTrip.driver?.name}`;
    }

    return null;
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

  onChildSelectionChange(childId: number, event: Event): void {
    const target = event.target as HTMLInputElement;
    const isChecked = target.checked;
    
    const currentIds = this.tripForm.get('childrenIds')?.value || [];
    const childState = this.childrenSelectionState.find(state => state.child.id === childId);
    
    if (isChecked) {
      // Si l'enfant est dans un autre trajet, le supprimer de cet autre trajet
      if (childState?.isInOtherTrip) {
        this.removeChildFromOtherTrip(childId);
      }
      
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

  private removeChildFromOtherTrip(childId: number): void {
    if (!this.modalData || !this.schedule) return;

    const currentSchedule = this.modalData.weekType === WeekType.Even 
      ? this.schedule.evenSchedule 
      : this.schedule.oddSchedule;

    if (!currentSchedule?.trips) return;

    // Trouver et modifier le trajet qui contient cet enfant
    currentSchedule.trips.forEach(trip => {
      if (trip.id !== this.editingTrip?.id && 
          trip.weekDay === this.modalData!.weekDay && 
          trip.timeSlot === this.modalData!.timeSlot &&
          trip.children?.some(child => child.id === childId)) {
        
        // Supprimer l'enfant de ce trajet
        trip.children = trip.children?.filter(child => child.id !== childId) || [];
      }
    });
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

  /**
   * Vérifie s'il y a des enfants dans d'autres trajets qui ne sont pas sélectionnés
   * Méthode extraite du template pour éviter la logique complexe dans le HTML
   */
  hasChildrenInOtherTrips(): boolean {
    return this.childrenSelectionState.some(state => 
      state.isInOtherTrip && !state.isSelected
    );
  }

  /**
   * Vérifie si la capacité maximale est atteinte
   * Méthode extraite du template pour une meilleure lisibilité
   */
  isCapacityReached(): boolean {
    return this.getRemainingCapacity() === 0 && !!this.tripForm.get('driverId')?.value;
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
        // IMPORTANT: L'ID doit être undefined pour les nouveaux trajets
        // Le backend se chargera de générer l'ID approprié
        id: this.editingTrip?.id, // undefined pour les nouveaux trajets, ID existant pour les modifications
        weekDay: formValue.weekDay,
        timeSlot: formValue.timeSlot,
        driver: driver,
        children: children
      };

      // Créer une copie profonde du planning pour éviter les mutations
      const updatedSchedule: FullSchedule = JSON.parse(JSON.stringify(this.schedule));
      
      // Mettre à jour le planning local
      const currentSchedule = this.modalData.weekType === WeekType.Even 
        ? updatedSchedule.evenSchedule 
        : updatedSchedule.oddSchedule;

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
          // L'ID reste undefined - le backend générera l'ID lors de la sauvegarde
          currentSchedule.trips.push(trip);
        }
      }

      // Émettre le planning mis à jour
      this.tripSaved.emit(updatedSchedule);
    }
  }

  onClose(): void {
    this.close.emit();
  }

  get isEditMode(): boolean {
    return this.editingTrip !== null;
  }
}