import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/families',
    pathMatch: 'full'
  },
  {
    path: 'families',
    loadChildren: () => import('./features/families/families.routes').then(m => m.familiesRoutes)
  }
];