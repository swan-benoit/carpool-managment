import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import {
  FullSchedule,
  Trip,
  WeekDay,
  TimeSlot,
  Family,
  Child
} from '../../../../modules/openapi';
import { ScheduleService } from '../../services/schedule.service';
import { FamilyService } from '../../../families/services/family.service';

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

  getTripForSlot(weekDay: WeekDay, timeSlot: TimeSlot): Trip | undefined {
    return this.getCurrentTrips().find(trip =>
      trip.weekDay === weekDay && trip.timeSlot === timeSlot
    );
  }

  openTripModal(weekDay: WeekDay, timeSlot: TimeSlot): void {
    this.selectedSlot = { weekDay, timeSlot };
    this.editingTrip = this.getTripForSlot(weekDay, timeSlot);

    if (this.editingTrip) {
      // Mode édition
      this.tripForm.patchValue({
        weekDay: this.editingTrip.weekDay,
        timeSlot: this.editingTrip.timeSlot,
        driverId: this.editingTrip.driver?.id,
        childrenIds: this.editingTrip.children?.map(child => child.id) || []
      });
    } else {
      // Mode création
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
    if (!this.selectedSlot) return [];

    const allChildren = this.getAllChildren(families);
    const currentTrips = this.getCurrentTrips();

    // Exclure les enfants déjà assignés à d'autres trajets pour ce créneau
    const assignedChildrenIds = currentTrips
      .filter(trip =>
        trip.weekDay === this.selectedSlot!.weekDay &&
        trip.timeSlot === this.selectedSlot!.timeSlot &&
        trip.id !== this.editingTrip?.id
      )
      .flatMap(trip => trip.children?.map(child => child.id) || []);

    return allChildren.filter(child => !assignedChildrenIds.includes(child.id));
  }

  onChildSelectionChange(event: any, childId: number): void {
    const currentIds = this.tripForm.get('childrenIds')?.value || [];
    if (event.target.checked) {
      this.tripForm.patchValue({
        childrenIds: [...currentIds, childId]
      });
    } else {
      this.tripForm.patchValue({
        childrenIds: currentIds.filter((id: number) => id !== childId)
      });
    }
  }

  saveTripModal(families: Family[]): void {
    if (this.tripForm.valid && this.schedule && this.selectedSlot) {
      const formValue = this.tripForm.value;
      const driver = families.find(f => f.id === +formValue.driverId);
      const children = this.getAllChildren(families).filter(child =>
        formValue.childrenIds.includes(child.id)
      );

      if (!driver) return;

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

        if (this.editingTrip) {
          // Modifier le trajet existant
          const index = currentSchedule.trips.findIndex(t => t.id === this.editingTrip!.id);
          if (index !== -1) {
            currentSchedule.trips[index] = trip;
          }
        } else {
          trip.id = undefined; // ID temporaire
          currentSchedule.trips.push(trip);
        }
      }

      this.closeTripModal();
    }
  }

  deleteTrip(weekDay: WeekDay, timeSlot: TimeSlot): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce trajet ?')) {
      const currentSchedule = this.getCurrentSchedule();
      if (currentSchedule?.trips) {
        currentSchedule.trips = currentSchedule.trips.filter(trip =>
          !(trip.weekDay === weekDay && trip.timeSlot === timeSlot)
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
}
