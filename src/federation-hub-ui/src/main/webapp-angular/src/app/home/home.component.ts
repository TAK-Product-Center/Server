import { Component, inject } from '@angular/core';
import { LocalStorageService } from '../local-storage.service';
import { WorkflowService } from '../workflow.service';
import { NewWorkflowComponent } from '../new-workflow/new-workflow.component';
import { NgIf, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog} from '@angular/material/dialog';
import { DataService } from '../data.service';
import { Router } from '@angular/router';  // Import Router service

@Component({
  selector: 'app-home',
  imports: [NewWorkflowComponent, NgIf, CommonModule, FormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  readonly dialog = inject(MatDialog);

  workflowList: any[] = [];
  displayedCollection: any[] = [];
  diagramType = "Workflow";
  mode = "load"
  selectedPolicy: any = {};
  itemSelected: boolean = false;
  searchTerm: string = '';

  constructor(private localStoreageService: LocalStorageService, private workflowService: WorkflowService, private dataService: DataService, private router: Router) {
    dataService.setActivePolicy(undefined)
    workflowService.getWorkflowDescriptors().subscribe({
      next: (v) => this.successFunc(v),
      error: (e) => this.failureFunc(e)
    })
  }

  private successFunc(workflowList: any[]) {
    this.workflowList = workflowList;
    this.displayedCollection = this.workflowList;
  };

  private failureFunc(result: Error) {
    console.log("Failed getting workflow names. Error: " + result.message);
  };

  public filterItems() {
    if (!this.searchTerm) {
      this.displayedCollection = [...this.workflowList];
    } else {
      this.displayedCollection = this.workflowList.filter(item => {
        return item.name.toLowerCase().includes(this.searchTerm.toLowerCase())
      });
    }
  }

  public selectRow(row:any){
    this.selectedPolicy = row;
    this.itemSelected = true;
  }

  public onNew() {
    this.dialog.open(NewWorkflowComponent, {
      autoFocus: false,
      restoreFocus: false,
      height: '300px',
      width: '600px',
      data: { workflowList: this.workflowList }
    });
  }

  public onLoad() {
    this.router.navigate(['/drawflow'], { queryParams: { policy: this.selectedPolicy.name }}); 
  }
}
