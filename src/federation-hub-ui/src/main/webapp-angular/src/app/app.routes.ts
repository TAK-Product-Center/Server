import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { NewWorkflowComponent } from './new-workflow/new-workflow.component';
import { SettingsComponent } from './settings/settings.component';
import { DrawflowComponent } from './drawflow/drawflow.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { CAManagerComponent } from './ca/camanager/camanager.component';

export const routes: Routes = [
    { path: '', redirectTo: '/home', pathMatch: 'full' },
    {path: 'home', component: HomeComponent},
    {path: 'metrics', component: DashboardComponent},
    {path: 'ca-manager', component: CAManagerComponent},
    {path: 'new-workflow', component: NewWorkflowComponent},
    {path: 'settings', component:SettingsComponent},
    {path: 'drawflow', component: DrawflowComponent}
];
