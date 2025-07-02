import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, combineLatest, of, BehaviorSubject } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { Family, FullSchedule, TimeSlot, WeekDay, WeekType } from '../../../../modules/openapi';
import { ScheduleService } from '../../services/schedule.service';
import { FamilyService } from '../../../families/services/family.service';
import { SnackbarService } from '../../../../shared/services/snackbar.service';
import { ScheduleGridComponent } from './components/schedule-grid/schedule-grid.component';
import { WeekSelectorComponent } from './components/week-selector/week-selector.component';
import { TripModalComponent } from './components/trip-modal/trip-modal.component';
import { ScheduleHeaderComponent } from './components/schedule-header/schedule-header.component';
import { SnackbarComponent } from '../../../../shared/components/snackbar/snackbar.component';

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
    TripModalComponent,
    SnackbarComponent
  ],
  templateUrl: './schedule-edit-v2.component.html',
  styleUrl: './schedule-edit-v2.component.css'
})
export class ScheduleEditV2Component implements OnInit {
  state$!: Observable<ScheduleEditState>;
  showTripModal = false;
  tripModalData: TripModalData | null = null;

  private scheduleId: number | null = null;
  private stateSubject = new BehaviorSubject<ScheduleEditState>({
    schedule: null,
    families: [],
    isLoading: true,
    isSaving: false,
    selectedWeekType: WeekType.Even
  });

  constructor(
    private scheduleService: ScheduleService,
    private familyService: FamilyService,
    private snackbarService: SnackbarService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.state$ = this.stateSubject.asObservable();
  }

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

    combineLatest([
      this.scheduleService.getSchedule(this.scheduleId),
      this.familyService.getFamilies()
    ]).subscribe({
      next: ([schedule, families]) => {
        this.updateState({
          schedule,
          families,
          isLoading: false,
          isSaving: false,
          selectedWeekType: WeekType.Even
        });
      },
      error: (error) => {
        console.error('Erreur lors du chargement:', error);
        this.snackbarService.error('Erreur lors du chargement du planning');
        this.updateState({
          schedule: null,
          families: [],
          isLoading: false,
          isSaving: false,
          selectedWeekType: WeekType.Even
        });
      }
    });
  }

  private updateState(newState: Partial<ScheduleEditState>): void {
    const currentState = this.stateSubject.value;
    this.stateSubject.next({
      ...currentState,
      ...newState
    });
  }

  onWeekTypeChange(weekType: WeekType): void {
    this.updateState({ selectedWeekType: weekType });
  }

  onOpenTripModal(data: TripModalData): void {
    this.tripModalData = data;
    this.showTripModal = true;
  }

  onCloseTripModal(): void {
    this.showTripModal = false;
    this.tripModalData = null;
  }

  onTripSaved(updatedSchedule: FullSchedule): void {
    // Mettre à jour l'état avec le planning modifié
    this.updateState({ schedule: updatedSchedule });
    this.onCloseTripModal();
    
    // Afficher une snackbar de succès
    this.snackbarService.success('Trajet ajouté avec succès ! 🚗');
  }

  onTripDeleted(): void {
    // L'état est déjà mis à jour dans le composant grid
    // Forcer une mise à jour pour déclencher la détection de changement
    const currentState = this.stateSubject.value;
    this.updateState({ schedule: { ...currentState.schedule! } });
    
    // Afficher une snackbar de succès
    this.snackbarService.success('Trajet supprimé avec succès ! 🗑️');
  }

  onSaveSchedule(): void {
    const currentState = this.stateSubject.value;
    if (currentState.schedule && this.scheduleId) {
      this.updateState({ isSaving: true });

      this.scheduleService.updateSchedule(this.scheduleId, currentState.schedule).subscribe({
        next: () => {
          this.updateState({ isSaving: false });
          
          // ✅ PLUS DE REDIRECTION - Rester sur l'interface de modification
          // this.router.navigate(['/schedules', this.scheduleId, 'view']);
          
          // ✅ Afficher une snackbar verte de succès
          this.snackbarService.success('Planning sauvegardé avec succès ! 🎉', 4000);
        },
        error: (error) => {
          console.error('Erreur lors de la sauvegarde:', error);
          this.updateState({ isSaving: false });
          
          // Afficher une snackbar d'erreur
          this.snackbarService.error('Erreur lors de la sauvegarde du planning');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/schedules']);
  }

  // Exposer le service snackbar pour le template
  get snackbar$() {
    return this.snackbarService.snackbar$;
  }
}