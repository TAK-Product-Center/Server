import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { filter, Subscription } from 'rxjs';
import { DataService } from './data.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgbDropdownModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'federation-hub-ui';
  activeLink: string = '';
  routerSubscription: Subscription | undefined;

  constructor(private router: Router, private dataService: DataService) {}

  ngOnInit(): void {
    this.routerSubscription = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.updateActiveLink(event.urlAfterRedirects);
      });

    // Initialize on component load
    this.updateActiveLink(this.router.url);
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  determinePolicyUrl() {
    // this.router.navigate(['/home']);
    if (this.dataService.getActivePolicy()) {
      return '#/drawflow?policy=' + this.dataService.getActivePolicy().name;
    } else {
      return '#'
    }
  }

  updateActiveLink(url: string): void {
    if (url.includes('/metrics')) {
      this.activeLink = 'metrics';
    } else if (url.includes('/ca-manager')) {
      this.activeLink = 'ca-manager';
    } else {
      this.activeLink = 'policy-manager'; // Default if no match
    }
  }
}
