import { Component, Input } from '@angular/core';

@Component({
  selector: 'df-grouping',
  imports: [],
  templateUrl: './grouping.html',
  styleUrl: './grouping.css'
})
export class Grouping {
  @Input() federation : any = {}
  @Input() properties : any = {}
}


