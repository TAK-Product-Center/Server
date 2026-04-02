import { Component, Input, OnInit, inject, Inject} from "@angular/core";
import { CommonModule } from "@angular/common";
import {MatButtonModule} from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA} from '@angular/material/dialog';
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { DataService } from "../../../data.service";
import { WorkflowService } from "../../../workflow.service";
import { MatDialog} from '@angular/material/dialog';
import { ActiveConnectionDashboardComponent } from "../../../connections/active-connection-dashboard/active-connection-dashboard.component";

@Component({
  selector: 'app-add-ca-group-modal',
  imports: [CommonModule, MatButtonModule, MatDialogModule, FormsModule],
  templateUrl: './add-ca-group-modal.component.html',
  styleUrl: './add-ca-group-modal.component.css',
})
export class AddCAGroupModalComponent {
  readonly dialog = inject(MatDialog);

  certificateFile = null
  uploadProgress: number = 0;
  uploadSuccess: boolean | null = null;
  
  stringId : any = undefined

  editorTitle : string = "Add";
  editExisting : boolean = false;
  submitInProgress : boolean = false;
  federation : any = {}
  activeConnections = [];
  filters = [];
  knownCas : any;

  existingName = () => {
    let exists = this.dataService.getFederationDataForNodes()
      // ignore self
      .filter((f: any) => f.node_id !== this.federation.node_id)
      // map to just an array of existing node names
      .map((f: any) => f.stringId)
      .includes(this.federation.stringId)

      return exists
  }

  existingCaNameIdentity = () => {
    let exists = this.dataService.getFederationDataForCaNodes()
      // ignore self
      .filter((f: any) =>  f.node_id !== this.federation.node_id)
      // map to just an array of existing node names
      .map((f: any) => f.name)
      .includes(this.federation.name)

      return exists
  }

  constructor(@Inject(MAT_DIALOG_DATA) public data: any, private dataService: DataService, private workflowService: WorkflowService) {
    JSON.parse(JSON.stringify(this.federation = data.federation))

    if (this.federation.stringId) {
        this.editorTitle = "Modify";
        this.editExisting = true;
    }

    this.getCaGroups();
  }

  onSave(federation : any) {
    if (this.data.onSave) {
      this.data.onSave(federation);
    }
  }

  onCancel() {
    if (this.data.onCancel) {
      this.data.onCancel(this.editExisting);
    }
  }

  getCaGroups() {
    this.workflowService.getKnownCaGroups().subscribe({
      next: (v) => {
        this.knownCas = v
      },
      error: (e) => {
        console.log('Error getting getKnownCaGroups',e)
      }
    })
  };
  
  showActiveConnections() {
    this.dialog.open(ActiveConnectionDashboardComponent, {
      autoFocus: false,
      restoreFocus: false,
      height: '30vw',
      width: '80vw',
      maxWidth: '80vw',
      data: { 
        selectedCa: this.federation.name
      }
    });
  }
}
