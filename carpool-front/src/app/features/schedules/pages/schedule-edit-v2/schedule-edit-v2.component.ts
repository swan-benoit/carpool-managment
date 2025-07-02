import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, combineLatest, BehaviorSubject } from 'rxjs';
import { Family, FullSchedule, TimeSlot, WeekDay, WeekType, Stats } from '../../../../modules/openapi';
import { ScheduleService } from '../../services/schedule.service';
import { FamilyService } from '../../../families/services/family.service';
import { ScheduleStatsService } from '../../services/schedule-stats.service';
import { ScheduleGridComponent } from './components/schedule-grid/schedule-grid.component';
import { WeekSelectorComponent } from './components/week-selector/week-selector.component';
import { TripModalComponent } from './components/trip-modal/trip-modal.component';
import { ScheduleHeaderComponent } from './components/schedule-header/schedule-header.component';
import { StatsBannerComponent } from './components/stats-banner/stats-banner.component';
import { SnackbarService } from '../../../../shared/services/snackbar.service';

export interface ScheduleEditState {
  schedule: FullSchedule | null;
  families: Family[];
  stats: Stats[];
  isLoading: boolean;
  isSaving: boolean;
  selectedWeekType: WeekType;
  showStats: boolean;
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
    StatsBannerComponent,
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
    stats: [],
    isLoading: true,
    isSaving: false,
    selectedWeekType: WeekType.Even,
    showStats: false
  });

  constructor(
    private scheduleService: ScheduleService,
    private familyService: FamilyService,
    private scheduleStatsService: ScheduleStatsService,
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
      this.familyService.getFamilies(),
      this.scheduleStatsService.getScheduleStats(this.scheduleId)
    ]).subscribe({
      next: ([schedule, families, stats]) => {
        this.updateState({
          schedule,
          families,
          stats,
          isLoading: false,
          isSaving: false,
          selectedWeekType: WeekType.Even,
          showStats: false
        });
        
        // Afficher les stats après un court délai pour l'animation
        setTimeout(() => {
          this.updateState({ showStats: true });
        }, 300);
      },
      error: (error) => {
        console.error('Erreur lors du chargement:', error);
        this.snackbarService.error('Erreur lors du chargement du planning');
        this.updateState({
          schedule: null,
          families: [],
          stats: [],
          isLoading: false,
          isSaving: false,
          selectedWeekType: WeekType.Even,
          showStats: false
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

  /**
   * ✅ NOUVELLE MÉTHODE : Sauvegarde automatique + rafraîchissement des stats
   * Utilisée après chaque modification de trajet (ajout/suppression)
   */
  private saveAndRefreshStats(): void {
    const currentState = this.stateSubject.value;
    if (!currentState.schedule || !this.scheduleId) return;

    // 1️⃣ Sauvegarder le planning
    this.scheduleService.updateSchedule(this.scheduleId, currentState.schedule).subscribe({
      next: () => {
        // 2️⃣ Afficher la snackbar de succès
        this.snackbarService.success('Trajet sauvegardé avec succès ! 🚗');

        // 3️⃣ Rafraîchir les statistiques après la sauvegarde
        this.refreshStats();
      },
      error: (error) => {
        console.error('Erreur lors de la sauvegarde automatique:', error);
        this.snackbarService.error('Erreur lors de la sauvegarde du trajet');
      }
    });
  }

  private refreshStats(): void {
    if (!this.scheduleId) return;

    this.scheduleStatsService.getScheduleStats(this.scheduleId).subscribe({
      next: (stats) => {
        this.updateState({ stats, showStats: true });
      },
      error: (error) => {
        console.error('Erreur lors du chargement des stats:', error);
      }
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
    // ✅ CORRECTION : Mettre à jour l'état + sauvegarder automatiquement + rafraîchir les stats
    this.updateState({ schedule: updatedSchedule });
    this.onCloseTripModal();

    // ✅ NOUVEAU FLUX : Sauvegarde automatique → Snackbar → Stats
    this.saveAndRefreshStats();
  }

  onTripDeleted(): void {
    // ✅ CORRECTION : Sauvegarder automatiquement après suppression
    const currentState = this.stateSubject.value;
    this.updateState({ schedule: { ...currentState.schedule! } });

    // ✅ NOUVEAU FLUX : Sauvegarde automatique → Snackbar → Stats
    this.saveAndRefreshStats();
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

          // ✅ Afficher une snackbar verte de succès avec MatSnackBar
          this.snackbarService.success('Planning sauvegardé avec succès ! 🎉', 4000);

          // ✅ Rafraîchir les statistiques après sauvegarde manuelle
          this.refreshStats();
        },
        error: (error) => {
          console.error('Erreur lors de la sauvegarde:', error);
          this.updateState({ isSaving: false });

          // Afficher une snackbar d'erreur avec MatSnackBar
          this.snackbarService.error('Erreur lors de la sauvegarde du planning');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/schedules']);
  }
}