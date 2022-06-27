
"use strict";

angular.module('roger_federation.Workflows')
  .controller('AddPolicyFilterController', ['$rootScope', '$scope', '$stateParams', '$modalInstance', '$log', '$cookieStore', 'growl', 'WorkflowTemplate', 'WorkflowService', 'OntologyService', 'JointPaper', addPolicyFilterController]);

function addPolicyFilterController($rootScope, $scope, $stateParams, $modalInstance, $log, $cookieStore, growl, WorkflowTemplate, WorkflowService, OntologyService, JointPaper) {

    $scope.initialize = function() {
    };

    $scope.submit = function() {

    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };




};