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

    this.showTripModal = true;
  }

  closeTripModal(): void {
    this.showTripModal = false;
    this.selectedSlot = undefined;
    this.editingTrip = undefined;
    this.editingTripIndex = undefined;
    this.tripForm.reset();
  }

  onDriverChange(families: Family[]): void {
    const driverId = this.tripForm.get('driverId')?.value;
    if (driverId) {
      const driver = families.find(f => f.id === +driverId);
      if (driver) {
        // Pré-sélectionner les enfants de la famille conductrice
        const driverChildrenIds = driver.children?.map(child => child.id) || [];
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
    return this.getAllChildren(families);
  }

  isChildAlreadyAssigned(child: Child): boolean {
    if (!this.selectedSlot) return false;

    const currentTrips = this.getCurrentTrips();
    
    // Vérifier si l'enfant est assigné à un autre trajet pour ce créneau
    return currentTrips.some(trip => 
      trip.weekDay === this.selectedSlot!.weekDay &&
      trip.timeSlot === this.selectedSlot!.timeSlot &&
      trip.id !== this.editingTrip?.id && // Exclure le trajet en cours d'édition
      trip.children?.some(assignedChild => assignedChild.id === child.id)
    );
  }

  onChildSelectionChange(event: any, childId: number): void {
    const currentIds = this.tripForm.get('childrenIds')?.value || [];
    const driverId = this.tripForm.get('driverId')?.value;

    if (!driverId) return;

    this.families$.subscribe(families => {
      const driver = families.find(f => f.id === +driverId);
      if (!driver) return;

      const child = this.getAllChildren(families).find(c => c.id === childId);
      if (!child) return;

      if (event.target.checked) {
        // Vérifier si l'enfant est déjà assigné à un autre trajet
        if (this.isChildAlreadyAssigned(child)) {
          event.target.checked = false;
          alert(`${child.name} est déjà assigné(e) à un autre trajet pour ce créneau.`);
          return;
        }

        // Vérifier la capacité
        if (currentIds.length < driver.carCapacity!) {
          this.tripForm.patchValue({
            childrenIds: [...currentIds, childId]
          });
        } else {
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
      const children = this.getAllChildren(families).filter(child =>
        formValue.childrenIds.includes(child.id)
      );

      if (!driver) return;

      // Validation finale
      if (children.length > driver.carCapacity!) {
        alert(`Erreur : ${children.length} enfants sélectionnés mais la voiture ne peut transporter que ${driver.carCapacity} enfants maximum.`);
        return;
      }

      // Vérifier les enfants déjà assignés
      const alreadyAssignedChildren = children.filter(child => this.isChildAlreadyAssigned(child));
      if (alreadyAssignedChildren.length > 0) {
        const names = alreadyAssignedChildren.map(child => child.name).join(', ');
        alert(`Les enfants suivants sont déjà assignés à un autre trajet : ${names}`);
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
          trip.id = undefined;
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

    // Vérifier si l'enfant est déjà assigné à un autre trajet
    if (this.isChildAlreadyAssigned(child)) return true;

    // Sinon, vérifier si on peut encore ajouter des enfants
    return this.getRemainingCapacity(families) <= 0;
  }

  getCarpacity(families: Family[]) {
    let driverId = this.tripForm.get('driverId')?.value;
    return families.find(f => f.id == driverId)?.carCapacity ?? 0;
  }
}