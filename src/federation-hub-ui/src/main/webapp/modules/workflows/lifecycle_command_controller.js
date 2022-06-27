
"use strict";

angular.module('roger_federation.Workflows')
  .controller('LifecycleCommandController', ['$scope', '$modalInstance', '$log', 'growl', '$stateParams', 'WorkflowService', 'WorkflowTemplate', lifecycleCommandController]);

function lifecycleCommandController($scope, $modalInstance, $log, growl, $stateParams, WorkflowService, WorkflowTemplate) {
  $scope.lifecycleType = "";
  $scope.lifecycle = {};
  $scope.newCommand = {
    type: "PrimeSoapRequest"
  };

  $scope.initialize = function() {
    var lifecycleEventId = $stateParams.lifecycleEventId;
    $scope.lifecycle = WorkflowTemplate.getLifecycleEvents().filter(function(item) {
      return item.id === lifecycleEventId;
    })[0];

    $scope.lifecycleType = $scope.lifecycle['@class'].substring($scope.lifecycle['@class'].lastIndexOf('.') + 1);
    if ($scope.lifecycleType === "OnAuth") {
      $scope.lifecycleType += " (" + $scope.lifecycle.identity + ")";
    }
  };

  $scope.refresh = function() {
    WorkflowService.getWorkflow(WorkflowTemplate.getId()).then(function(workflow) {
      WorkflowTemplate.setLifecycleEvents(workflow.lifecycleEvents);
      $scope.initialize();
    }, function(result) {
      growl.error("Failed to retrieve workflow. Error: " + result.data.error);
    });
  };

  $scope.deleteLifecycleCommand = function(command) {
    WorkflowService.deleteLifecycleCommand(WorkflowTemplate.getId(), $scope.lifecycle.id, command.id).then(function() {
      $scope.refresh();
    }, function(result) {
      growl.error("Failed to delete Lifecycle Event. Error: " + result.statusText);
    });
  };

  $scope.addLifecycleCommand = function() {
    var newSoapCmd = {
      "@class": "com.bbn.roger_federation.domain.ims." + $scope.newCommand.type
    };
    WorkflowService.addCommandToLifeCycleEvent(WorkflowTemplate.getId(), $scope.lifecycle.id, newSoapCmd).then(function() {
      $scope.refresh();
    }, function(result) {
      growl.error("Failed to create Lifecycle command. Error: " + result.statusText);
    });
  };

  $scope.updateLifecycleCommand = function(command) {
    delete command.lifecycleEvent;
    WorkflowService.updateLifeCycleCommand(WorkflowTemplate.getId(), $scope.lifecycle.id, command).then(function() {
      // $scope.refresh();
    }, function(result) {
      growl.error("Failed to update Lifecycle command. Error: " + result.statusText);
    });
  };

  $scope.remove_argument = function(command, index) {
    command.arguments.splice(index, 1);
    $scope.updateLifecycleCommand(command);
  };
  $scope.add_argument = function(command) {
    command.arguments.push("");
    $scope.updateLifecycleCommand(command);
  };

  $scope.submit = function() {
    $modalInstance.close('ok');
  };
}
