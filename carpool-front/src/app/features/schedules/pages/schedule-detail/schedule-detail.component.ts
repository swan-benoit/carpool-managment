import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FullSchedule, Trip, WeekDay, TimeSlot, Family, Child } from '../../../../modules/openapi';
import { ScheduleService } from '../../services/schedule.service';
import { FamilyService } from '../../../families/services/family.service';

@Component({
  selector: 'app-schedule-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './schedule-detail.component.html',
  styleUrl: './schedule-detail.component.css'
})
export class ScheduleDetailComponent implements OnInit {
  schedule?: FullSchedule;
  families: Family[] = [];
  isLoading = true;
  showTripModal = false;
  tripForm!: FormGroup;
  selectedWeekType: 'even' | 'odd' = 'even';
  selectedWeekDay?: WeekDay;
  selectedTimeSlot?: TimeSlot;
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
    private scheduleService: ScheduleService,
    private familyService: FamilyService,
    private route: ActivatedRoute,
    private fb: FormBuilder
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
    this.familyService.getFamilies().subscribe({
      next: (families) => {
        this.families = families;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des familles:', error);
      }
    });
  }

  getTripsArray(trips?: Set<Trip>): Trip[] {
    return trips ? Array.from(trips) : [];
  }

  getTripForSlot(trips: Trip[], weekDay: WeekDay, timeSlot: TimeSlot): Trip | undefined {
    return trips.find(trip => trip.weekDay === weekDay && trip.timeSlot === timeSlot);
  }

  getChildrenArray(children?: Set<any>): any[] {
    return children ? Array.from(children) : [];
  }

  getAllChildren(): Child[] {
    return this.families.flatMap(family => family.children || []);
  }

  getSelectedDriver(): Family | undefined {
    const driverId = this.tripForm.get('driverId')?.value;
    return this.families.find(family => family.id === +driverId);
  }

  openTripModal(weekType: 'even' | 'odd', weekDay: WeekDay, timeSlot: TimeSlot): void {
    this.selectedWeekType = weekType;
    this.selectedWeekDay = weekDay;
    this.selectedTimeSlot = timeSlot;
    
    // Vérifier s'il y a déjà un trajet pour ce créneau
    const trips = weekType === 'even' 
      ? this.getTripsArray(this.schedule?.evenSchedule?.trips)
      : this.getTripsArray(this.schedule?.oddSchedule?.trips);
    
    this.editingTrip = this.getTripForSlot(trips, weekDay, timeSlot);
    
    if (this.editingTrip) {
      // Mode édition
      this.tripForm.patchValue({
        driverId: this.editingTrip.driver?.id || '',
        childrenIds: this.getChildrenArray(this.editingTrip.children).map(child => child.id)
      });
    } else {
      // Mode création
      this.tripForm.reset();
      this.tripForm.patchValue({
        driverId: '',
        childrenIds: []
      });
    }
    
    this.showTripModal = true;
  }

  closeTripModal(): void {
    this.showTripModal = false;
    this.editingTrip = undefined;
    this.tripForm.reset();
  }

  onDriverChange(): void {
    // Réinitialiser la sélection des enfants quand le conducteur change
    this.tripForm.patchValue({ childrenIds: [] });
  }

  onChildrenChange(childId: number, event: any): void {
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

  isChildSelected(childId: number): boolean {
    const selectedIds = this.tripForm.get('childrenIds')?.value || [];
    return selectedIds.includes(childId);
  }

  canSelectChild(child: Child): boolean {
    const selectedDriver = this.getSelectedDriver();
    if (!selectedDriver) return false;
    
    const selectedIds = this.tripForm.get('childrenIds')?.value || [];
    const currentCount = selectedIds.length;
    
    // Vérifier la capacité de la voiture
    if (currentCount >= selectedDriver.carCapacity && !this.isChildSelected(child.id!)) {
      return false;
    }
    
    return true;
  }

  saveTrip(): void {
    if (this.tripForm.valid && this.schedule) {
      const formValue = this.tripForm.value;
      const driver = this.families.find(f => f.id === +formValue.driverId);
      const children = this.getAllChildren().filter(child => 
        formValue.childrenIds.includes(child.id)
      );

      const newTrip: Trip = {
        id: this.editingTrip?.id,
        weekDay: this.selectedWeekDay,
        timeSlot: this.selectedTimeSlot,
        driver: driver,
        children: new Set(children)
      };

      // Mettre à jour le planning
      const updatedSchedule = { ...this.schedule };
      
      if (this.selectedWeekType === 'even') {
        const evenTrips = this.getTripsArray(updatedSchedule.evenSchedule?.trips);
        const filteredTrips = evenTrips.filter(trip => 
          !(trip.weekDay === this.selectedWeekDay && trip.timeSlot === this.selectedTimeSlot)
        );
        updatedSchedule.evenSchedule = {
          ...updatedSchedule.evenSchedule,
          trips: new Set([...filteredTrips, newTrip])
        };
      } else {
        const oddTrips = this.getTripsArray(updatedSchedule.oddSchedule?.trips);
        const filteredTrips = oddTrips.filter(trip => 
          !(trip.weekDay === this.selectedWeekDay && trip.timeSlot === this.selectedTimeSlot)
        );
        updatedSchedule.oddSchedule = {
          ...updatedSchedule.oddSchedule,
          trips: new Set([...filteredTrips, newTrip])
        };
      }

      // Sauvegarder
      this.scheduleService.updateSchedule(this.schedule.id!, updatedSchedule).subscribe({
        next: (updated) => {
          this.schedule = updated;
          this.closeTripModal();
        },
        error: (error) => {
          console.error('Erreur lors de la sauvegarde:', error);
          alert('Erreur lors de la sauvegarde du trajet');
        }
      });
    }
  }

  deleteTrip(): void {
    if (this.editingTrip && this.schedule && 
        confirm('Êtes-vous sûr de vouloir supprimer ce trajet ?')) {
      
      const updatedSchedule = { ...this.schedule };
      
      if (this.selectedWeekType === 'even') {
        const evenTrips = this.getTripsArray(updatedSchedule.evenSchedule?.trips);
        const filteredTrips = evenTrips.filter(trip => 
          !(trip.weekDay === this.selectedWeekDay && trip.timeSlot === this.selectedTimeSlot)
        );
        updatedSchedule.evenSchedule = {
          ...updatedSchedule.evenSchedule,
          trips: new Set(filteredTrips)
        };
      } else {
        const oddTrips = this.getTripsArray(updatedSchedule.oddSchedule?.trips);
        const filteredTrips = oddTrips.filter(trip => 
          !(trip.weekDay === this.selectedWeekDay && trip.timeSlot === this.selectedTimeSlot)
        );
        updatedSchedule.oddSchedule = {
          ...updatedSchedule.oddSchedule,
          trips: new Set(filteredTrips)
        };
      }

      this.scheduleService.updateSchedule(this.schedule.id!, updatedSchedule).subscribe({
        next: (updated) => {
          this.schedule = updated;
          this.closeTripModal();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          alert('Erreur lors de la suppression du trajet');
        }
      });
    }
  }

  getWeekTypeLabel(weekType: 'even' | 'odd'): string {
    return weekType === 'even' ? 'paire' : 'impaire';
  }

  getDayLabel(weekDay?: WeekDay): string {
    return this.weekDays.find(day => day.value === weekDay)?.label || '';
  }

  getTimeSlotLabel(timeSlot?: TimeSlot): string {
    return this.timeSlots.find(slot => slot.value === timeSlot)?.label || '';
  }
}