
"use strict";

angular.module('roger_federation.Workflows')
.controller('SelectProductController',
	[ '$scope', '$modalInstance', '$log', 'growl', 'WorkflowTemplate', 'WorkflowGraphFactory', 'RoleProductSetService', selectProductController ]);

function selectProductController($scope, $modalInstance, $log, growl, WorkflowTemplate, WorkflowGraphFactory, RoleProductSetService) {

    $scope.treeOptions = {
	    nodeChildren: "children",
	    dirSelectable: true,
	    multiSelection : false
    };

    $scope.selectedNode = undefined;
    $scope.productData = [];

    $scope.initialize = function () {
    	var productSetData = WorkflowTemplate.getProductSetData();
    	if (productSetData === undefined) {
    	    var apSetId = WorkflowTemplate.getRoleProductSet();
    	    if (apSetId !== undefined) {
    		RoleProductSetService.getRoleProductSet(apSetId).then(function (result) {
    		    WorkflowTemplate.setProductSetData(result.products);
    		    $scope.productData = result.products;
    		}, function(result) {
    		    growl.error("Failed to acquire role-product set. Error: " + result.data.error);
    		});

    	    } else {
    		growl.error("Role/Product Set is Unknown!");
    	    }
    	} else {
    	    $scope.productData = productSetData;
    	}
    };


    $scope.submit = function() {
    RoleProductSetService.setLatestSelectedProduct($scope.selectedNode);
    	
	$modalInstance.dismiss('ok');
    };


    $scope.cancel = function() {
	$modalInstance.dismiss('cancel');
    };

};

