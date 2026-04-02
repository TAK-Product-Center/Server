import { Component, Input, OnInit, Inject} from "@angular/core";
import { CommonModule } from "@angular/common";
import {MatButtonModule} from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA} from '@angular/material/dialog';
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { DataService } from "../../../data.service";
import { WorkflowService } from "../../../workflow.service";
import { ToastrModule, ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-add-token-group-modal',
  imports: [CommonModule, MatButtonModule, MatDialogModule, FormsModule, ToastrModule],
  templateUrl: './add-token-group-modal.component.html',
  styleUrl: './add-token-group-modal.component.css'
})
export class AddTokenGroupModalComponent {
  certificateFile = null

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

  constructor(@Inject(MAT_DIALOG_DATA) public data: any, private toastr: ToastrService, private dataService: DataService, private workflowService: WorkflowService) {
    this.federation = JSON.parse(JSON.stringify(data.federation))

    if (this.federation.stringId) {
        this.editorTitle = "Modify";
        this.editExisting = true;
    } else {
      this.federation.tokens = []
      this.federation.expiration = -1
    }
  }

  onSave(federation : any) {
    federation.name = federation.stringId + '_token_group'
    if (this.data.onSave) {
      this.data.onSave(federation);
    }
  }

  onCancel() {
    if (this.data.onCancel) {
      this.data.onCancel(this.editExisting);
    }
  }

previewToken(token: string) {
    return token.substring(0,8) + '...' + token.substring(token.length-8);;
}

copyToken(token: string) {
    navigator.clipboard.writeText(token)
    this.toastr.info("Token Copied to Clipboard!", 'Success', {
      timeOut: 3000,
      positionClass: 'toast-top-center'
    });
}

generateToken() {
    if (!this.editExisting) {
        // growl.error("Please save this node to the graph before generating tokens.");
        this.toastr.error('Please save this node to the graph before generating tokens.', 'Error');
        return
    }

    let now = Date.now();

    let expiration = this.federation.expiration === -1 ? -1 
            : this.federation.expiration + now

    this.workflowService.generateJwtToken({
      clientFingerprint: now +'_' + this.federation.stringId + '_token_fingerprint',
      clientGroup: this.federation.name,
      expiration: expiration
    }).subscribe({
        next: (res: any) => {
          this.federation.tokens.push({
            token: res.token,
            expiration: expiration
        })
          // growl.info("Token generated! Don't forget to save the policy to keep your changes!");
        },
        error: (e) => {
          // growl.error("Error with token request", e);
        }
    })
}

  getDisplayDate(milliseconds: number) {
      return milliseconds === -1 ? -1 : new Date(milliseconds)
  }

  deleteToken(token: string) {
      this.federation.tokens = this.federation.tokens.filter((t: any) => t !== token);
  };
}
