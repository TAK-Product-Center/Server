import { Component, Inject, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTabsModule } from '@angular/material/tabs';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { DataService } from "../../data.service";
import { WorkflowService } from "../../workflow.service";

@Component({
  selector: 'app-plugin-edit',
  templateUrl: './plugin-edit.component.html',
  styleUrls: ['./plugin-edit.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatTabsModule]
})
export class PluginEditComponent implements OnInit {
  sendByDefault = false;
  receiveByDefault = false;

  // Data source sets
  dontSend: any[] = [];
  send: any[] = [];
  dontReceive: any[] = [];
  receive: any[] = [];
  disabled: boolean = false;

  constructor(
    public dialogRef: MatDialogRef<PluginEditComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { plugin: any },
    private dataService: DataService,
    private workflowService: WorkflowService
  ) {}

  ngOnInit(): void {
    const policy = this.dataService.getActiveEditingPolicy();
    const pluginConf = this.data?.plugin ?? {};
    const pluginPolicy = policy.pluginsData?.plugins?.find((p: any) => p.pluginName === pluginConf.name);

    const pluginType = pluginConf?.type ?? '';

    // Collect available nodes
    const availableNodes = Object.values(
      this.dataService.getEditor().drawflow.drawflow.Home.data || {}
    )
      .map((n: any) => n.federation)
      .filter((n: any) => !!n)
      .map((n: any) => ({ name: n.name, stringId: n.stringId }));

    // Helper: check if a node exists
    const nodeExists = (name: string) =>
      availableNodes.some((n) => n.name === name);

    // if we have an existing policy, set it
    let sendSet = new Set();
    let dontSendSet = new Set();
    let receiveSet = new Set();
    let dontReceiveSet = new Set();
    if (pluginPolicy) {
      this.disabled = pluginPolicy.disabled
      
      this.sendByDefault = pluginPolicy.sendByDefault
      this.receiveByDefault = pluginPolicy.receiveByDefault

      const sendTo: string[] = (pluginPolicy.sendTo ?? []).filter(nodeExists);
      const dontSend: string[] = (pluginPolicy.dontSend ?? []).filter(nodeExists);
      const receiveFrom: string[] = (pluginPolicy.receiveFrom ?? []).filter(nodeExists);
      const dontReceive: string[] = (pluginPolicy.dontReceive ?? []).filter(nodeExists);
      sendSet = new Set(sendTo);
      dontSendSet = new Set(dontSend);
      receiveSet = new Set(receiveFrom);
      dontReceiveSet = new Set(dontReceive);
    }

    // Process all available nodes
    for (const node of availableNodes) {
      const name = node.name;

      // --- Sending logic ---
      if (pluginType === 'sender' || pluginType === 'interceptor') {
        if (sendSet.has(name)) {
          this.send.push(node);
        } else if (dontSendSet.has(name)) {
          this.dontSend.push(node);
        } else if (this.sendByDefault) {
          this.send.push(node);
        } else {
          this.dontSend.push(node);
        }
      }

      // --- Receiving logic ---
      if (pluginType === 'receiver' || pluginType === 'interceptor') {
        if (receiveSet.has(name)) {
          this.receive.push(node);
        } else if (dontReceiveSet.has(name)) {
          this.dontReceive.push(node);
        } else if (this.receiveByDefault) {
          this.receive.push(node);
        } else {
          this.dontReceive.push(node);
        }
      }
    }
  }

  // ---- Shared shuttle logic ----
  moveRight(source: any[], target: any[], item: any) {
    const idx = source.findIndex(d => d.stringId === item.stringId);
    if (idx > -1) {
      source.splice(idx, 1);
      target.push(item);
    }
  }

  moveLeft(source: any[], target: any[], item: any) {
    this.moveRight(target, source, item);
  }

  moveAllRight(source: any[], target: any[]) {
    target.push(...source);
    source.length = 0;
  }

  moveAllLeft(source: any[], target: any[]) {
    this.moveAllRight(target, source);
  }

  // ---- Save / Cancel ----
  save() {
    const payload: any = {
      pluginName: this.data.plugin.name,
      sendTo: this.send.map(node => node.name),
      dontSend: this.dontSend.map(node => node.name),
      receiveFrom: this.receive.map(node => node.name),
      dontReceive: this.dontReceive.map(node => node.name),
      sendByDefault: this.sendByDefault,
      receiveByDefault: this.receiveByDefault,
      disabled: this.disabled
    };

    let policy = this.dataService.getActiveEditingPolicy()
    policy.pluginsData ??= {};
    policy.pluginsData.plugins ??= [];

    const index = policy.pluginsData.plugins.findIndex((p: any) => p.pluginName === payload.pluginName);

    if (index !== -1) {
      policy.pluginsData.plugins[index] = payload;
    } else {
      policy.pluginsData.plugins.push(payload);
    }

    this.workflowService.saveFederationPluginsPolicy(policy).subscribe({
      next: (v) => {
        this.dataService.setActiveEditingPolicy(v)
      },
      error: (e) => {
        console.log('err', e)
      }
    })
    
    this.dialogRef.close(payload);
  }

  cancel() {
    this.dialogRef.close();
  }
}
