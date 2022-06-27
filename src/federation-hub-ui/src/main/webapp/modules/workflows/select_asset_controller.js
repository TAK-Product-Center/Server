
"use strict";

angular.module('roger_federation.Workflows')
.controller('SelectAssetController',
	[ '$scope', '$state', '$modalInstance', '$log', 'growl', 'AssetService', selectAssetController ]);

function selectAssetController($scope, $state, $modalInstance, $log, growl, AssetService) {

    $scope.rowCollection = [];
    $scope.displayedCollection = [];
    $scope.itemsByPage=15;
    $scope.selectedRowId = -1;


    $scope.initialize = function () {
	AssetService.getAssetFilenames().then(function (AssetList) {
	    $scope.rowCollection = AssetList;
	    $scope.displayedCollection = [].concat($scope.rowCollection);
	}, function(result) {
	    growl.error("Failed getting asset filenames. Error: " + result.data.error);
	    $scope.rowCollection.length = 0;
	    $scope.displayedCollection = [].concat($scope.rowCollection);
	});
    };


    //fires when table rows are selected
    $scope.$watch('displayedCollection', function (row) {
	$scope.selectedRowId = -1;
	//get selected row
	row.filter(function (r) {
	    if (r.isSelected) {
		$scope.selectedRowId = r.id;
	    }
	});
    }, true);


    $scope.submit = function () {

	var selectedAssetArray = $scope.displayedCollection.filter(function( row ) {
	  return row.id === $scope.selectedRowId;
	});

	var AssetFilename = selectedAssetArray[0];

	$state.go('workflows.editor.instantiation', {workflowId : $state.params.workflowId });
	$modalInstance.close(AssetFilename);
	$scope.$close(true);
    };

    $scope.cancel = function () {
	$state.go('workflows.editor', {workflowId : $state.params.workflowId });
	$modalInstance.dismiss('cancel');
    };
}



