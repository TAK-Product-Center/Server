
"use strict";

angular.module('roger_federation.Workflows')
    .controller('CaController', ['$rootScope', '$scope', '$state', '$http', '$stateParams', '$modalInstance', '$timeout', '$log', '$cookieStore', 'growl', 'WorkflowTemplate', 'WorkflowService', 'OntologyService', 'JointPaper', caController]);

function caController($rootScope, $scope, $state, $http, $stateParams, $modalInstance, $timeout, $log, $cookieStore, growl, WorkflowTemplate, WorkflowService, OntologyService, JointPaper) {

    $scope.knownCas = [];

    $scope.initialize = function() {
        $scope.getCaGroups();
    };

    $scope.getCaGroups = function () {
        WorkflowService.getKnownCaGroups().then(
            function(caList) {
                $scope.knownCas = caList;
            },
            function (result) {
                console.log("Unable to load list of know CA's, " + result);
            }
        );
    };

    $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
    };

    $scope.deleteGroupCa = function(uid) {
        $scope.knownCas = $scope.knownCas.filter(ca => ca.uid !== uid);
        if (JointPaper.paper &&  JointPaper.paper._views) {
            var nodeKeys = Object.keys(JointPaper.paper._views);
            for (var i = 0; i < nodeKeys.length; i++) {
                let cellView = JointPaper.paper._views[nodeKeys[i]]
                
                // incase we hit an edge that was auto deleted
                if (!cellView) continue;
                
                if (cellView.model.attributes.roger_federation.name === uid) {
                    var id = cellView.model.id;
                    var cell = JointPaper.graph.getCell(id);
                    cell.remove();
                }
            }
        }

        $scope.sendToFederationManagerAndFile()

        WorkflowService.deleteGroupCa(uid).then(
            function() {
                $scope.getCaGroups()
            },
            function (result) {
                console.log("Unable to delete CA, " + result);
            }
        );
    }

    $scope.sendToFederationManagerAndFile = function() {
        $scope.saveGraphPromise().then(
            function() {
                WorkflowService.sendToFederationManagerAndFile($rootScope.workflow.name).then(function(result) {
                    growl.success("Removed CA from graph and sent updated policy to federation manager");
                }, function(result) {
                    growl.error("Failed to transfer new policy to federation manager " + result.statusText);
                });
            }, function(error) {
                growl.error("Failed to save federation graph.  The policy may be out of date. Error: " + error.statusText);
            });
    };

      // This function is used instead of saveGraph when we want to execute some other code after the graph is saved
      $scope.saveGraphPromise = function() {
          if ($scope.workflow !== undefined) {
              var graphJSON = JointPaper.graph.toJSON();
              $rootScope.workflow.cells = graphJSON.cells;
              return WorkflowService.saveBpmnGraph(angular.toJson($rootScope.workflow))
          } else {
              console.log("WARNING: $scope.workflow is undefined in saveGraph");
          }
      }
}