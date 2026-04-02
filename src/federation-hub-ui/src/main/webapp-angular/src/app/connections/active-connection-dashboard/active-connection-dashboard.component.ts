import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef } from '@angular/material/dialog';
import { WorkflowService } from '../../workflow.service';
import { MAT_DIALOG_DATA} from '@angular/material/dialog';
import { FormsModule } from '@angular/forms'; // Import FormsModule


@Component({
  selector: 'app-active-connection-dashboard',
  imports: [CommonModule, FormsModule],
  templateUrl: './active-connection-dashboard.component.html',
  styleUrl: './active-connection-dashboard.component.css'
})
export class ActiveConnectionDashboardComponent {
  activeConnections: any = [];
  filteredActiveConnections: any = [];
  selectedCa: any = 'All';
  knownCas: any = [];
  flows: any = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: any, public dialogRef: MatDialogRef<ActiveConnectionDashboardComponent>, private workflowService: WorkflowService) {
    if (data) {
      this.selectedCa = data.selectedCa ? data.selectedCa : 'All';
    }
    
    this.getCaGroups()
    this.pollActiveConnections()
    this.pollDataFlows();
  }

  onClose() {
    this.dialogRef.close()
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
  }
  pollDataFlows() {
    this.workflowService.getFedhubMetrics().subscribe({
      next: (v) => {
        this.flows = v.channelInfos;
      },
      error: (e) => {
        console.log('Error getting getFedhubMetrics',e)
      }
    })
    setTimeout(() => this.pollDataFlows(), 2000);
  }

pollActiveConnections() {
  this.workflowService.getActiveConnections().subscribe({
    next: (v) => {
      this.activeConnections = v
      this.filterActiveConnections()
    },
    error: (e) => {
      console.log('Error getting getActiveConnections',e)
    }
  })
  setTimeout(() => this.pollActiveConnections(), 2000);
}

  disconnect(ac: any) {        
    this.filteredActiveConnections = this.filteredActiveConnections.filter(function( obj: any ) {
      return obj.connectionId !== ac.connectionId;
    });

    this.workflowService.disconnectFederate(ac.connectionId).subscribe({
      next: (v) => {
        
      },
      error: (e) => {
        console.log('Unable to disconnect federate',e)
      }
    })
  };

  filterActiveConnections() {
    let conns: any = []
    this.activeConnections.forEach((activeConnection: any) => {
        activeConnection.groupIdentitiesSet = new Set(activeConnection.groupIdentities)
        activeConnection["bRead"] = 0
        activeConnection["reads"] = 0
        activeConnection["bWritten"] = 0
        activeConnection["writes"] = 0
        if(this.flows) {
            var _flows = this.flows;
            for(var flow of _flows){
                if(flow["sourceId"] == activeConnection.connectionId){
                    activeConnection["bWritten"] = activeConnection["bWritten"] + flow["bytesWritten"]
                    activeConnection["writes"] = activeConnection["writes"] + flow["messagesWritten"]
                    activeConnection["bRead"] = activeConnection["bRead"] + flow["bytesRead"]
                    activeConnection["reads"] = activeConnection["reads"] + flow["messagesRead"]
                }
            }
        }

        if (this.selectedCa === 'All') {
            conns.push(activeConnection)
        } else {
            if (activeConnection.groupIdentitiesSet.has(this.selectedCa))
              conns.push(activeConnection)
        }
    })
    this.filteredActiveConnections = conns
  }
  connectionIdPreview(id: string) {
    if (id.length > 16)
      return id.substring(0,8) + '...' + id.substring(id.length-8);
    else return id;
}
}
