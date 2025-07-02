import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Stats, ScheduleStatResourceService } from '../../../modules/openapi';

@Injectable({
  providedIn: 'root'
})
export class ScheduleStatsService {
  constructor(private scheduleStatResourceService: ScheduleStatResourceService) {}

  getScheduleStats(scheduleId: number): Observable<Stats[]> {
    return this.scheduleStatResourceService.scheduleIdStatsGet(scheduleId);
  }
}