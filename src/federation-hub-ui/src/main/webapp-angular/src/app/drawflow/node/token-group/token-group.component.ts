import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-token-group',
  imports: [CommonModule],
  templateUrl: './token-group.component.html',
  styleUrl: './token-group.component.css'
})
export class TokenGroupComponent {
  @Input() federation : any = {}
  @Input() properties : any = {}
}
