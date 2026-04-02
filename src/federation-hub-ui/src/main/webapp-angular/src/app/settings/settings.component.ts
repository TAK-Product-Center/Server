import { Component } from '@angular/core';
import { WorkflowService } from '../workflow.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-settings',
  imports: [],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent {

  constructor(private workflowService: WorkflowService, private toastrService: ToastrService){}

  private successFuncDownloadCa(response: any) {
    let dataType = response.type;
    let binaryData = [];
    binaryData.push(response);
    let downloadLink = document.createElement('a');
    downloadLink.href = window.URL.createObjectURL(new Blob(binaryData, {type: dataType}));
    downloadLink.setAttribute('download', 'ca.pem');
    document.body.appendChild(downloadLink);
    downloadLink.click();
    console.log("Downloaded ca.pem file!")
    this.toastrService.success("Downloaded ca.pem file!")
  }

  private successFuncRestartBroker(response: any) {
    console.log("Federation Hub Broker Process Has Successfully Restarted")
    this.toastrService.success("Federation Hub Broker Process Has Successfully Restarted")
  }

  private failureFunc(error: Error, message: string){
    console.log(error);
    this.toastrService.error(message)
  }

  public downloadSelfCa() {
    this.workflowService.getSelfCa().subscribe({
      next: (v) => this.successFuncDownloadCa(v),
      error: (e) => this.failureFunc(e, "Failed to Load Self CA")
    });
  }

  public restartBroker() {
    this.toastrService.success("Federation Hub Broker Process Restarting - Please wait for it to restart before proceeding.")
    this.workflowService.restartBroker().subscribe({
      next: (v) => this.successFuncRestartBroker(v),
      error: (e) => this.failureFunc(e, "Federation Hub Broker Process Restart Failed")
    });
  }

}
