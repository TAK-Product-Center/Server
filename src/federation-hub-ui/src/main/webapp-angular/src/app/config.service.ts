import { Injectable } from '@angular/core';
import { CookieService } from 'ngx-cookie-service';
import { LocalStorageService } from './local-storage.service';

@Injectable({
  providedIn: 'root'
})

export class ConfigService {

  private service = {};
  private ROGER_FEDERATION_PROTOCOL_STR = window.location.protocol;
  private ROGER_FEDERATION_BASE_PATH = "/api/";
  private initialized = false;
  private localServerInfo: serverInfo = {
    roger_federation: {
      server: {
        protocol: this.ROGER_FEDERATION_PROTOCOL_STR,
        name: "undefined",
        port: 0,
        basePath: this.ROGER_FEDERATION_BASE_PATH
      }
    },
    fuseki: {
      uri: ""
    }
  };
  
  constructor(private cookieService: CookieService, 
              private localStorageService: LocalStorageService) { 

  }

   private initializeUiConfig():any {

    var hostname = this.cookieService.get('roger_federation.server.hostname');
    if (hostname === "") {
      hostname = location.hostname;
    }

    var api_port = location.port;
    var local_data = this.localStorageService.get("api_port") as string;

    if(local_data !== null){
      api_port = local_data;
    }
    var port = Number(api_port)

    if (typeof this.localStorageService.get("configuration") === null) {
      this.localStorageService.set("configuration", {})
    }
    var configuration = this.localStorageService.get("configuration") as serverInfo
    if (configuration === null) {
      configuration = {
        roger_federation : {
          server: {
            protocol: this.ROGER_FEDERATION_PROTOCOL_STR,
            name: hostname,
            port: port,
            basePath: this.ROGER_FEDERATION_BASE_PATH
          }
        }
      }
    } else {
      if (typeof configuration.roger_federation.server === "undefined") {
        configuration.roger_federation.server = {
          protocol: this.ROGER_FEDERATION_PROTOCOL_STR,
          name: hostname,
          port: port,
          basePath: this.ROGER_FEDERATION_BASE_PATH
        };
      }
    }
    
    configuration.roger_federation.server.name = hostname;
    configuration.roger_federation.server.port = port;

    this.localServerInfo.roger_federation.server = {
      protocol: this.ROGER_FEDERATION_PROTOCOL_STR,
      name: configuration.roger_federation.server.name,
      port: configuration.roger_federation.server.port,
      basePath: this.ROGER_FEDERATION_BASE_PATH
    };

    this.initialized = true;
  }

  public getServerBaseUrlStr(): string {
    if (this.initialized === false) {
      this.initializeUiConfig();
    }
    var baseUrlString = this.localServerInfo.roger_federation.server.protocol + "//" + 
                           this.localServerInfo.roger_federation.server.name + ":" + 
                           this.localServerInfo.roger_federation.server.port + 
                           this.localServerInfo.roger_federation.server.basePath;
    return baseUrlString;
  };

}

export type serverInfo = {
  roger_federation: {
    server: {
      protocol: string
      name: string
      port: number
      basePath: string
    }
  }
  fuseki?: {
    uri: string
  };
}
