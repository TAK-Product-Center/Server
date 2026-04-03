import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter, Subscription, firstValueFrom } from 'rxjs';
import { DataService } from '../data.service';
import { WorkflowService } from '../workflow.service';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';


@Component({
  selector: 'app-main-layout',
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.css'],
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    NgbDropdownModule,
    MatTooltipModule,
    CommonModule
  ]
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  authChecked = false;
  
  title = 'federation-hub-ui';
  activeLink: string = '';
  routerSubscription: Subscription | undefined;

  constructor(
    private router: Router,
    private dataService: DataService,
    private workflowService: WorkflowService,
    private http: HttpClient
  ) {}

  async ngOnInit() {
    await firstValueFrom(this.http.get('/api/isAdmin'));

    this.authChecked = true;

    // keep the same behavior you had in AppComponent: router event tracking
    this.routerSubscription = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.updateActiveLink(event.urlAfterRedirects);
      });

    // Initialize on component load
    this.updateActiveLink(this.router.url);

    // same logic to fetch active enforced policy and navigate to drawflow if found
    if (!this.dataService.getActiveEditingPolicy()) {
      try {
        const res = await firstValueFrom(this.workflowService.getActiveEnforcedPolicy());
        if (res && res.name) {
          this.dataService.setActiveEditingPolicy(res);
          this.router.navigate(['/drawflow']);
        }
      } catch (e) {
        console.error(e);
      }
    }
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  determinePolicyUrl() {
    if (this.dataService.getActiveEditingPolicy()) {
      return '/drawflow';
    } else {
      return '/';
    }
  }

  isPolicyActive() {
    return this.dataService.getActiveEditingPolicy();
  }

  updateActiveLink(url: string): void {
    if (url.includes('/metrics')) {
      this.activeLink = 'metrics';
    } else if (url.includes('/ca-manager')) {
      this.activeLink = 'ca-manager';
    } else if (url.includes('/plugins')) {
      this.activeLink = 'plugins';
    } else {
      this.activeLink = 'policy-manager'; // Default if no match
    }
  }
}
