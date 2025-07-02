import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs';
import {Child, Family, FullSchedule, TimeSlot, Trip, WeekDay} from '../../../../modules/openapi';
import {ScheduleService} from '../../services/schedule.service';
import {FamilyService} from '../../../families/services/family.service';

@Component({
  selector: 'app-schedule-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './schedule-edit.component.html',
  styleUrl: './schedule-edit.component.css'
})
export class ScheduleEditComponent implements OnInit {
  schedule?: FullSchedule;
  families$!: Observable<Family[]>;
  isLoading = true;
  isSaving = false;

  selectedWeekType: 'EVEN' | 'ODD' = 'EVEN';
  selectedSlot?: { weekDay: WeekDay; timeSlot: TimeSlot };

  tripForm!: FormGroup;
  showTripModal = false;
  editingTrip?: Trip;
  editingTripIndex?: number;

  // Cache pour les enfants disponibles
  availableChildrenCache: Child[] = [];

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

  constructor(
    private fb: FormBuilder,
    private scheduleService: ScheduleService,
    private familyService: FamilyService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.initTripForm();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadSchedule(+id);
      this.loadFamilies();
    }
  }

  initTripForm(): void {
    this.tripForm = this.fb.group({
      weekDay: ['', Validators.required],
      timeSlot: ['', Validators.required],
      driverId: ['', Validators.required],
      childrenIds: [[]]
    });

    // Écouter les changements de jour et créneau pour mettre à jour la liste des enfants
    this.tripForm.get('weekDay')?.valueChanges.subscribe(() => {
      this.updateAvailableChildren();
      this.resetChildrenSelection();
    });

    this.tripForm.get('timeSlot')?.valueChanges.subscribe(() => {
      this.updateAvailableChildren();
      this.resetChildrenSelection();
    });
  }

  loadSchedule(id: number): void {
    this.scheduleService.getSchedule(id).subscribe({
      next: (schedule) => {
        this.schedule = schedule;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement:', error);
        this.isLoading = false;
      }
    });
  }

  loadFamilies(): void {
    this.families$ = this.familyService.getFamilies();
  }

  getCurrentSchedule() {
    return this.selectedWeekType === 'EVEN'
      ? this.schedule?.evenSchedule
      : this.schedule?.oddSchedule;
  }

  getCurrentTrips(): Trip[] {
    return this.getCurrentSchedule()?.trips || [];
  }

  getTripsForSlot(weekDay: WeekDay, timeSlot: TimeSlot): Trip[] {
    return this.getCurrentTrips().filter(trip =>
      trip.weekDay === weekDay && trip.timeSlot === timeSlot
    );
  }

  openTripModal(weekDay: WeekDay, timeSlot: TimeSlot, tripIndex?: number): void {
    this.selectedSlot = { weekDay, timeSlot };

    if (tripIndex !== undefined) {
      // Mode édition
      const trips = this.getTripsForSlot(weekDay, timeSlot);
      this.editingTrip = trips[tripIndex];
      this.editingTripIndex = tripIndex;

      this.tripForm.patchValue({
        weekDay: this.editingTrip.weekDay,
        timeSlot: this.editingTrip.timeSlot,
        driverId: this.editingTrip.driver?.id,
        childrenIds: this.editingTrip.children?.map(child => child.id) || []
      });
    } else {
      // Mode création
      this.editingTrip = undefined;
      this.editingTripIndex = undefined;

      this.tripForm.patchValue({
        weekDay,
        timeSlot,
        driverId: '',
        childrenIds: []
      });
    }

    // Mettre à jour la liste des enfants disponibles
    this.updateAvailableChildren();
    this.showTripModal = true;
  }

  closeTripModal(): void {
    this.showTripModal = false;
    this.selectedSlot = undefined;
    this.editingTrip = undefined;
    this.editingTripIndex = undefined;
    this.availableChildrenCache = [];
    this.tripForm.reset();
  }

  updateAvailableChildren(): void {
    this.families$.subscribe(families => {
      this.availableChildrenCache = this.calculateAvailableChildren(families);
    });
  }

  calculateAvailableChildren(families: Family[]): Child[] {
    const weekDay = this.tripForm.get('weekDay')?.value;
    const timeSlot = this.tripForm.get('timeSlot')?.value;

    if (!weekDay || !timeSlot) {
      return [];
    }

    const allChildren = this.getAllChildren(families);
    const currentTrips = this.getCurrentTrips();

    // Obtenir tous les enfants déjà assignés à des trajets pour ce créneau
    const assignedChildrenIds = currentTrips
      .filter(trip =>
        trip.weekDay === weekDay &&
        trip.timeSlot === timeSlot &&
        trip.id !== this.editingTrip?.id // Exclure le trajet en cours d'édition
      )
      .flatMap(trip => trip.children?.map(child => child.id) || []);

    // Retourner seulement les enfants non assignés
    return allChildren.filter(child => !assignedChildrenIds.includes(child.id));
  }

  resetChildrenSelection(): void {
    this.tripForm.patchValue({
      childrenIds: []
    });
  }

  onDriverChange(families: Family[]): void {
    const driverId = this.tripForm.get('driverId')?.value;
    if (driverId) {
      const driver = families.find(f => f.id === +driverId);
      if (driver) {
        // Pré-sélectionner les enfants de la famille conductrice qui sont disponibles
        const driverChildrenIds = driver.children
          ?.filter(child => this.availableChildrenCache.some(available => available.id === child.id))
          .map(child => child.id) || [];
        
        this.tripForm.patchValue({
          childrenIds: driverChildrenIds
        });
      }
    }
  }

  getAllChildren(families: Family[]): Child[] {
    return families.flatMap(family => family.children || []);
  }

  getAvailableChildren(families: Family[]): Child[] {
    // Utiliser le cache si disponible, sinon calculer
    if (this.availableChildrenCache.length > 0) {
      return this.availableChildrenCache;
    }
    return this.calculateAvailableChildren(families);
  }

  onChildSelectionChange(event: any, childId: number): void {
    const currentIds = this.tripForm.get('childrenIds')?.value || [];
    const driverId = this.tripForm.get('driverId')?.value;

    if (!driverId) return;

    // Récupérer la famille conductrice pour vérifier la capacité
    this.families$.subscribe(families => {
      const driver = families.find(f => f.id === +driverId);
      if (!driver) return;

      if (event.target.checked) {
        // Vérifier si on peut ajouter cet enfant sans dépasser la capacité
        if (currentIds.length < driver.carCapacity!) {
          this.tripForm.patchValue({
            childrenIds: [...currentIds, childId]
          });
        } else {
          // Décocher la case si la capacité est dépassée
          event.target.checked = false;
          alert(`Capacité maximale atteinte ! Cette voiture ne peut transporter que ${driver.carCapacity} enfants.`);
        }
      } else {
        this.tripForm.patchValue({
          childrenIds: currentIds.filter((id: number) => id !== childId)
        });
      }
    });
  }

  saveTripModal(families: Family[]): void {
    if (this.tripForm.valid && this.schedule && this.selectedSlot) {
      const formValue = this.tripForm.value;
      const driver = families.find(f => f.id === +formValue.driverId);
      const children = this.availableChildrenCache.filter(child =>
        formValue.childrenIds.includes(child.id)
      );

      if (!driver) return;

      // Validation finale de la capacité
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
      const currentSchedule = this.getCurrentSchedule();
      if (currentSchedule) {
        if (!currentSchedule.trips) {
          currentSchedule.trips = [];
        }

        if (this.editingTrip && this.editingTripIndex !== undefined) {
          // Modifier le trajet existant
          const tripIndex = currentSchedule.trips.findIndex(t =>
            t.weekDay === this.selectedSlot!.weekDay &&
            t.timeSlot === this.selectedSlot!.timeSlot &&
            t.id === this.editingTrip!.id
          );
          if (tripIndex !== -1) {
            currentSchedule.trips[tripIndex] = trip;
          }
        } else {
          // Ajouter un nouveau trajet
          trip.id = undefined; // ID temporaire
          currentSchedule.trips.push(trip);
        }
      }

      this.closeTripModal();
    }
  }

  deleteTrip(weekDay: WeekDay, timeSlot: TimeSlot, tripIndex: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce trajet ?')) {
      const currentSchedule = this.getCurrentSchedule();
      if (currentSchedule?.trips) {
        const trips = this.getTripsForSlot(weekDay, timeSlot);
        const tripToDelete = trips[tripIndex];

        currentSchedule.trips = currentSchedule.trips.filter(trip =>
          !(trip.weekDay === weekDay &&
            trip.timeSlot === timeSlot &&
            trip.id === tripToDelete.id)
        );
      }
    }
  }

  saveSchedule(): void {
    if (this.schedule && this.schedule.id) {
      this.isSaving = true;
      this.scheduleService.updateSchedule(this.schedule.id, this.schedule).subscribe({
        next: () => {
          this.isSaving = false;
          this.router.navigate(['/schedules', this.schedule!.id, 'view']);
        },
        error: (error) => {
          console.error('Erreur lors de la sauvegarde:', error);
          this.isSaving = false;
          alert('Erreur lors de la sauvegarde');
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/schedules']);
  }

  getRemainingCapacity(families: Family[]): number {
    const driverId = this.tripForm.get('driverId')?.value;
    if (!driverId) return 0;

    const driver = families.find(f => f.id === +driverId);
    if (!driver) return 0;

    const selectedChildrenCount = this.tripForm.get('childrenIds')?.value?.length || 0;
    return driver.carCapacity! - selectedChildrenCount;
  }

  canAddMoreTrips(weekDay: WeekDay, timeSlot: TimeSlot, families: Family[]): boolean {
    const existingTrips = this.getTripsForSlot(weekDay, timeSlot);
    const usedFamilyIds = existingTrips.map(trip => trip.driver?.id);
    const availableFamilies = families.filter(family => !usedFamilyIds.includes(family.id));
    return availableFamilies.length > 0;
  }

  isChildSelectionDisabled(child: Child, families: Family[]): boolean {
    const currentIds = this.tripForm.get('childrenIds')?.value || [];
    const isAlreadySelected = currentIds.includes(child.id);

    // Si l'enfant est déjà sélectionné, il peut être désélectionné
    if (isAlreadySelected) return false;

    // Sinon, vérifier si on peut encore ajouter des enfants
    return this.getRemainingCapacity(families) <= 0;
  }

  getCarpacity(families: Family[]) {
    let driverId = this.tripForm.get('driverId')?.value;
    return families.find(f => f.id == driverId)?.carCapacity ?? 0;
  }
}