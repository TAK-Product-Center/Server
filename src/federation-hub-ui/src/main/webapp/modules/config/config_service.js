
'use strict';

angular.module('roger_federation.Config')
  .factory('ConfigService', ["$http", "$localStorage", '$cookieStore', 'growl',
    function($http, $localStorage, $cookieStore, growl) {

      var service = {};
      var ROGER_FEDERATION_PROTOCOL_STR = "https";
      var ROGER_FEDERATION_BASE_PATH = "/fig/";
      var initialized = false;
      var serverInfo = {
        roger_federation: {},
        fuseki: {
          uri: ""
        }
      };


      function initializeUiConfig() {
        var hostname = $cookieStore.get('roger_federation.server.hostname');
        if (typeof hostname === "undefined") {
          hostname = location.hostname;
        }

        var port = Number(location.port)

        if (typeof $localStorage.configuration === "undefined") {
          $localStorage.configuration = {};
        }
        if (typeof $localStorage.configuration.roger_federation === "undefined") {
          $localStorage.configuration.roger_federation = {
            server: {
              protocol: ROGER_FEDERATION_PROTOCOL_STR,
              name: hostname,
              port: port,
              basePath: ROGER_FEDERATION_BASE_PATH
            }
          };
        } else {
          if (typeof $localStorage.configuration.roger_federation.server === "undefined") {
            $localStorage.configuration.roger_federation.server = {
              protocol: ROGER_FEDERATION_PROTOCOL_STR,
              name: hostname,
              port: port,
              basePath: ROGER_FEDERATION_BASE_PATH
            };
          }
        }
        
        $localStorage.configuration.roger_federation.server.name = hostname;
        $localStorage.configuration.roger_federation.server.port = port;

        serverInfo.roger_federation = {
          protocol: ROGER_FEDERATION_PROTOCOL_STR,
          name: $localStorage.configuration.roger_federation.server.name,
          port: $localStorage.configuration.roger_federation.server.port,
          basePath: ROGER_FEDERATION_BASE_PATH
        };

        initialized = true;
        service.setServerInfoFromHAL();
      }


      service.setServerInfo = function(info) {
        if (initialized === false) {
          initializeUiConfig();
        }
        if (typeof $localStorage.configuration === "undefined") {
          $localStorage.configuration = {
            roger_federation: {
              server: {
                protocol: ROGER_FEDERATION_PROTOCOL_STR,
                name: info.roger_federation.name,
                port: info.roger_federation.port,
                basePath: ROGER_FEDERATION_BASE_PATH
              }
            },
            fuseki: {
              server: {
                uri: info.fuseki.uri
              }
            }
          };
        } else {
          $localStorage.configuration.roger_federation.server.name = serverInfo.roger_federation.name = info.roger_federation.name;
          $localStorage.configuration.roger_federation.server.port = serverInfo.roger_federation.port = info.roger_federation.port;
          serverInfo.fuseki.uri = info.fuseki.uri;
        }
        $cookieStore.put('roger_federation.server.hostname', info.roger_federation.name);
        $cookieStore.put('roger_federation.server.port', info.roger_federation.port);

        service.getPropertyValueForName(FUSEKI_PROPERTY_NAME).then(function(result) {
          if (result.data !== 'undefined' && result.data.length > 0) {
            var fusekiProperty = {
              description: result.data[0].description,
              name: result.data[0].name,
              value: info.fuseki.uri
            };
            service.updatePropertyValue(result.data[0].id, JSON.stringify(fusekiProperty)).then(function(result) {}, function(result) {
              growl.error("Failed to upload Fuseki database location to the server. Error: " + result.data.error);
            });
          } else {
            var fusekiProperty = {
              description: FUSEKI_PROPERTY_DESCRIPTION,
              name: FUSEKI_PROPERTY_NAME,
              value: info.fuseki.uri
            };
            service.addPropertyValue(JSON.stringify(fusekiProperty)).then(function(result) {}, function(result) {
              growl.error("Failed to add Fuseki database location to the server. Error: " + result.data.error);
            });
          }
        }, function(result) {
          var fusekiProperty = {
            description: FUSEKI_PROPERTY_DESCRIPTION,
            name: FUSEKI_PROPERTY_NAME,
            value: info.fuseki.uri
          };
          service.addPropertyValue(JSON.stringify(fusekiProperty)).then(function(result) {}, function(result) {
            growl.error("Failed to add Fuseki database location to the server. Error: " + result.data.error);
          });
        });
      };

      service.getUiServerInfo = function() {
        if (initialized === false) {
          initializeUiConfig();
        }
        return serverInfo;
      };

      service.getServerRootUrlStr = function() {
        if (initialized === false) {
          initializeUiConfig();
        }
        return serverInfo.roger_federation.protocol + "://" + serverInfo.roger_federation.name + ":" + serverInfo.roger_federation.port;
      };

      service.getServerBaseUrlStr = function() {
        if (initialized === false) {
          initializeUiConfig();
        }
        return serverInfo.roger_federation.protocol + "://" + serverInfo.roger_federation.name + ":" + serverInfo.roger_federation.port + serverInfo.roger_federation.basePath;
      };

      service.getServerBaseUrlStrV2 = function() {
        if (initialized === false) {
          initializeUiConfig();
        }
//        return serverInfo.roger_federation.protocol + "://" + serverInfo.roger_federation.name + ":" + serverInfo.roger_federation.port + "/api/v2/";
          console.log(service.getServerBaseUrlStr());
          return service.getServerBaseUrlStr();
      };


      /**
       * Update Property value
       *
       * @param {Object}
       *                content
       * @return {Array}
       */
      service.updatePropertyValue = function(id, content) {
        return $http({
          method: 'PUT',
          url: service.getServerBaseUrlStr() + 'properties/' + id,
          data: content,
          transformRequest: [],
          headers: {
            'Content-Type': 'application/json'
          }
        }).then(function(res) {
          return res;
        }, function(reason) {
          throw reason;
        });
      };


      /**
       * Add Property data
       *
       * @param {Object}
       *                content
       * @return {Array}
       */
      service.addPropertyValue = function(content) {
        return $http({
          method: 'POST',
          url: service.getServerBaseUrlStr() + 'properties/',
          data: content,
          transformRequest: [],
          headers: {
            'Content-Type': 'application/json'
          }
        }).then(function(res) {
          return res;
        }, function(reason) {
          throw reason;
        });
      };



      /**
       * Get Property Value for Name
       *
       * @param {Object}
       *                propertyName
       * @return {Array}
       */
      service.getPropertyValueForName = function(propertyName) {
        return $http.get(
          service.getServerBaseUrlStr() + 'properties/name/' + propertyName).then(
          function(res) {
            return res;
          },
          function(reason) {
            throw reason;
          });
      };


      service.getServerStatus = function() {
        var serverStatus = {
          uiOK: true,
          fusekiOK: true,
          Message: ""
        };
//        return $http.get(service.getServerBaseUrlStr() + 'ontology/tdb/status').then(
//          function(res) {
//            serverStatus.amtOK = true;
//            serverStatus.Message = "roger_federation server status: OK";
//            if (res.data) {
//              serverStatus.fusekiOK = true;
//              serverStatus.Message += "\nFuseki server status: OK";
//            } else {
//              serverStatus.fusekiOK = false;
//              serverStatus.Message += "\nFuseki server status: Unavailable";
//            }
//            return serverStatus;
//          },
//          function() {
//            serverStatus.amtOK = false;
//            serverStatus.fusekiOK = false;
//            serverStatus.Message = "roger_federation server status: Unavailable";
//            return serverStatus;
//          });
      };

      service.getHALInfo = function() {
        return $http.get(
          service.getServerRootUrlStr() + '/info').then(
          function(res) {
            return res;
          },
          function(reason) {
            throw reason;
          });
      };

      service.setServerInfoFromHAL = function() {
        service.getHALInfo().then(
          function(res) {
            serverInfo.fuseki.uri = res.data.fuseki.host + ":" + res.data.fuseki.port;
            serverInfo.roger_federation.version = res.data.build.version;
            return res;
          },
          function(reason) {
            serverInfo.fuseki.uri = "localhost:3030";
            serverInfo.roger_federation.version = "?";
            throw reason;
          });
      };


      return service;

    }
  ]);
