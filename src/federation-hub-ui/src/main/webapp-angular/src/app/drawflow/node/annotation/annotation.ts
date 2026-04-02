import { Component, Input } from '@angular/core';

@Component({
  selector: 'df-annotation',
  imports: [],
  templateUrl: './annotation.html',
  styleUrl: './annotation.css'
})
export class Annotation {
  @Input() federation : any = {}
  @Input() properties : any = {}
}


