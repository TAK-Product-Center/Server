
"use strict";

angular.module('roger_federation.Workflows')
  .controller('HomeController', ['$scope', '$state', '$stateParams', 'growl', 'WorkflowService', homeController]);

function homeController($scope, $state, $stateParams, growl, WorkflowService) {
  $scope.rowCollection = [];
  $scope.displayedCollection = [];
  $scope.itemsByPage = 15;
  $scope.selectedRowId = -1;
  $scope.mode = $stateParams.mode;
  $scope.diagramType = $stateParams.diagramType;

  var successFunc = function (workflowList) {
    $scope.rowCollection = workflowList;
    $scope.displayedCollection = [].concat($scope.rowCollection);
    $scope.firstWF = workflowList[0]
    var forwarded_already = localStorage.getItem("forwarded_dash");

    if (forwarded_already != "true") {
      if ($scope.firstWF == undefined) {
        localStorage.setItem("forwarded_dash", true);
        $state.go('federations-new');
      }
      else if ($scope.firstWF != undefined && $scope.firstWF.name != undefined) {
        localStorage.setItem("forwarded_dash", true);
        $state.go('workflows.editor', {
          workflowId: $scope.firstWF.name
        });
      }
    }
  };

  var failureFunc = function (result) {
    growl.error("Failed getting workflow names. Error: " + result.data.error);
    $scope.rowCollection.length = 0;
    $scope.displayedCollection = [].concat($scope.rowCollection);
  };

  WorkflowService.getWorkflowDescriptors().then(successFunc, failureFunc);
}