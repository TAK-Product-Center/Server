import { Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class DataService {
  editor!: any;
  policy: any;
  policyIdFromRoute: any;

  constructor(private route: ActivatedRoute) {
    this.route.queryParamMap.subscribe((params: any) => {
      this.policyIdFromRoute = params.get('policy');
    });
   }

  setEditor(e : any) {
    this.editor = e;
  }

  getEditor() {
    return this.editor;
  }

  getFederationDataForNodes() {
    let vals : any = Object.values(this.editor.drawflow.drawflow.Home.data);
    return vals.map((n: { federation: any; }) => n.federation).filter((f: any) => f != undefined);
  }

  getFederationDataForCaNodes() {
    let vals : any = Object.values(this.editor.drawflow.drawflow.Home.data);
    return vals.filter((n: any) => n.name === 'GroupCell').map((n: { federation: any; }) => n.federation).filter((f: any) => f != undefined);
  }

  setActivePolicy(policy: any) {
    this.policy = policy;
  }

  getActivePolicy() {
    return this.policy;
  }

  getPolicyIdFromRoute() {
    return this.policyIdFromRoute
  }
}
