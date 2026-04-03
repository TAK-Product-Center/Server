import { Component, Input, OnInit, Inject} from "@angular/core";
import { CommonModule } from "@angular/common";
import {MatButtonModule} from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA} from '@angular/material/dialog';
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { DataService } from "../../../data.service";
import { WorkflowService } from "../../../workflow.service";


@Component({
  selector: 'app-add-edge-modal',
  imports: [CommonModule, MatButtonModule, MatDialogModule, FormsModule],
  templateUrl: './add-edge-modal.component.html',
  styleUrl: './add-edge-modal.component.css'
})
export class AddEdgeModalComponent {
  outboundGroup : string = ''

  knownOutboundGroups : string[] = []

  editorTitle : string = "Add";
  editExisting : boolean = false;
  submitInProgress : boolean = false;
  edge : any = {}

  constructor(@Inject(MAT_DIALOG_DATA) public data: any, private dataService: DataService, private workflowService: WorkflowService) {
    this.edge = JSON.parse(JSON.stringify(data.connection))
    
    if (data.connection.federation) {
      this.editExisting = true;
    } else {
      this.edge.federation = {
        name: data.connection.sourceStringId + ' -> ' + data.connection.destinationStringId,
        groupsFilterType: 'allGroups',
        allowedGroups: [],
        disallowedGroups: []
      }
    }
    this.workflowService.getKnownGroupsForNode(this.edge.source).subscribe({
        next: (res: any) => {
          this.knownOutboundGroups = res
        },
        error: (e) => {
          console.log('Error getKnownGroupsForNode', e)
        }
    })

  }

  onSave(edge : any) {
    if (this.data.onSave) {
      this.data.onSave(edge);
    }
  }

  onCancel() {
    if (this.data.onCancel) {
      this.data.onCancel(this.editExisting);
    }
  }

  allowGroup(allowedGroup: string) {
    if (this.edge.federation.allowedGroups.indexOf(allowedGroup) == -1) {
         this.edge.federation.allowedGroups.push(allowedGroup)
     }
     this.deleteDisallowedGroup(allowedGroup)
     this.outboundGroup = ''
 }

 deleteAllowedGroup(allowedGroup: string) {
     this.edge.federation.allowedGroups = this.edge.federation.allowedGroups.filter((group: string) => group !== allowedGroup)
 }

 disallowGroup(disallowedGroup: string) {
     if (this.edge.federation.disallowedGroups.indexOf(disallowedGroup) == -1) {
         this.edge.federation.disallowedGroups.push(disallowedGroup)
     }
     this.deleteAllowedGroup(disallowedGroup)
     this.outboundGroup = ''
 }

  deleteDisallowedGroup(disallowedGroup: string) {
     this.edge.federation.disallowedGroups = this.edge.federation.disallowedGroups.filter((group: string) => group !== disallowedGroup)
 }
}
