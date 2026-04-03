import { Component, Input, OnInit, Inject} from "@angular/core";
import { CommonModule } from "@angular/common";
import {MatButtonModule} from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA} from '@angular/material/dialog';
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { DataService } from "../../../data.service";


@Component({
  selector: 'app-add-outgoing-connection-modal',
  imports: [CommonModule, MatButtonModule, MatDialogModule, FormsModule],
  templateUrl: './add-outgoing-connection-modal.component.html',
  styleUrl: './add-outgoing-connection-modal.component.css'
})
export class AddOutgoingConnectionModalComponent {    

  certificateFile = null

  stringId : any = undefined

  editorTitle : string = "Add";
  editExisting : boolean = false;
  submitInProgress : boolean = false;
  federation : any = {}

  existingName = () => {
    let exists = this.dataService.getFederationDataForNodes()
      // ignore self
      .filter((f: any) => f.node_id !== this.federation.node_id)
      // map to just an array of existing node names
      .map((f: any) => f.stringId)
      .includes(this.federation.stringId)

      return exists
  }

  constructor(@Inject(MAT_DIALOG_DATA) public data: any, private dataService: DataService) {
    this.federation = JSON.parse(JSON.stringify(data.federation))

    if (this.federation.stringId) {
        this.editorTitle = "Modify";
        this.editExisting = true;
    } else {
        this.federation.tls = true
    }

    if (!this.federation.tokenType) {
        this.federation.tokenType = 'manual';
        this.federation.useToken = false;
    }    
  }

  onSave(federation : any) {
    if (!federation.outgoing_uuid) {
      federation.outgoing_uuid = this.create_UUID()
    }

    federation.outgoingName = federation.stringId + '_' + federation.outgoing_uuid;
    federation.name = federation.outgoingName;

    if (this.data.onSave) {
      this.data.onSave(federation);
    }
  }

  onCancel() {
    if (this.data.onCancel) {
      this.data.onCancel(this.editExisting);
    }
  }

  create_UUID(){
    var dt = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (dt + Math.random()*16)%16 | 0;
        dt = Math.floor(dt/16);
        return (c=='x' ? r :(r&0x3|0x8)).toString(16);
    });
    return uuid;
  }
}
