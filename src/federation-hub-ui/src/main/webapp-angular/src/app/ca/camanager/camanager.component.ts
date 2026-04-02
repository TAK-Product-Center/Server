import { Component, Input, OnInit, inject, Inject} from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { WorkflowService } from "../../workflow.service";
import { DataService } from "../../data.service";
import { ToastrModule, ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-camanager',
  imports: [CommonModule, FormsModule],
  templateUrl: './camanager.component.html',
  styleUrl: './camanager.component.css'
})
export class CAManagerComponent {
  certificateFile = null
  uploadProgress: number = 0;
  uploadSuccess: boolean | null = null;
  submitInProgress : boolean = false;

  knownCas : any;
  
  constructor(private workflowService: WorkflowService, private toastr: ToastrService, private dataService: DataService) {
    this.getCaGroups();
  }

  getCaGroups() {
    this.workflowService.getKnownCaGroups().subscribe({
      next: (v) => {
        this.knownCas = [...v]
      },
      error: (e) => {
        console.log('Error getting getKnownCaGroups',e)
      }
    })
  };

  deleteCA(ca : any) {

    if (!confirm("Are you sure you want to delete this CA?")) {
      return;
    }

    let editor = this.dataService.getEditor()

    if (!editor) {
      console.log('no active policy, remvoing ca from truststore but not graph')
    } else {
      // find any active nodes on the graph using the deleted ca
      const activeCANodes = Object.values(editor.drawflow.drawflow.Home.data).filter((node: any) => node.federation.name === ca.uid);
      
      // automatically remove the active deleted ca and save
      activeCANodes.forEach((node: any) => editor.removeNodeId(`node-${node.id}`))

      let policy = this.dataService.getActiveEditingPolicy()
      policy['graphData']['nodes'] = this.dataService.getEditor().exportCells()
      policy.settings = this.dataService.getEditor().exportSettings()
      
      this.workflowService.saveFederationGraphPolicy(policy).subscribe({
        next: (v) => {
          this.dataService.setActiveEditingPolicy(v)
          this.workflowService.activateFederationPolicy(policy.name).subscribe({
            next: (v) => {},
            error: (e) => {}
          })
        },
        error: (e) => {}
      })
    }

    this.workflowService.deleteGroupCa(ca.uid).subscribe({
      next: (v) => {
        this.getCaGroups();
        this.toastr.info("Removed " + ca.uid, 'Success', {
          timeOut: 3000,
          positionClass: 'toast-top-center'
        });
      },
      error: (e) => {
        this.toastr.error("Error Deleting " + ca.uid, 'Error', {
          timeOut: 3000,
          positionClass: 'toast-top-center'
        });
        console.log('Error deleteCA', ca, e)
      }
    })
  }

  onFileChange(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.certificateFile = file;
    }
  }

  uploadFile(fileInput: HTMLInputElement) {
    if (this.certificateFile == null) {
      alert('Please provide a valid certificate file before submitting.');
    } else {
      var fd = new FormData();
      fd.append('file', this.certificateFile);

      this.submitInProgress = true;

      this.workflowService.uploadCAFile(fd).subscribe({
        next: (v) => {
          setTimeout(() => {
            this.getCaGroups();
          }, 3000);
          this.submitInProgress = false;
          this.certificateFile = null
          fileInput.value = '';
          this.toastr.info("Uploaded CA", 'Success', {
            timeOut: 3000,
            positionClass: 'toast-top-center'
          });
        },
        error: (e) => {
          this.toastr.error("Error Uploading CA ", 'Error', {
            timeOut: 3000,
            positionClass: 'toast-top-center'
          });
          this.submitInProgress = false;
          this.certificateFile = null
          fileInput.value = '';
        }
      })
    }
  };

}
