import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DataService {
  editor!: any;
  policy: any;
  pluginsPolicy: any = {};

  setEditor(e : any) {
    this.editor = e;
  }

  getEditor() {
    return this.editor;
  }

  setPluginsPolicy(policy: any) {
    this.pluginsPolicy = policy
  }

  getPluginsPolicy() {
    return this.pluginsPolicy
  }

  getFederationDataForNodes() {
    let vals : any = Object.values(this.editor.drawflow.drawflow.Home.data);
    return vals.map((n: { federation: any; }) => n.federation).filter((f: any) => f != undefined);
  }

  getFederationDataForCaNodes() {
    let vals : any = Object.values(this.editor.drawflow.drawflow.Home.data);
    return vals.filter((n: any) => n.name === 'GroupCell').map((n: { federation: any; }) => n.federation).filter((f: any) => f != undefined);
  }

  setActiveEditingPolicy(policy: any) {
    console.log('setActiveEditingPolicy', policy)
    this.policy = policy;
  }

  getActiveEditingPolicy() {
    return this.policy;
  }
}
