import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { FullSchedule, FullScheduleResourceService } from '../../../modules/openapi';

@Injectable({
  providedIn: 'root'
})
export class ScheduleService {
  constructor(private fullScheduleResourceService: FullScheduleResourceService) {}

  getSchedules(): Observable<FullSchedule[]> {
    return this.fullScheduleResourceService.fullScheduleGet(undefined, undefined, undefined, undefined, undefined, ['name']);
  }

  getSchedule(id: number): Observable<FullSchedule> {
    return this.fullScheduleResourceService.fullScheduleIdGet(id);
  }

  createSchedule(schedule: FullSchedule): Observable<FullSchedule> {
    return this.fullScheduleResourceService.fullSchedulePost(schedule);
  }

  updateSchedule(id: number, schedule: FullSchedule): Observable<FullSchedule> {
    return this.fullScheduleResourceService.fullScheduleIdPut(id, schedule);
  }

  deleteSchedule(id: number): Observable<any> {
    return this.fullScheduleResourceService.fullScheduleIdDelete(id);
  }
}