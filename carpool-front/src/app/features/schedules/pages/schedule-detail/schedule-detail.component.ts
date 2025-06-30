import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FullSchedule, Trip, WeekDay, TimeSlot } from '../../../../modules/openapi';
import { ScheduleService } from '../../services/schedule.service';

@Component({
  selector: 'app-schedule-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './schedule-detail.component.html',
  styleUrl: './schedule-detail.component.css'
})
export class ScheduleDetailComponent implements OnInit {
  schedule?: FullSchedule;
  isLoading = true;

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
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadSchedule(+id);
    }
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

  getTripsArray(trips?: Set<Trip>): Trip[] {
    return trips ? Array.from(trips) : [];
  }

  getTripForSlot(trips: Trip[], weekDay: WeekDay, timeSlot: TimeSlot): Trip | undefined {
    return trips.find(trip => trip.weekDay === weekDay && trip.timeSlot === timeSlot);
  }

  getChildrenArray(children?: Set<any>): any[] {
    return children ? Array.from(children) : [];
  }
}