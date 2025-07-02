import { Routes } from '@angular/router';

export const schedulesRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/schedule-list/schedule-list.component').then(c => c.ScheduleListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./pages/schedule-form/schedule-form.component').then(c => c.ScheduleFormComponent)
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./pages/schedule-edit/schedule-edit.component').then(c => c.ScheduleEditComponent)
  },
  {
    path: ':id/edit-v2',
    loadComponent: () => import('./pages/schedule-edit-v2/schedule-edit-v2.component').then(c => c.ScheduleEditV2Component)
  },
  {
    path: ':id/view',
    loadComponent: () => import('./pages/schedule-detail/schedule-detail.component').then(c => c.ScheduleDetailComponent)
  }
];