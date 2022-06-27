
"use strict";

angular.module('roger_federation.Workflows')
  .controller('LoadWorkflowController', ['$scope', '$state', '$stateParams', '$modalInstance', '$log', 'growl', 'configParams', 'WorkflowService', loadWorkflowController]);

function loadWorkflowController($scope, $state, $stateParams, $modalInstance, $log, growl, configParams, WorkflowService) {

  $scope.rowCollection = [];
  $scope.displayedCollection = [];
  $scope.itemsByPage = 15;
  $scope.selectedRowId = -1;

  $scope.initialize = function() {
    if (configParams !== undefined) {
      $scope.mode =  configParams.mode;
      $scope.diagramType = configParams.diagramType;
    } else {
      $scope.mode =  $stateParams.mode;
      $scope.diagramType = $stateParams.diagramType;
    }
    var successFunc = function(workflowList) {
      $scope.rowCollection = workflowList;
      $scope.displayedCollection = [].concat($scope.rowCollection);
    };

    var failureFunc = function(result) {
      growl.error("Failed getting workflow names. Error: " + result.data.error);
      $scope.rowCollection.length = 0;
      $scope.displayedCollection = [].concat($scope.rowCollection);
    };
    WorkflowService.getWorkflowDescriptors().then(successFunc, failureFunc);
  };

  $scope.diagramTypeFilter = function(diagram) {
    return diagram.diagramType === $scope.diagramType;
  };

  //fires when table rows are selected
  $scope.$watch('displayedCollection', function(row) {
    $scope.selectedRowId = -1;
    //get selected row
    row.filter(function(r) {
      if (r.isSelected) {
        $scope.selectedRowId = r.id;
      }
    });
  }, true);


  $scope.submit = function() {
    var workflowArray = $scope.displayedCollection.filter(function(row) {
      return row.id === $scope.selectedRowId;
    });

    var workflow = workflowArray[0];
    if ($scope.mode === "choose") {
      $modalInstance.close(workflow);
    } else {
      $state.go('workflows.editor', {
        workflowId: workflow.name
      });
      $modalInstance.close('ok');
    }
    $scope.$close(true);
  };

  $scope.cancel = function() {
    $modalInstance.dismiss('cancel');
  };
}
