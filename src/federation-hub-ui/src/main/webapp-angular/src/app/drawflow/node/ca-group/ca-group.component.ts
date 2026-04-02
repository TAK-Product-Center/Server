import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ca-group',
  imports: [],
  templateUrl: './ca-group.component.html',
  styleUrl: './ca-group.component.css'
})
export class CaGroupComponent {
  @Input() federation : any = {}
  @Input() properties : any = {}
}
