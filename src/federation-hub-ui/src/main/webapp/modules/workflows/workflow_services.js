'use strict';

angular.module('roger_federation.Workflows')

.factory('WorkflowService', function($http, ConfigService) {
  var workflowService = {};


  workflowService.getBpmnGraph = function(id) {
    return $http.get(
      ConfigService.getServerBaseUrlStrV2() + 'federation/' + id).then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  workflowService.deleteBpmnGraph = function(graphId) {
    return $http.get(
      ConfigService.getServerBaseUrlStrV2() + 'bPMNDiagrams/search/deleteById', {
        params: {
          id: graphId
        }
      }).then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  workflowService.saveBpmnGraph = function(graphJson) {
    return $http({
      method: 'POST',
      url: ConfigService.getServerBaseUrlStrV2() + 'saveFederation',
      data: graphJson,
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
   * Get Workflow Descriptors
   *
   * @return {Array}
   */
  workflowService.getWorkflowDescriptors = function() {
    return $http.get(
      ConfigService.getServerBaseUrlStrV2() + 'federations').then(
      function(res) {
        // return res.data;
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  /**
   * Get Group Descriptors
   *
   * @return {Array}
   */
  workflowService.getGroupDescriptors = function() {
    return $http.get(
      ConfigService.getServerBaseUrlStr() + 'workflow/descriptor/workflow', {
        params: {
          filter: "group"
        }
      }).then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  workflowService.addGroup = function(workflowID, groupID) {
    return $http({
      method: 'PUT',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/' + workflowID + '/addGroup',
      data: groupID,
      transformRequest: [],
      headers: {
        'Content-Type': 'application/json'
      }
    });
  };

  /**
   * Get Workflows
   *
   * @return {Array}
   */
  workflowService.getWorkflows = function() {
    return $http.get(
      ConfigService.getServerBaseUrlStr() + 'workflow').then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  /**
   * Get the default pubs and subs for a given role
   *
   * @return {Array}
   */
  workflowService.getDefaultsForRole = function(roleID) {
    var url = ConfigService.getServerBaseUrlStr() + 'defaults/';
    return $http.get(url, {
        params: {
          roleID: roleID
        }
      })
      .then(
        function(res) {
          return res.data;
        },
        function(reason) {
          throw reason;
        });
  };

  /**
   * Import Workflow
   *
   * @param {Object}
   *                content
   * @return {Boolean} true
   */
  workflowService.importWorkflow = function(content) {
    return $http({
      method: 'POST',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/import',
      data: content,
      transformRequest: [],
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      return res.headers('Location');
    }, function(reason) {
      throw reason;
    });
  };

  /**
   * Export Workflow
   *
   * @param {Object}
   *                workflowId
   * @return {Array}
   */
  workflowService.exportWorkflow = function(workflowId) {
    return $http.get(
      ConfigService.getServerBaseUrlStr() + 'workflow/export/' + workflowId).then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  /**
   * Upload Workflow
   *
   * @param {Object}
   *                content
   * @return {Boolean} true
   */
  workflowService.uploadWorkflow = function(content) {
    return $http({
      method: 'POST',
      url: ConfigService.getServerBaseUrlStr() + 'workflow',
      data: content,
      transformRequest: [],
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      return res.headers('Location');
    }, function(reason) {
      throw reason;
    });
  };


  /**
   * Delete Workflow
   *
   * @param {Object}
   *                workflowId
   * @return {Array}
   */
  workflowService.deleteWorkflow = function(workflowId) {
    return $http.delete(
      ConfigService.getServerBaseUrlStr() + 'workflow/' + workflowId).then(
      function(res) {
        return res;
      },
      function(reason) {
        throw reason;
      });
  };


  /**
   * Update Workflow
   *
   * @param {Object}
   *                workflowId
   * @param {Object}
   *                content
   * @return {Array}
   */
  workflowService.updateWorkflowAttributes = function(workflowId, content) {
    console.log("updateWorkflowAttributes :" + workflowId);
    return $http({
      method: 'PUT',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/' + workflowId,
      data: content,
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      return res.data;
    }, function(reason) {
      throw reason;
    });
  };

  /**
   * Get Workflow
   *
   * @param {Object}
   *                workflowId
   * @return {Array}
   */
  workflowService.getWorkflow = function(workflowId) {
    return $http.get(
      ConfigService.getServerBaseUrlStr() + 'workflow/' + workflowId).then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  /**
   * Get Workflow Graph Item
   *
   * @param {Object}
   *                graphItemId
   * @return {Array}
   */
  workflowService.getWorkflowGraphItem = function(graphItemId) {
    return $http.get(
      ConfigService.getServerBaseUrlStr() + 'workflow/graph_item/' + graphItemId).then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };


  /**
   * Upload Workflow Graph Item
   *
   * @param {Object}
   *                content
   * @return {Boolean} true
   */
  workflowService.uploadWorkflowGraphItem = function(graphItem) {
    var content = JSON.stringify(graphItem);
    return $http({
      method: 'POST',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/graph_item',
      data: content,
      transformRequest: [],
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      var parts = res.headers('Location').split("/");
      var graphItemId = parts[parts.length - 1];
      return {
        id: graphItem.item,
        graphItemID: graphItemId
      };
    }, function(reason) {
      throw reason;
    });
  };


  /**
   * Delete Workflow Graph Item
   *
   * @param {Object}
   *                graphItemId
   * @return {Array}
   */
  workflowService.deleteWorkflowGraphItem = function(graphItemId) {
    return $http.delete(
      ConfigService.getServerBaseUrlStr() + 'workflow/graph_item/' + graphItemId).then(
      function(res) {
        return res;
      },
      function(reason) {
        throw reason;
      });
  };


  /**
   * Update Workflow Graph Item
   *
   * @param {Object}
   *                graphItemId
   * @param {Object}
   *                content
   * @return {Array}
   */
  workflowService.updateWorkflowGraphItem = function(graphItemId, content) {
    return $http({
      method: 'PUT',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/graph_item/' + graphItemId,
      data: content,
      transformRequest: [],
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      return res.data;
    }, function(reason) {
      throw reason;
    });
  };



  /**
   * Upload Workflow Graph Link
   *
   * @param {Object}
   *                content
   * @return {Boolean} true
   */
  workflowService.uploadWorkflowGraphLink = function(content) {
    return $http({
      method: 'POST',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/graph_link',
      data: content,
      transformRequest: [],
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      return res.headers('Location');
    }, function(reason) {
      throw reason;
    });
  };


  /**
   * Delete Workflow Graph Link
   *
   * @param {Object}
   *                graphLinkId
   * @return {Array}
   */
  workflowService.deleteWorkflowGraphLink = function(graphLinkId) {
    return $http.delete(
      ConfigService.getServerBaseUrlStr() + 'workflow/graph_link/' + graphLinkId).then(
      function(res) {
        return res;
      },
      function(reason) {
        throw reason;
      });
  };


  /**
   * Update Workflow Graph Link
   *
   * @param {Object}
   *                graphLinkId
   * @param {Object}
   *                content
   * @return {Array}
   */
  workflowService.updateWorkflowGraphLink = function(graphLinkId, content) {
    return $http({
      method: 'PUT',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/graph_link/' + graphLinkId,
      data: content,
      transformRequest: [],
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      return res.data;
    }, function(reason) {
      throw reason;
    });
  };

  workflowService.uploadLifecycleEvent = function(workflowId, lifecycleEvent) {
    var content = JSON.stringify(lifecycleEvent);
    return $http({
      method: 'POST',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/' + workflowId + '/lifecycle_event',
      data: content,
      transformRequest: [],
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      return res.data;
    }, function(reason) {
      throw reason;
    });
  };

  workflowService.deleteLifecycleEvent = function(workflowId, lifecycleEventId) {
    return $http.delete(
      ConfigService.getServerBaseUrlStr() + 'workflow/' + workflowId + '/lifecycle_event/' + lifecycleEventId).then(
      function(res) {
        return res;
      },
      function(reason) {
        throw reason;
      });
  };

  workflowService.addCommandToLifeCycleEvent = function(workflowId, lifecycleEventId, imsCommand) {
    var content = JSON.stringify(imsCommand);
    return $http({
      method: 'POST',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/' + workflowId + '/lifecycle_event/' + lifecycleEventId,
      data: content,
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      return res.data;
    }, function(reason) {
      throw reason;
    });
  };

  workflowService.updateLifeCycleCommand = function(workflowId, lifecycleEventId, imsCommand) {
    var content = JSON.stringify(imsCommand);
    return $http({
      method: 'PUT',
      url: ConfigService.getServerBaseUrlStr() + 'workflow/' + workflowId + '/lifecycle_event/' + lifecycleEventId,
      data: content,
      transformRequest: [],
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(function(res) {
      return res.data;
    }, function(reason) {
      throw reason;
    });
  };

  workflowService.deleteLifecycleCommand = function(workflowId, lifecycleEventId, commandId) {
    return $http.delete(
      ConfigService.getServerBaseUrlStr() + 'workflow/' + workflowId + '/lifecycle_event/' + lifecycleEventId + '/command/' + commandId).then(
      function(res) {
        return res;
      },
      function(reason) {
        throw reason;
      });
  };

  workflowService.getAttributesList = function() {
    return $http.get(
      ConfigService.getServerBaseUrlStr() + 'attributes/keys').then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  workflowService.instantiateAndExecute = function(graphId) {
    return $http.get(
      ConfigService.getServerBaseUrlStr() + 'generate/templateContainer', {
        params: {
          id: graphId,
          action: 'INSTANTIATE'
        }
      }).then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  workflowService.downloadTemplateContainer = function(graphId) {
    return $http.get(
      ConfigService.getServerBaseUrlStr() + 'generate/templateContainer', {
        params: {
          id: graphId,
          action: 'DOWNLOAD'
        }
      }).then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  workflowService.diagramsUsing = function(diagramId) {
    return $http.get(
      ConfigService.getServerBaseUrlStr() + 'diagram/diagrams_using/' + diagramId).then(
      function(res) {
        return res.data;
      },
      function(reason) {
        throw reason;
      });
  };

  workflowService.getGraphAsJson = function(graphId) {
    return $http.get(
          ConfigService.getServerBaseUrlStr() + 'graphAsJson/' + graphId).then(
          function(res) {
            return res.data;
          },
          function(reason) {
            throw reason;
          });

  };

  workflowService.getGraphAsYaml = function(graphId) {
      return $http({
                method: 'GET',
                url: ConfigService.getServerBaseUrlStr() + 'graphAsYaml/' + graphId,
                transformResponse: undefined
            }).then(
            function(res) {
              return res.data;
            },
            function(reason) {
              throw reason;
            });

    };

  workflowService.sendToFederationManager = function(graphId) {
    return $http.get(
          ConfigService.getServerBaseUrlStr() + 'updateFederationManager/' + graphId).then(
          function(res) {
            return res.data;
          },
          function(reason) {
            throw reason;
          });
  };

  workflowService.sendToFederationManagerAndFile = function(graphId) {
      return $http.get(
          ConfigService.getServerBaseUrlStr() + 'updateFederationManagerAndFile/' + graphId).then(
          function(res) {
              return res.data;
          },
          function(reason) {
              throw reason;
          });
  };

  workflowService.getActiveFederation = function() {
      return $http.get(
          ConfigService.getServerBaseUrlStrV2() + 'getActivePolicy/').then(
          function(res) {
              return res.data;
          },
          function(reason) {
              throw reason;
          });
  };

  workflowService.getKnownCaGroups = function() {
      return $http.get(
          ConfigService.getServerBaseUrlStrV2() + 'getKnownCaGroups/').then(
          function(res) {
              return res.data;
          },
          function(reason) {
              throw reason;
          });
  };

  workflowService.getActiveConnections = function() {
      return $http.get(
          ConfigService.getServerBaseUrlStrV2() + 'getActiveConnections/').then(
          function(res) {
              return res.data;
          },
          function(reason) {
              throw reason;
          });
  };

  workflowService.deleteGroupCa = function(ca) {
      return $http.delete(
          ConfigService.getServerBaseUrlStrV2() + 'deleteGroupCa/' + ca).then(
          function(res) {
              return res.data;
          },
          function(reason) {
              throw reason;
          });
  };

  workflowService.getKnownGroupsForNode = function(graphNodeId) {
      return $http.get(
          ConfigService.getServerBaseUrlStrV2() + 'getKnownGroupsForGraphNode/' + graphNodeId).then(
          function(res) {
              return res.data;
          },
          function(reason) {
              throw reason;
          });
  };



  workflowService.getKnownFilters = function() {
      var newFilters = [];
      newFilters.push({
          name: "allowEventType",
          args: [
              {
                  name: "m",
                  type: "roger.message.Message",
                  value: "m"
              },
              {
                  name: "eventType",
                  type: "string",
                  value: ""
              }

          ],
          filterObject: "com.bbn.roger.plugin.federation.MessageTypeLambdaFilters",
          description: "allow the specified GeoEvent type"
      });

      newFilters.push({
          name: "allowAll",
          args: [
              {
                  name: "m",
                  type: "roger.message.Message",
                  value: "m"
              }
          ],
          filterObject: "com.bbn.roger.plugin.federation.MessageTypeLambdaFilters",
          description: "allow all messages"
      });

      newFilters.push({
          name: "allowChatMessage",
          args: [
              {
                  name: "m",
                  type: "roger.message.Message",
                  value: "m"
              }

          ],
          filterObject: "com.bbn.roger.plugin.federation.MessageTypeLambdaFilters",
          description: "Allow a message if it is a CoT chat message"
      });

      newFilters.push({
          name: "mutateEventType",
          args: [
              {
                  name: "m",
                  type: "roger.message.Message",
                  value: "m"
              },
              {
                  name: "eventType",
                  type: "string",
                  value: ""
              }

          ],
          filterObject: "com.bbn.roger.plugin.federation.MessageTypeLambdaFilters",
          description: "Change the GeoEvent of the message to the given string"
      });

      newFilters.push({
          name: "chatMessagePrepend",
          args: [
              {
                  name: "m",
                  type: "roger.message.Message",
                  value: "m"
              },
              {
                  name: "toPrepend",
                  type: "string",
                  value: ""
              }

          ],
          filterObject: "com.bbn.roger.plugin.federation.MessageTypeLambdaFilters",
          description: "Prepend the given string to the chat message"
      });

      newFilters.push({
          name: "colorTrackByTeam",
          args: [
              {
                  name: "m",
                  type: "roger.message.Message",
                  value: "m"
              },
              {
                  name: "color",
                  type: "string",
                  value: ""
              }

          ],
          filterObject: "com.bbn.roger.plugin.federation.MessageTypeLambdaFilters",
          description: "Colors track messages moving along this edge"
      });

      newFilters.push({
          name: "updateLocation",
          args: [
              {
                  name: "m",
                  type: "roger.message.Message",
                  value: "m"
              },
              {
                  name: "fuzzDistance",
                  type: "double",
                  value: ""
              }

          ],
          filterObject: "com.bbn.roger.plugin.federation.MessageTypeLambdaFilters",
          description: "Fuzzes the point by the specified fuzzDistance, in degrees"
      });


    return newFilters;
  }

  return workflowService;
})
