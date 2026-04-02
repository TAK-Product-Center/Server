import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';

export const routes: Routes = [
    { path: '', redirectTo: '/metrics', pathMatch: 'full' },
    {path: 'metrics', component: DashboardComponent}
];
