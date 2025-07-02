import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { Family, FullSchedule, TimeSlot, WeekDay, WeekType } from '../../../../modules/openapi';
import { ScheduleService } from '../../services/schedule.service';
import { FamilyService } from '../../../families/services/family.service';
import { ScheduleGridComponent } from './components/schedule-grid/schedule-grid.component';
import { WeekSelectorComponent } from './components/week-selector/week-selector.component';
import { TripModalComponent } from './components/trip-modal/trip-modal.component';
import { ScheduleHeaderComponent } from './components/schedule-header/schedule-header.component';

export interface ScheduleEditState {
  schedule: FullSchedule | null;
  families: Family[];
  isLoading: boolean;
  isSaving: boolean;
  selectedWeekType: WeekType;
}

export interface TripModalData {
  weekDay: WeekDay;
  timeSlot: TimeSlot;
  weekType: WeekType;
  tripIndex?: number;
}

@Component({
  selector: 'app-schedule-edit-v2',
  standalone: true,
  imports: [
    CommonModule,
    ScheduleHeaderComponent,
    WeekSelectorComponent,
    ScheduleGridComponent,
    TripModalComponent
  ],
  templateUrl: './schedule-edit-v2.component.html',
  styleUrl: './schedule-edit-v2.component.css'
})
export class ScheduleEditV2Component implements OnInit {
  state$: Observable<ScheduleEditState>;
  showTripModal = false;
  tripModalData: TripModalData | null = null;

  private scheduleId: number | null = null;

  constructor(
    private scheduleService: ScheduleService,
    private familyService: FamilyService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.state$ = this.initializeState();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.scheduleId = +id;
      this.loadData();
    }
  }

  private initializeState(): Observable<ScheduleEditState> {
    return combineLatest([
      this.scheduleService.getSchedule(this.scheduleId || 0),
      this.familyService.getFamilies()
    ]).pipe(
      map(([schedule, families]) => ({
        schedule,
        families,
        isLoading: false,
        isSaving: false,
        selectedWeekType: WeekType.Even
      }))
    );
  }

  private loadData(): void {
    // Les données sont chargées via l'Observable state$
  }

  onWeekTypeChange(weekType: WeekType): void {
    // Mettre à jour le type de semaine sélectionné
    this.state$ = this.state$.pipe(
      map(state => ({ ...state, selectedWeekType: weekType }))
    );
  }

  onOpenTripModal(data: TripModalData): void {
    this.tripModalData = data;
    this.showTripModal = true;
  }

  onCloseTripModal(): void {
    this.showTripModal = false;
    this.tripModalData = null;
  }

  onTripSaved(): void {
    this.onCloseTripModal();
    this.loadData(); // Recharger les données
  }

  onTripDeleted(): void {
    this.loadData(); // Recharger les données
  }

  onSaveSchedule(): void {
    this.state$.subscribe(state => {
      if (state.schedule && this.scheduleId) {
        this.scheduleService.updateSchedule(this.scheduleId, state.schedule).subscribe({
          next: () => {
            this.router.navigate(['/schedules', this.scheduleId, 'view']);
          },
          error: (error) => {
            console.error('Erreur lors de la sauvegarde:', error);
            alert('Erreur lors de la sauvegarde');
          }
        });
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/schedules']);
  }
}