import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { FullSchedule } from '../../../../modules/openapi';
import { ScheduleService } from '../../services/schedule.service';

@Component({
  selector: 'app-schedule-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './schedule-list.component.html',
  styleUrl: './schedule-list.component.css'
})
export class ScheduleListComponent implements OnInit {
  schedules$!: Observable<FullSchedule[]>;

  constructor(private scheduleService: ScheduleService) {}

  ngOnInit(): void {
    this.loadSchedules();
  }

  loadSchedules(): void {
    this.schedules$ = this.scheduleService.getSchedules();
  }

  deleteSchedule(schedule: FullSchedule): void {
    if (schedule.id && confirm(`Êtes-vous sûr de vouloir supprimer le planning "${schedule.name}" ?`)) {
      this.scheduleService.deleteSchedule(schedule.id).subscribe({
        next: () => {
          this.loadSchedules();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          alert('Erreur lors de la suppression du planning');
        }
      });
    }
  }

  // Compter le nombre de trajets dans un planning
  getTotalTrips(schedule: FullSchedule): number {
    const evenTrips = schedule.evenSchedule?.trips ? schedule.evenSchedule.trips.length : 0;
    const oddTrips = schedule.oddSchedule?.trips ? schedule.oddSchedule.trips.length : 0;
    return evenTrips + oddTrips;
  }

  // Vérifier si un planning est complet
  isScheduleComplete(schedule: FullSchedule): boolean {
    const totalTrips = this.getTotalTrips(schedule);
    return totalTrips >= 16; // 8 créneaux × 2 semaines
  }
}