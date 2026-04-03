import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { NewWorkflowComponent } from './new-workflow/new-workflow.component';
import { SettingsComponent } from './settings/settings.component';
import { DrawflowComponent } from './drawflow/drawflow.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { CAManagerComponent } from './ca/camanager/camanager.component';
import { PluginComponent } from './plugins/plugins.component';
import { LoginComponent } from './login/login.component';
import { MainLayoutComponent } from './main-layout/main-layout.component';
import { ErrorComponent } from './error/error.component';

export const routes: Routes = [
    {
        path: 'login',
        component: LoginComponent
    },
    {
        path: 'error-page',
        component: ErrorComponent
    },
    {
        path: '',
        component: MainLayoutComponent,
        children: [
            { path: '', redirectTo: '/home', pathMatch: 'full' },
            {path: 'home', component: HomeComponent},
            {path: 'metrics', component: DashboardComponent},
            {path: 'ca-manager', component: CAManagerComponent},
            {path: 'plugins', component: PluginComponent},
            {path: 'new-workflow', component: NewWorkflowComponent},
            {path: 'settings', component:SettingsComponent},
            {path: 'drawflow', component: DrawflowComponent},
            {path: 'login', component: LoginComponent}
        ]
    },
    { path: '**', redirectTo: '' }
];
