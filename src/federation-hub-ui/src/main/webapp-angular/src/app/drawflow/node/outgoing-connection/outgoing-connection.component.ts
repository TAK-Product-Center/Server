import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-outgoing-connection',
  imports: [],
  templateUrl: './outgoing-connection.component.html',
  styleUrl: './outgoing-connection.component.css'
})
export class OutgoingConnectionComponent {
  @Input() federation : any = {}
  @Input() properties : any = {}
}
