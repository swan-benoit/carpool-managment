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
  isAbsent: boolean; // ✅ NOUVEAU : Indique si l'enfant est absent
  absenceReason?: string; // ✅ NOUVEAU : Raison de l'absence
}

interface FamilyOption {
  family: Family;
  isDisabled: boolean;
  disabledReason?: string;
  isUnavailable: boolean; // ✅ NOUVEAU : Indique si la famille a une indisponibilité
  unavailabilityReason?: string; // ✅ NOUVEAU : Raison de l'indisponibilité
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
  familyOptions: FamilyOption[] = [];

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
      this.updateFamilyOptions();
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
      this.editingTrip = null; // Important: pas de trip en édition
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

  /**
   * ✅ NOUVELLE MÉTHODE : Vérifie si une famille a une indisponibilité pour le créneau donné
   */
  private isFamilyUnavailable(family: Family): boolean {
    if (!this.modalData || !family.requirements || family.requirements.length === 0) {
      return false;
    }

    console.log('🔍 Vérification indisponibilité pour:', family.name);
    console.log('📅 Créneau actuel:', {
      weekDay: this.modalData.weekDay,
      timeSlot: this.modalData.timeSlot,
      weekType: this.modalData.weekType
    });
    console.log('🚫 Indisponibilités:', family.requirements);

    const isUnavailable = Array.from(family.requirements).some(requirement => {
      // ✅ Comparaison directe des valeurs string
      const weekDayMatch = requirement.weekDay === this.modalData!.weekDay;
      const timeSlotMatch = requirement.timeSlot === this.modalData!.timeSlot;
      const weekTypeMatch = requirement.weekType === this.modalData!.weekType;
      
      console.log('🔍 Comparaison indisponibilité:', {
        requirement,
        weekDayMatch,
        timeSlotMatch,
        weekTypeMatch,
        result: weekDayMatch && timeSlotMatch && weekTypeMatch
      });
      
      return weekDayMatch && timeSlotMatch && weekTypeMatch;
    });

    console.log('✅ Résultat final pour', family.name, ':', isUnavailable ? 'INDISPONIBLE' : 'DISPONIBLE');
    return isUnavailable;
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Obtient la raison de l'indisponibilité d'une famille
   */
  private getUnavailabilityReason(family: Family): string {
    if (!this.modalData) return '';

    const weekTypeLabel = this.modalData.weekType === WeekType.Even ? 'paire' : 'impaire';
    const weekDayLabel = this.weekDays.find(day => day.value === this.modalData!.weekDay)?.label || '';
    const timeSlotLabel = this.timeSlots.find(slot => slot.value === this.modalData!.timeSlot)?.label || '';
    
    return `Indisponible le ${weekDayLabel} ${timeSlotLabel} en semaine ${weekTypeLabel}`;
  }

  /**
   * Met à jour les options de familles disponibles pour conduire
   * Exclut les familles qui conduisent déjà sur ce créneau (sauf en mode édition)
   * ✅ NOUVEAU : Marque les familles indisponibles
   */
  private updateFamilyOptions(): void {
    if (!this.modalData) {
      this.familyOptions = [];
      return;
    }

    const existingTrips = this.getTripsForSlot(this.modalData.weekDay, this.modalData.timeSlot);
    const usedFamilyIds = existingTrips
      .filter(trip => trip.id !== this.editingTrip?.id) // Exclure le trip en cours d'édition
      .map(trip => trip.driver?.id)
      .filter(id => id !== undefined);

    this.familyOptions = this.families.map(family => {
      const isAlreadyDriving = usedFamilyIds.includes(family.id ?? -1);
      
      // ✅ NOUVEAU : Vérifier l'indisponibilité
      const isUnavailable = this.isFamilyUnavailable(family);
      const unavailabilityReason = isUnavailable ? this.getUnavailabilityReason(family) : undefined;

      // ✅ MODIFICATION : Une famille indisponible peut toujours être sélectionnée (avec avertissement)
      // mais une famille qui conduit déjà ne peut pas être sélectionnée
      const isDisabled = isAlreadyDriving;
      let disabledReason = '';
      
      if (isAlreadyDriving) {
        disabledReason = 'Conduit déjà sur ce créneau';
      }

      return {
        family,
        isDisabled,
        disabledReason,
        isUnavailable, // ✅ NOUVEAU
        unavailabilityReason // ✅ NOUVEAU
      };
    });
  }

  /**
   * ✅ CORRECTION CRITIQUE : Vérifie si un enfant est absent pour le créneau donné
   * Le problème était dans la comparaison des enums
   */
  private isChildAbsent(child: Child): boolean {
    if (!this.modalData || !child.absenceDays || child.absenceDays.length === 0) {
      return false;
    }

    console.log('🔍 Vérification absence pour:', child.name);
    console.log('📅 Créneau actuel:', {
      weekDay: this.modalData.weekDay,
      weekType: this.modalData.weekType
    });
    console.log('🚫 Jours d\'absence:', child.absenceDays);

    const isAbsent = child.absenceDays.some(absence => {
      // ✅ CORRECTION : Comparaison directe des valeurs string
      const weekDayMatch = absence.weekDay === this.modalData!.weekDay;
      const weekTypeMatch = absence.weekType === this.modalData!.weekType;
      
      console.log('🔍 Comparaison:', {
        absence,
        weekDayMatch,
        weekTypeMatch,
        result: weekDayMatch && weekTypeMatch
      });
      
      return weekDayMatch && weekTypeMatch;
    });

    console.log('✅ Résultat final pour', child.name, ':', isAbsent ? 'ABSENT' : 'DISPONIBLE');
    return isAbsent;
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Obtient la raison de l'absence d'un enfant
   */
  private getAbsenceReason(child: Child): string {
    if (!this.modalData) return '';

    const weekTypeLabel = this.modalData.weekType === WeekType.Even ? 'paire' : 'impaire';
    const weekDayLabel = this.weekDays.find(day => day.value === this.modalData!.weekDay)?.label || '';
    
    return `Absent le ${weekDayLabel} en semaine ${weekTypeLabel}`;
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
      
      // ✅ NOUVEAU : Vérifier si l'enfant est absent
      const isAbsent = this.isChildAbsent(child);
      const absenceReason = isAbsent ? this.getAbsenceReason(child) : undefined;

      let isDisabled = false;
      let disabledReason = '';

      // ✅ NOUVEAU : Priorité à l'absence sur les autres raisons
      if (isAbsent) {
        isDisabled = true;
        disabledReason = 'Absent ce jour-là';
      } else if (capacityReached && !isInOtherTrip) {
        isDisabled = true;
        disabledReason = 'Capacité atteinte';
      }

      return {
        child,
        isSelected,
        isDisabled,
        disabledReason,
        isInOtherTrip,
        otherTripInfo: otherTripInfo || undefined,
        isAbsent, // ✅ NOUVEAU
        absenceReason // ✅ NOUVEAU
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
        // ✅ MODIFICATION : Pré-sélectionner seulement les enfants NON ABSENTS de la famille conductrice
        const driverChildrenIds = driver.children
          ?.filter(child => !this.isChildAbsent(child)) // ✅ Filtrer les enfants absents
          ?.map(child => child.id) || [];
        
        console.log('👨‍👩‍👧‍👦 Enfants de la famille conductrice (non absents):', driverChildrenIds);
        
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

    // ✅ NOUVEAU : Empêcher la sélection d'enfants absents
    if (isChecked && childState?.isAbsent) {
      target.checked = false;
      alert(`${childState.child.name} est absent ce jour-là et ne peut pas être sélectionné(e).`);
      return;
    }

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
   * ✅ NOUVELLE MÉTHODE : Vérifie s'il y a des enfants absents
   */
  hasAbsentChildren(): boolean {
    return this.childrenSelectionState.some(state => state.isAbsent);
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Obtient la liste des enfants absents
   */
  getAbsentChildren(): ChildSelectionState[] {
    return this.childrenSelectionState.filter(state => state.isAbsent);
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Obtient la liste des enfants disponibles (non absents)
   */
  getAvailableChildren(): ChildSelectionState[] {
    return this.childrenSelectionState.filter(state => !state.isAbsent);
  }

  /**
   * Vérifie si la capacité maximale est atteinte
   * Méthode extraite du template pour une meilleure lisibilité
   */
  isCapacityReached(): boolean {
    return this.getRemainingCapacity() === 0 && !!this.tripForm.get('driverId')?.value;
  }

  /**
   * ✅ MODIFICATION : Obtient les familles disponibles pour conduire (non désactivées)
   */
  getAvailableFamilies(): FamilyOption[] {
    return this.familyOptions.filter(option => !option.isDisabled && !option.isUnavailable);
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Obtient les familles indisponibles (avec requirements)
   */
  getUnavailableFamilies(): FamilyOption[] {
    return this.familyOptions.filter(option => option.isUnavailable && !option.isDisabled);
  }

  /**
   * ✅ MODIFICATION : Obtient les familles non disponibles pour conduire (désactivées - conduisent déjà)
   */
  getDisabledFamilies(): FamilyOption[] {
    return this.familyOptions.filter(option => option.isDisabled);
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Vérifie s'il y a des familles indisponibles
   */
  hasUnavailableFamilies(): boolean {
    return this.getUnavailableFamilies().length > 0;
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Vérifie si la famille sélectionnée est indisponible
   */
  isSelectedFamilyUnavailable(): boolean {
    const driverId = this.tripForm.get('driverId')?.value;
    if (!driverId) return false;
    
    const familyOption = this.familyOptions.find(option => option.family.id === +driverId);
    return familyOption?.isUnavailable || false;
  }

  /**
   * ✅ NOUVELLE MÉTHODE : Obtient le message d'avertissement pour la famille sélectionnée
   */
  getSelectedFamilyWarning(): string {
    const driverId = this.tripForm.get('driverId')?.value;
    if (!driverId) return '';
    
    const familyOption = this.familyOptions.find(option => option.family.id === +driverId);
    return familyOption?.unavailabilityReason || '';
  }

  onSubmit(): void {
    if (this.tripForm.valid && this.schedule && this.modalData) {
      const formValue = this.tripForm.value;
      const driver = this.families.find(f => f.id === +formValue.driverId);
      const children = this.getAllChildren().filter(child =>
        formValue.childrenIds.includes(child.id)
      );

      if (!driver) return;

      // ✅ NOUVELLE VALIDATION : Vérifier qu'aucun enfant sélectionné n'est absent
      const absentSelectedChildren = children.filter(child => this.isChildAbsent(child));
      if (absentSelectedChildren.length > 0) {
        const absentNames = absentSelectedChildren.map(child => child.name).join(', ');
        alert(`Erreur : Les enfants suivants sont absents ce jour-là et ne peuvent pas être transportés : ${absentNames}`);
        return;
      }

      // ✅ NOUVEAU : Avertissement si la famille est indisponible
      if (this.isSelectedFamilyUnavailable()) {
        const familyName = driver.name;
        const reason = this.getSelectedFamilyWarning();
        const confirmMessage = `⚠️ ATTENTION : La famille "${familyName}" a une indisponibilité pour ce créneau.\n\nRaison : ${reason}\n\nÊtes-vous sûr de vouloir continuer ?`;
        
        if (!confirm(confirmMessage)) {
          return; // L'utilisateur a annulé
        }
      }

      // Validation finale
      if (children.length > driver.carCapacity!) {
        alert(`Erreur : ${children.length} enfants sélectionnés mais la voiture ne peut transporter que ${driver.carCapacity} enfants maximum.`);
        return;
      }

      // Vérifier que la famille n'est pas déjà conductrice sur ce créneau (sauf en mode édition)
      const familyOption = this.familyOptions.find(option => option.family.id === driver.id);
      if (familyOption?.isDisabled && !this.isEditMode) {
        alert(`Erreur : La famille "${driver.name}" conduit déjà sur ce créneau.`);
        return;
      }

      const trip: Trip = {
        // ✅ CORRECTION IMPORTANTE :
        // - Pour les nouveaux trajets (mode création) : id = undefined
        // - Pour les trajets existants (mode édition) : id = ID existant
        id: this.editingTrip?.id,
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

        if (this.isEditMode && this.modalData.tripIndex !== undefined) {
          // ✅ Mode édition : modifier le trajet existant
          const tripIndex = currentSchedule.trips.findIndex(t =>
            t.weekDay === this.modalData!.weekDay &&
            t.timeSlot === this.modalData!.timeSlot &&
            t.id === this.editingTrip!.id
          );
          if (tripIndex !== -1) {
            currentSchedule.trips[tripIndex] = trip;
          }
        } else {
          // ✅ Mode création : ajouter un nouveau trajet avec id = undefined
          // Le backend générera automatiquement l'ID lors de la sauvegarde
          currentSchedule.trips.push({
            id: undefined,
            ...trip
          });
        }
      }

      // Émettre le planning mis à jour
      console.log('💾 Sauvegarde du planning:', {
        id: this.editingTrip?.id,
        ...updatedSchedule
      });
      this.tripSaved.emit({
        id: this.editingTrip?.id,
        ...updatedSchedule
      });
    }
  }

  onClose(): void {
    this.close.emit();
  }

  get isEditMode(): boolean {
    return this.editingTrip !== null;
  }
}