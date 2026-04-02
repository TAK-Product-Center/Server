import { Component, Output, EventEmitter, Inject } from '@angular/core';
import { NgIf } from '@angular/common';
import { workflow, WorkflowService } from '../workflow.service';
import { MatDialogModule, MAT_DIALOG_DATA} from '@angular/material/dialog';
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { Router } from '@angular/router';  // Import Router service
import { DataService } from '../data.service';

@Component({
  selector: 'app-new-workflow',
  imports: [FormsModule, NgIf, MatDialogModule],
  templateUrl: './new-workflow.component.html',
  styleUrl: './new-workflow.component.css'
})
export class NewWorkflowComponent {
  workflowList: any = []

  // Set the correct presets
  policy: workflow = {
      name: '',
      version: 'v2',
      type: '',
      creatorName: '',
      description: '',
      roleProductSet: '',
      diagramType:  "Workflow",
      federationId: '',
      federationName: ''
  }

  constructor(@Inject(MAT_DIALOG_DATA) public data: any, private dataService: DataService, private workflowService: WorkflowService, private router: Router) {
    this.workflowList = data.workflowList
  }

  existingPolicyName = () => {
    let exists = this.workflowList
      .map((p: any) => p.name)
      .includes(this.policy.name)
      return exists
  }

  onSave() {
    this.workflowService.saveGraph(this.policy).subscribe({
      next: (v) => this.successFunc(v),
      error: (e) => this.failureFunc(e)
    })
  }

  private successFunc(workflowList: Record<string, any>) {
    console.log("Saved: " + workflowList);
    // Go to  editor
    this.dataService.setActivePolicy(this.policy)
    this.router.navigate(['/drawflow'], { queryParams: { policy: this.policy.name }}); 
  }  

  private failureFunc(result: Error) {
    console.log("Failed making new workflow. Error: " + result.message);
  };

}
