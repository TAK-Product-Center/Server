import { PluginEditComponent } from "./plugin-edit/plugin-edit.component";
import { Component, Input, OnInit, inject, Inject} from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { WorkflowService } from "../workflow.service";
import { MatDialog } from '@angular/material/dialog';
import { DataService } from "../data.service";
import { Router } from '@angular/router';

interface Plugin {
  name: string;
  version: string;
  description: string;
  type: 'sender' | 'receiver' | 'interceptor';
  disabled: boolean;
  uptime: number;
  startTime: string;
  author?: string;
  tags?: string[];
}

@Component({
  selector: 'app-plugins',
  imports: [CommonModule, FormsModule],
  templateUrl: './plugins.component.html',
  styleUrl: './plugins.component.css'
})
export class PluginComponent implements OnInit {
  readonly dialog = inject(MatDialog);

  plugins: Plugin[] = [];
  filteredPlugins: Plugin[] = [];
  
  pluginConfigs: any = {}

  loading = false

  showOnlyActive = false;  // renamed from showOnlyRunning
  sortBy: 'name' | 'disabled' | 'uptime' = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(private workflowService: WorkflowService, private dataService: DataService, private router: Router) {}
  
  ngOnInit(): void {
    if (!this.dataService.getActiveEditingPolicy())
      this.router.navigate(['/home']);

    let policy = this.dataService.getActiveEditingPolicy()
    policy?.pluginsData ?? {};
    policy?.pluginsData?.plugins ?? [];

    this.pluginConfigs = Object.fromEntries(
      (policy?.pluginsData?.plugins || [])
        .filter((p: any) => p?.pluginName)
        .map((p: any) => [p.pluginName, p])
    );

    this.loading = true;
    this.loadPlugins();
  }

  ngOnDestroy() {
  }

  openEditModal(plugin: any) {
    const dialogRef = this.dialog.open(PluginEditComponent, {
      width: '800px',
      data: { plugin }, // pass plugin into modal
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        console.log('Destinations updated:', result);
        // TODO: Save changes to backend
      }
    });
  }
  
  loadPlugins() {
    this.loading = true;
    // setTimeout(() => {
    //   this.plugins = Array.from({ length: 3 }).map((_, i) => ({
    //     name: `Plugin ${i + 1}`,
    //     version: `1.${i % 3}.${i}`,
    //     description: `This is a sample plugin used for development and layout testing.`,
    //     state: (['running', 'paused', 'stopped'] as const)[i % 3],
    //     uptime: this.parseStartTimeMillis(Date.now().toString()),
    //     type: i%3 == 0 ? 'sender' : i%3 == 1 ? 'receiver' : 'interceptor',
    //     author: `Dev Team ${i % 5 + 1}`,
    //     tags: ['test', 'dev', `group-${i % 3}`],
    //     startTime: Date.now().toString()
    //   }));
    //   this.applyFilters();
    //   this.loading = false;
    // }, 1000);

    this.workflowService.getRegisteredPlugins().subscribe({
      next: (v) => {
        this.plugins = v.map((item: any) => ({
          ...item,
          disabled: this.pluginConfigs[item.name]?.disabled ?? false,
          type: item.type.toLowerCase(),
          uptime: this.parseStartTimeMillis(item.startTimeMillis)
        }));
        this.applyFilters();
        this.loading = false;
      },
      error: (e) => {
        this.loading = false;
        console.log('Error getting plugins', e);
      }
    })
  }
  
  applyFilters() {
    let result = [...this.plugins];
  
    if (this.showOnlyActive) {
      result = result.filter(p => !p.disabled);
    }
  
    result.sort((a, b) => {
      let compare = 0;
      switch (this.sortBy) {
        case 'name':
          compare = a.name.localeCompare(b.name);
          break;

        case 'disabled':
          compare = (a.disabled === b.disabled) ? 0 : (a.disabled ? 1 : -1);
          break;

        case 'uptime':
          compare = b.uptime - a.uptime;
          break;
      }
  
      return this.sortDirection === 'asc' ? compare : -compare;
    });
  
    this.filteredPlugins = result;
  }
  
  parseStartTimeMillis(uptime: string): number {
    return Date.now() - Number(uptime)
  }

  togglePlugin(plugin: Plugin) {
    plugin.disabled = !plugin.disabled
    this.applyFilters();

    let policy = this.dataService.getActiveEditingPolicy()
    policy.pluginsData ??= {};
    policy.pluginsData.plugins ??= [];

    const index = policy.pluginsData.plugins.findIndex((p: any) => p.pluginName === plugin.name);

    if (index !== -1) {
      console.log('here1',  JSON.parse(JSON.stringify(policy.pluginsData.plugins[index])))
      policy.pluginsData.plugins[index].disabled = plugin.disabled
      console.log('here2',  JSON.parse(JSON.stringify(policy.pluginsData.plugins[index])))
    } else {
      console.log('here3',  policy.pluginsData.plugins[index])
      policy.pluginsData.plugins.push({
        pluginName: plugin.name,
        sendTo: [],
        dontSend: [],
        receiveFrom: [],
        dontReceive: [],
        sendByDefault: false,
        receiveByDefault: false,
        disabled: plugin.disabled
      });
    }

    console.log('send',policy)

    this.workflowService.saveFederationPluginsPolicy(policy).subscribe({
      next: (v) => {
        this.dataService.setActiveEditingPolicy(v)
      },
      error: (e) => {
        console.log('err', e)
      }
    })
  }
  
  toggleSortDirection() {
    this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    this.applyFilters();
  }

  formatUptime(ms: number) {
    if (!ms || ms < 0) return '—';
  
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60) % 60;
    const hours = Math.floor(seconds / 3600) % 24;
    const days = Math.floor(seconds / 86400);
  
    const parts = [];
    if (days > 0) parts.push(`${days}d`);
    if (hours > 0 || days > 0) parts.push(`${hours}h`);
    if (minutes > 0 || hours > 0 || days > 0) parts.push(`${minutes}m`);
  
    return parts.length ? parts.join(' ') : '<1m';
  }
}
