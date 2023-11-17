
'use strict';

angular.module('roger_federation.Manage')

.controller('ManageController', ['$scope', '$rootScope', '$state', '$stateParams', '$log', 'growl', 'ACCESS_EVENTS',
  'RoleProductSetService', 'WorkflowService', 'AssetService',
  'DefaultsService', 'AuthenticationService', manageController
]);


function manageController($scope, $rootScope, $state, $stateParams, $log, growl, ACCESS_EVENTS, RoleProductSetService,
  WorkflowService, AssetService, DefaultsService, AuthenticationService) {
  $rootScope.$state = $state;
  $rootScope.$stateParams = $stateParams;

  $rootScope.$on('$stateChangeStart', handleStateChangeStart);

  $scope.rowCollection = [];
  $scope.displayedCollection = [];
  $scope.itemsByPage = 15;
  $scope.defaultsData = {
    datasets: [],
    currentDataset: undefined,
    currentDefaults: [],
    orderAsc: false,
    orderByField: 'ownerItem.name'
  };

  $scope.assetUploadData = {};


  $scope.initialize = function() {
    if ($state.current.name === 'manage.role-sets') {
      $scope.initializeRoleProductSetView();
    } else if ($state.current.name === 'manage.diagrams' || $state.current.name === 'manage.federations') {
      $scope.initializeWorkflowView();
    } else if ($state.current.name === 'manage.workflow-sets') {
      $scope.initializeWorkflowSetsView();
    } else if ($state.current.name === 'manage.instances') {
      $scope.initializeInstanceView();
    } else if ($state.current.name === 'manage.assets') {
      $scope.initializeAssetView();
    } else if ($state.current.name === 'manage.defaults') {
      $scope.initializeDefaultsView();
    }
  };


  function handleStateChangeStart(event, toState, toParams, fromState, fromParams) {
    if (toState.name === 'manage.role-sets') {
      $scope.initializeRoleProductSetView();
    } else if (toState.name === 'manage.diagrams' || $state.current.name === 'manage.federations') {
      $scope.initializeWorkflowView();
    } else if (toState.name === 'manage.workflow-sets') {
      $scope.initializeWorkflowSetsView();
    } else if (toState.name === 'manage.instances') {
      $scope.initializeInstanceView();
    } else if (toState.name === 'manage.assets') {
      $scope.initializeAssetView();
    } else if (toState.name === 'manage.defaults') {
      $scope.initializeDefaultsView();
    }
  }


  // INITIALIZE VIEWS

  $scope.initializeRoleProductSetView = function() {
    RoleProductSetService.getRoleProductSetDescriptors().then(function(roleProductSetList) {
      $scope.rowCollection = roleProductSetList;
      $scope.displayedCollection = [].concat($scope.rowCollection);
    }, function(result) {
      $scope.rowCollection.length = 0;
      $scope.displayedCollection = [].concat($scope.rowCollection);
      if (result.status === -1) {
        $rootScope.$broadcast(ACCESS_EVENTS.accessFailed, result);
      } else {
        var err = ".";
        if (((typeof result.data) !== "undefined") && result.data !== null && ((typeof result.data.error) !== "undefined") && result.data.error !== null) {
          err = result.data.error + ".";
        }
        growl.error("Failed getting role-product sets. Error: " + err);
      }
    });
  };


  $scope.initializeWorkflowView = function() {
    WorkflowService.getWorkflowDescriptors().then(function(workflowList) {
      $scope.rowCollection = workflowList;
      $scope.displayedCollection = [].concat($scope.rowCollection);
    }, function(result) {
      $scope.rowCollection.length = 0;
      $scope.displayedCollection = [].concat($scope.rowCollection);
      if (result.status === -1) {
        $rootScope.$broadcast(ACCESS_EVENTS.accessFailed, result);
      } else {
        var err = ".";
        if (((typeof result.data) !== "undefined") && result.data !== null && ((typeof result.data.error) !== "undefined") && result.data.error !== null) {
          err = result.data.error + ".";
        }
        growl.error("Failed getting workflows. Error: " + err);
      }
    });
  };

  $scope.initializeWorkflowSetsView = function() {
    WorkflowSetsService.getWorkflowSetsDescriptors().then(function(workflowSetsList) {
      $scope.rowCollection = workflowSetsList;
      $scope.displayedCollection = [].concat($scope.rowCollection);
    }, function(result) {
      $scope.rowCollection.length = 0;
      $scope.displayedCollection = [].concat($scope.rowCollection);
      if (result.status === -1) {
        $rootScope.$broadcast(ACCESS_EVENTS.accessFailed, result);
      } else {
        var err = ".";
        if (((typeof result.data) !== "undefined") && result.data !== null && ((typeof result.data.error) !== "undefined") && result.data.error !== null) {
          err = result.data.error + ".";
        }
        growl.error("Failed getting workflow sets files. Error: " + err);
      }
    });
  };

  $scope.initializeInstanceView = function() {
    InstanceService.getInstanceDescriptors().then(function(instanceList) {
      $scope.rowCollection = instanceList;
      $scope.displayedCollection = [].concat($scope.rowCollection);
    }, function(result) {
      $scope.rowCollection.length = 0;
      $scope.displayedCollection = [].concat($scope.rowCollection);
      if (result.status === -1) {
        $rootScope.$broadcast(ACCESS_EVENTS.accessFailed, result);
      } else {
        var err = ".";
        if (((typeof result.data) !== "undefined") && result.data !== null && ((typeof result.data.error) !== "undefined") && result.data.error !== null) {
          err = result.data.error + ".";
        }
        growl.error("Failed getting instances. Error: " + err);
      }
    });
  };

  $scope.initializeAssetView = function() {
    AssetService.getAssetFileDescriptors().then(function(assetList) {
      $scope.rowCollection = assetList;
      $scope.displayedCollection = [].concat($scope.rowCollection);
    }, function(result) {
      $scope.rowCollection.length = 0;
      $scope.displayedCollection = [].concat($scope.rowCollection);
      if (result.status === -1) {
        $rootScope.$broadcast(ACCESS_EVENTS.accessFailed, result);
      } else {
        var err = ".";
        if (((typeof result.data) !== "undefined") && result.data !== null && ((typeof result.data.error) !== "undefined") && result.data.error !== null) {
          err = result.data.error + ".";
        }
        growl.error("Failed getting asset files. Error: " + err);
      }
    });
  };

  $scope.initializeDefaultsView = function() {
    DefaultsService.initiateOntologiesRetrieval().then(
      function(res) {
        $scope.defaultsData.datasets = res;
      },
      function(res) {
        growl.error("Error retrieving dataset list: " + res);
      }
    );
  };

  $scope.changeDefaultsOrderBy = function(fieldName) {
    if ($scope.defaultsData.orderByField == fieldName) {
      $scope.defaultsData.orderAsc = !$scope.defaultsData.orderAsc;
    } else {
      $scope.defaultsData.orderAsc = false;
    }
    $scope.defaultsData.orderByField = fieldName;
  }

  $scope.datasetChanged = function() {
    DefaultsService.initiateDefaultsRetrieval($scope.defaultsData.currentDataset).then(
      function(res) {
        $scope.defaultsData.currentDefaults = res;
      },
      function(res) {
        growl.error("Error retrieving current defaults list: " + res);
      }
    );
  };

  $scope.importDefaults = function() {
    DefaultsService.initiateDefaultsImport($scope.defaultsData.currentDataset).then(
      function(res) {
        growl.success("Defaults imported");
        $scope.datasetChanged();
      },
      function(res) {
        growl.error("Error importing defaults: " + res);
      }
    );
  };

  $scope.removeDefault = function(defaultID) {
    DefaultsService.initiateDefaultRemoval(defaultID).then(
      function(res) {
        growl.success("Default removed");
        $scope.datasetChanged();
      },
      function(res) {
        growl.error("Error removing default: " + res);
      }
    );
  };

  // EXPORT ROWS




  $scope.exportRoleProductSet = function(row) {
    RoleProductSetService.getRoleProductSet(row.id).then(function(res) {

      function convertToDefinitionFile(data) {
        var result = {
          name: data.name,
          description: data.description,
          roles: [],
          products: []
        };

        for (var i = 0, len = data.roles.length; i < len; i++) {
          result.roles.push(data.roles[i].item.uri);
        }
        for (var i = 0, len = data.products.length; i < len; i++) {
          result.products.push(data.products[i].item.uri);
        }
        return result;
      }

      var data = convertToDefinitionFile(res);

      var filename = 'roger_federation-role-product-set-' + res.name + '.json';
      data = new Blob([JSON.stringify(data)], {
        type: "text/plain;charset=utf-8"
      });
      saveAs(data, filename);
    }, function(res) {
      var msg = "Failed to export role-product set.";
      growl.error(msg);
    });
  };

  $scope.exportWorkflow = function(row) {
    WorkflowService.exportWorkflow(row.id).then(function(res) {
      var filename = 'roger_federation-workflow-' + res.name + '.json';
      var data = new Blob([JSON.stringify(res)], {
        type: "text/plain;charset=utf-8"
      });
      saveAs(data, filename);
    }, function(res) {
      var msg = "Failed to export workflow.";
      growl.error(msg);
    });
  };

  $scope.exportWorkflowSet = function(row) {
    WorkflowSetsService.getWorkflowSet(row.id).then(function(res) {
      var filename = 'roger_federation-workflow-set-' + res.name + '.json';
      var data = new Blob([JSON.stringify(res)], {
        type: "text/plain;charset=utf-8"
      });
      saveAs(data, filename);
    }, function(res) {
      var msg = "Failed to export workflow set.";
      growl.error(msg);
    });
  }

  $scope.exportInstance = function(row) {
    InstanceService.getInstance(row.id).then(function(res) {
      var filename = 'roger_federation-instance-' + res.name + '.json';
      var data = new Blob([JSON.stringify(res)], {
        type: "text/plain;charset=utf-8"
      });
      saveAs(data, filename);
    }, function(res) {
      var msg = "Failed to export instance.";
      growl.error(msg);
    });
  }

  $scope.exportAsset = function(row) {
    AssetService.getAssetFile(row.id).then(function(res) {
      var filename = 'roger_federation-asset-' + res.name + '.json';
      var data = new Blob([JSON.stringify(res)], {
        type: "text/plain;charset=utf-8"
      });
      saveAs(data, filename);
    }, function(res) {
      var msg = "Failed to export asset file.";
      growl.error(msg);
    });
  }


  // REMOVE ROWS

  function removeRowFromCollection(row) {
    var index = $scope.rowCollection.indexOf(row);
    if (index !== -1) {
      $scope.rowCollection.splice(index, 1);
      // do I need to do this???
      $scope.displayedCollection = [].concat($scope.rowCollection);
    }
  }



  // IMPORT DATASET

  $scope.handleFileReadComplete = function(event) {
    var content = event.target.result,
      error = event.target.error;

    if (error != null) {
      switch (error.code) {
        case error.ENCODING_ERR:
          growl.error("File read encoding error.");
          break;

        case error.NOT_FOUND_ERR:
          growl.error("The file could not be found at the time the read was processed.");
          break;

        case error.NOT_READABLE_ERR:
          growl.error("The file could not be read.");
          break;

        case error.SECURITY_ERR:
          growl.error("Security error with file.");
          break;

        default:
          growl.error("Uncategorized file read error: " + event.target.error.name + ".");
      }
    } else {
      return content;
    }
    return null;
  };

  $scope.handleFileReadAbort = function() {
    growl.error("File read aborted.");
  };

  $scope.handleFileReadError = function(evt) {
    switch (evt.target.error.name) {
      case "NotFoundError":
        growl.error("The file could not be found at the time the read was processed.");
        break;

      case "SecurityError":
        growl.error("Security error with file.");
        break;

      case "NotReadableError":
        growl.error("The file could not be read.");
        break;

      case "EncodingError":
        growl.error("File read encoding error.");
        break;

      default:
        growl.error("Uncategorized file read error: " + event.target.error.name + ".");

    } // switch
  }; // handleFileReadError


  //  Import Role Product Set
  $scope.importRoleProductSet = function(file) {
    if (file === null) {
      return;
    }
    var reader = new FileReader();

    reader.onloadend = (function() {
      return function(event) {
        var result = $scope.handleFileReadComplete(event);
        if (result != null) {
          result = JSON.parse(result);
          delete result.id;
          for (var i = 0, len = result.roles.length; i < len; i++) {
            delete result.roles[i].id;
            /*			for (varresult.roles[i].children.length) */
          }
          for (var i = 0, len = result.products.length; i < len; i++) {
            delete result.products[i].id;
          }
          result = JSON.stringify(result);
          RoleProductSetService.importRoleProductSet(result).then(function() {
            growl.success("Role-Product file uploaded.");
            $scope.initializeRoleProductSetView();
          }, function(result) {
            growl.error("Failed to upload role-product set. Error: " + result.data.error);
          });
        };
      };
    })();

    reader.abort = (function() {
      return function() {
        $scope.handleFileReadAbort();
      };
    })();

    reader.onerror = (function() {
      return function(event) {
        $scope.handleFileReadError(event);
      };
    })();

    reader.readAsText(file);
  };

  //  Import Role Product Set Definition
  $scope.importRoleProductSetDefinition = function(files) {
    if (files === null) {
      return;
    }
    files.forEach(function(file) {
      if (file === null) {
        return;
      }
      var reader = new FileReader();

      reader.onloadend = (function() {
        return function(event) {
          var result = $scope.handleFileReadComplete(event);
          if (result != null) {
            RoleProductSetService.uploadRoleProductSetDefinition(result).then(function() {
              growl.success("File: (" + file.name + ")<br>" + "Role-Product set definition file uploaded.");
              $scope.initializeRoleProductSetView();
            }, function(result) {
              growl.error("File: (" + file.name + ")<br>" + "Failed to upload role-product set definition file. Error: " + result.data.error);
            });
          };
        };
      })();

      reader.abort = (function() {
        return function() {
          $scope.handleFileReadAbort();
        };
      })();

      reader.onerror = (function() {
        return function(event) {
          $scope.handleFileReadError(event);
        };
      })();

      reader.readAsText(file);
    });
  };

  //  Import Workflow
  $scope.importWorkflow = function(files) {
    if (files === null) {
      return;
    }
    files.forEach(function(file) {
      if (file === null) {
        return;
      }
      var reader = new FileReader();
      reader.onloadend = (function() {
        return function(event) {
          var result = $scope.handleFileReadComplete(event);
          if (result != null) {
            result = JSON.parse(result);
            delete result.id;
            result = JSON.stringify(result);
            WorkflowService.importWorkflow(result).then(function() {
              $scope.initializeWorkflowView();
            }, function(result) {
              if (result.data.message !== undefined) {
                var errMsg = result.data.message;
                growl.error("File: (" + file.name + ")<br>" + errMsg.substring(errMsg.lastIndexOf(":") + 1));
              } else {
                growl.error("File: (" + file.name + ")<br>" + "Failed to upload workflow file. Error: " + result.data.error);
              }
            });
          };
        };
      })();

      reader.abort = (function() {
        return function() {
          $scope.handleFileReadAbort();
        };
      })();

      reader.onerror = (function() {
        return function(event) {
          $scope.handleFileReadError(event);
        };
      })();

      reader.readAsText(file);
    });
  };

  //  Import Asset
  $scope.importAsset = function(file) {
    if (file === null) {
      return;
    }
    var reader = new FileReader();

    reader.onloadend = (function() {
      return function(event) {
        var result = $scope.handleFileReadComplete(event);
        if (result != null) {
          var data = {
            contents: result,
            name: $scope.assetUploadData.name,
            description: $scope.assetUploadData.description,
            fileName: file.name,
            creatorName: AuthenticationService.getUserId()
          };
          AssetService.uploadAssetFile(data).then(function() {
            $scope.$$prevSibling.initializeAssetView();
          }, function(result) {
            growl.error("Failed to upload Asset file. Error: " + result.data.error + ". " + result.data.message);
          });
        };
      };
    })();

    reader.abort = (function() {
      return function() {
        $scope.handleFileReadAbort();
      };
    })();

    reader.onerror = (function() {
      return function(event) {
        $scope.handleFileReadError(event);
      };
    })();

    reader.readAsText(file);
  };


  //  Import Instance
  $scope.importInstance = function(file) {
    if (file === null) {
      return;
    }
    var reader = new FileReader();

    reader.onloadend = (function() {
      return function(event) {
        var result = $scope.handleFileReadComplete(event);
        if (result != null) {
          result = JSON.parse(result);
          delete result.id;
          result = JSON.stringify(result);
          InstanceService.uploadInstance(result).then(function() {
            growl.success("Instance file uploaded.");
            $scope.initializeInstanceView();
          }, function(result) {
            growl.error("Failed to upload instance file. Error: " + result.data.error);
          });
        };
      };
    })();

    reader.abort = (function() {
      return function() {
        $scope.handleFileReadAbort();
      };
    })();

    reader.onerror = (function() {
      return function(event) {
        $scope.handleFileReadError(event);
      };
    })();

    reader.readAsText(file);
  };


  //  Import Workflow Set
  $scope.importWorkflowSet = function(files) {
    if (files === null) {
      return;
    }
    files.forEach(function(file) {
      if (file === null) {
        return;
      }
      var reader = new FileReader();

      reader.onloadend = (function() {
        return function(event) {
          var result = $scope.handleFileReadComplete(event);
          if (result != null) {
            result = JSON.parse(result);
            delete result.id;
            result = JSON.stringify(result);
            InstanceService.uploadWorkflowSet(result).then(function() {
              growl.success("File: (" + file.name + ")<br>" + "Workflow set file uploaded.");
              $scope.initializeInstanceView();
            }, function(result) {
              growl.error("File: (" + file.name + ")<br>" + "Failed to upload workflow set file. Error: " + result.data.error);
            });
          };
        };
      })();

      reader.abort = (function() {
        return function() {
          $scope.handleFileReadAbort();
        };
      })();

      reader.onerror = (function() {
        return function(event) {
          $scope.handleFileReadError(event);
        };
      })();

      reader.readAsText(file);
    });
  };


  /** Upload Asset File Functions **/

  $scope.initializeUploadAssetFile = function() {
    $scope.assetUploadData = {
      name: '',
      assetFile: null,
      description: '',
      creatorName: AuthenticationService.getUserId()
    };
  };


  $scope.uploadAssetFile = function() {
    $scope.importAsset($scope.assetUploadData.assetFile);
    $scope.$close(true);
  };


  $scope.cancelUploadAssetModal = function() {
    $scope.$dismiss('cancel');
  };

}
