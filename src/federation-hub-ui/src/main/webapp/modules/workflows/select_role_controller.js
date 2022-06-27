
"use strict";

angular.module('roger_federation.Workflows')
.controller('SelectRoleController',
	[ '$scope', '$stateParams', '$modalInstance', '$log', 'growl', 'WorkflowTemplate', 'WorkflowGraphFactory', 'RoleProductSetService', selectRoleController ]);

function selectRoleController($scope, $stateParams, $modalInstance, $log, growl, WorkflowTemplate, WorkflowGraphFactory, RoleProductSetService) {


    $scope.treeOptions = {
	    nodeChildren: "children",
	    dirSelectable: true,
	    multiSelection : false
    };

    $scope.selectedNode = undefined;
    $scope.roleData = [];


    $scope.initialize = function () {
    	var roleSetData = WorkflowTemplate.getRoleSetData();
    	if (roleSetData === undefined) {
    	    var apSetId = WorkflowTemplate.getRoleProductSet();
    	    if (apSetId !== undefined) {
    		RoleProductSetService.getRoleProductSet(apSetId).then(function (result) {
    		    WorkflowTemplate.setRoleSetData(result.roles);
    		    $scope.roleData = result.roles;
    		}, function() {
    		    growl.error("Failed to acquire RoleProduct Set Data.");
    		});

    	    } else {
    		growl.error("Role/Product Set is Unknown!");
    	    }
    	} else {
    	    $scope.roleData = roleSetData;
    	}
    };

    $scope.submit = function() {
		RoleProductSetService.setLatestSelectedRole($scope.selectedNode);
	
		$modalInstance.close('ok');
    };


    $scope.cancel = function() {
    	$modalInstance.dismiss('cancel');
    };

};


