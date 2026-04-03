import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';


@Component({
  selector: 'app-error',
  templateUrl: './error.component.html',
  imports: [RouterModule],
  styleUrl: './error.component.css'
})
export class ErrorComponent {
  message: string = 'An unknown error occurred';

  constructor(private router: Router) {
    const nav = this.router.getCurrentNavigation();
    this.message = nav?.extras?.state?.['message'] || this.message;
  }

  reAuth() {
    window.location.href = '/api/oauth/login/auth?force=true';
  }
}
