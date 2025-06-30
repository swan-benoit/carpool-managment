import { Routes } from '@angular/router';

export const familiesRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/family-list/family-list.component').then(c => c.FamilyListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./pages/family-form/family-form.component').then(c => c.FamilyFormComponent)
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./pages/family-form/family-form.component').then(c => c.FamilyFormComponent)
  }
];