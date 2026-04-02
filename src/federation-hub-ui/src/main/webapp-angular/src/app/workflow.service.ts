import { Injectable } from '@angular/core';
import { ConfigService } from './config.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class WorkflowService {

  constructor(private configService: ConfigService, private http: HttpClient) { }

  /**
   * Get Workflow Descriptors
   *
   * @return {Array}
   */
  public getWorkflowDescriptors(): Observable<any> {
    var url = this.configService.getServerBaseUrlStr() + 'federations';
    return this.http.get(url)
  };

  public saveGraph(graphJson: any): Observable<any> {
    graphJson.version = 'v2'
    var url = this.configService.getServerBaseUrlStr() +'saveFederation'
    return this.http.post(url, graphJson)
  }

  public loadGraph(id: any): Observable<any> {
    var url = this.configService.getServerBaseUrlStr() +'federation/' + id
    return this.http.get(url)
  }

  public updateFederationManagerAndFile(id: any): Observable<any> {
    var url = this.configService.getServerBaseUrlStr() +'updateFederationManagerAndFile/' + id
    return this.http.get(url)
  }

  public getKnownGroupsForNode(id: any): Observable<any> {
    var url = this.configService.getServerBaseUrlStr() +'getKnownGroupsForGraphNode/' + id
    return this.http.get(url)
  }

  public getSelfCa(): Observable<any> {
    var url =  this.configService.getServerBaseUrlStr() + 'getSelfCaFile/';
    return this.http.get(url, { responseType: 'text' });
  };

  public getKnownCaGroups(): Observable<any> {
    var url =  this.configService.getServerBaseUrlStr() + 'getKnownCaGroups/';
    return this.http.get(url, { responseType: 'json' });
  };

  public deleteGroupCa(ca:string): Observable<any> {
    var url =  this.configService.getServerBaseUrlStr() + 'deleteGroupCa/' + ca;
    return this.http.delete(url, { responseType: 'json' });
  };

  public restartBroker(): Observable<any> {
    var url =  this.configService.getServerBaseUrlStr() + 'restartBroker/';
    return this.http.get(url);
  };

  public generateJwtToken(tokenRequest: any) {
    var url =  this.configService.getServerBaseUrlStr() + 'generateJwtToken/';
    return this.http.post(url, tokenRequest)
  }

  public disconnectFederate(connectionId: any) {
    var url = this.configService.getServerBaseUrlStr() + 'disconnectFederate/' + connectionId;
    return this.http.delete(url)
  };

  public getFedhubMetrics(): Observable<any> {
    var url =  this.configService.getServerBaseUrlStr() + 'getBrokerMetrics/';
    return this.http.get(url);
  };

  public getActiveConnections(): Observable<any> {
    var url =  this.configService.getServerBaseUrlStr() + 'getActiveConnections/';
    return this.http.get(url);
  };

  public uploadCAFile(formData: FormData): Observable<any> {
    var url = this.configService.getServerBaseUrlStr() + 'addNewGroupCa/'
    return this.http.post(url, formData, {
      headers: new HttpHeaders({
        // 'Content-Type': 'multipart/form-data' is **not** needed, browsers set it automatically
      })
    });
  }
}

export type workflow = {
  name: string,
  version: string,
  type: string,
  creatorName: string,
  description: string,
  roleProductSet: string,
  diagramType:  string,
  federationId: string,
  federationName: string
};
