import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, combineLatest, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
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
  state$!: Observable<ScheduleEditState>;
  showTripModal = false;
  tripModalData: TripModalData | null = null;

  private scheduleId: number | null = null;
  private currentState: ScheduleEditState = {
    schedule: null,
    families: [],
    isLoading: true,
    isSaving: false,
    selectedWeekType: WeekType.Even
  };

  constructor(
    private scheduleService: ScheduleService,
    private familyService: FamilyService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.scheduleId = +id;
      this.initializeState();
    } else {
      // Rediriger vers la liste si pas d'ID
      this.router.navigate(['/schedules']);
    }
  }

  private initializeState(): void {
    if (!this.scheduleId) return;

    this.state$ = combineLatest([
      this.scheduleService.getSchedule(this.scheduleId),
      this.familyService.getFamilies()
    ]).pipe(
      map(([schedule, families]) => {
        this.currentState = {
          schedule,
          families,
          isLoading: false,
          isSaving: false,
          selectedWeekType: WeekType.Even
        };
        return this.currentState;
      })
    );
  }

  onWeekTypeChange(weekType: WeekType): void {
    this.currentState = {
      ...this.currentState,
      selectedWeekType: weekType
    };
    this.state$ = of(this.currentState);
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
    // Pas besoin de recharger, les données sont déjà mises à jour dans le state
  }

  onTripDeleted(): void {
    // Pas besoin de recharger, les données sont déjà mises à jour dans le state
  }

  onSaveSchedule(): void {
    if (this.currentState.schedule && this.scheduleId) {
      this.currentState = {
        ...this.currentState,
        isSaving: true
      };
      this.state$ = of(this.currentState);

      this.scheduleService.updateSchedule(this.scheduleId, this.currentState.schedule).subscribe({
        next: () => {
          this.router.navigate(['/schedules', this.scheduleId, 'view']);
        },
        error: (error) => {
          console.error('Erreur lors de la sauvegarde:', error);
          alert('Erreur lors de la sauvegarde');
          this.currentState = {
            ...this.currentState,
            isSaving: false
          };
          this.state$ = of(this.currentState);
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/schedules']);
  }
}