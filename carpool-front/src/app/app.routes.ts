import { Routes } from '@angular/router';
import { Home } from './home/home';
import { FamilyList } from './components/family-list/family-list';
import { FamilyForm } from './components/family-form/family-form';
import { Planning } from './components/planning/planning';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/families',
    pathMatch: 'full'
  },
  {
    path: 'families',
    component: FamilyList
  },
  {
    path: 'families/new',
    component: FamilyForm
  },
  {
    path: 'families/:id/edit',
    component: FamilyForm
  },
  {
    path: 'planning',
    component: Planning
  }
];