import { Component, inject } from '@angular/core';
import { DataService } from '../../data.service';
import { WorkflowService } from '../../workflow.service';
import { MatDialog} from '@angular/material/dialog';
import { ActiveConnectionDashboardComponent } from '../../connections/active-connection-dashboard/active-connection-dashboard.component';
import { ToastrModule, ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-toolbar',
  imports: [ToastrModule],
  templateUrl: './toolbar.component.html',
  styleUrl: './toolbar.component.css'
})
export class ToolbarComponent {
  title = 'Policy'
  readonly dialog = inject(MatDialog);

  constructor(private toastr: ToastrService, private dataService: DataService, private workflowService: WorkflowService) {}

  ngOnInit() {
    let policy = this.dataService.getActivePolicy()
    if (!policy) {
      let policyId = this.dataService.getPolicyIdFromRoute();
      if (policyId) {
        this.workflowService.loadGraph(policyId).subscribe({
          next: (res) => {
            if (res && res.name) {
              this.title = res.name
            }
          },
          error: (e) => console.log(e)
       })
      }
    } else {
      this.title = policy.name
    }
  }

  undo(event: MouseEvent) {
    event.preventDefault();
  }

  redo(event: MouseEvent) {
    event.preventDefault();
  }
  exportToJson(event: MouseEvent) {
    event.preventDefault();

    let policy: any = {}
    policy.cells = this.dataService.getEditor().exportCells()
    policy.settings = this.dataService.getEditor().exportSettings()

    // Convert the object to a JSON string
    const jsonString = JSON.stringify(policy, null, 2); // `null, 2` for pretty-printing

    // Create a Blob with the JSON string
    const blob = new Blob([jsonString], { type: 'application/json' });

    // Create a link element to trigger the download
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'fedhub_ui_policy.json';  // Specify the file name
    link.click();  // Programmatically click the link to trigger the download

    // Clean up the object URL after the download is triggered
    URL.revokeObjectURL(link.href);

    this.toastr.info("Downloaded file to fedhub_ui_policy.json", 'Success', {
      timeOut: 3000,
      positionClass: 'toast-top-center'
    });
  }

  loadFromJson(event: MouseEvent) {
    event.preventDefault();
    let jsonFileInput = document.getElementById('json-file-input');
    if (jsonFileInput)
      jsonFileInput.click();
  }

  onFileSelected(event : any) {
    const file = event.target.files[0];
    const dataService = this.dataService
    if (file) {
      const reader = new FileReader();
  
      // This will read the file as text (JSON string)
      reader.readAsText(file);
  
      // Event handler for when the file is successfully read
      reader.onload = (event) => {
        const result = event.target?.result;
        if (typeof result === 'string') {
          try {
            // Parse the JSON string into a JavaScript object
            const policy = JSON.parse(result);
            const cells = policy.cells
            const settings = policy.settings

            let graph : any = {
              drawflow: { Home: { data: {} } }
            };
            debugger;
            for (let cell of cells) {
              if (cell.graphType === 'GroupCell' || cell.graphType === 'PolicyTextAnnotation' || cell.graphType === 'FederationTokenGroupCell' || cell.graphType === 'FederationOutgoingCell') {
                graph['drawflow']['Home']['data'][cell.id] = cell
              }
            }
            
            dataService.getEditor().import(graph);
            dataService.getEditor().applySettings(settings)
            this.toastr.info("Successfully uploaded JSON file", 'Success', {
              timeOut: 3000,
              positionClass: 'toast-top-center'
            });
          } catch (error) {
            console.error('Error parsing JSON:', error);
            
            this.toastr.error("Invalid JSON file", 'Error', {
              timeOut: 3000,
              positionClass: 'toast-top-center'
            });
          }
        }
      };
  
      // Event handler for errors
      reader.onerror = (event) => {
        console.error('Error reading file:', event.target?.error);
        this.toastr.error("Invalid JSON file", 'Error', {
          timeOut: 3000,
          positionClass: 'toast-top-center'
        });
      };
    }
  }
  
  export(event: MouseEvent) {
    event.preventDefault();
  }

  setAndSavePolicy(event: MouseEvent) {
    event.preventDefault();

    let policy = this.dataService.getActivePolicy()
    policy.cells = this.dataService.getEditor().exportCells()
    policy.settings = this.dataService.getEditor().exportSettings()

    this.workflowService.saveGraph(policy).subscribe({
      next: (v) => {
        this.workflowService.updateFederationManagerAndFile(policy.name).subscribe({
          next: (v) => {
            this.toastr.info("Successfully Set and Saved Policy", 'Success', {
              timeOut: 3000,
              positionClass: 'toast-top-center'
            });
          },
          error: (e) => { 
            console.log('e',e)
            this.toastr.error("Unable to set policy", 'Error', {
              timeOut: 3000,
              positionClass: 'toast-top-center'
            });
          }
        })
      },
      error: (e) => { 
        console.log('e',e)
        this.toastr.error("Unable to set policy", 'Error', {
          timeOut: 3000,
          positionClass: 'toast-top-center'
        });
      }
    })
  }

  setPolicy(event: MouseEvent) {
    event.preventDefault();

    let policy = this.dataService.getActivePolicy()
    policy.cells = this.dataService.getEditor().exportCells()
    policy.settings = this.dataService.getEditor().exportSettings()

    this.workflowService.saveGraph(policy).subscribe({
      next: (v) => {
        this.toastr.info("Successfully Set Policy", 'Success', {
          timeOut: 3000,
          positionClass: 'toast-top-center'
        });
      },
      error: (e) => { 
        console.log('e',e)
        this.toastr.error("Unable to set policy", 'Error', {
          timeOut: 3000,
          positionClass: 'toast-top-center'
        });
      }
    })
  }

  showActiveConnections(event: MouseEvent) {
    event.preventDefault();
    this.dialog.open(ActiveConnectionDashboardComponent, {
      autoFocus: false,
      restoreFocus: false,
      height: '30vw',
      width: '80vw',
      maxWidth: '80vw'
    });
  }
}
