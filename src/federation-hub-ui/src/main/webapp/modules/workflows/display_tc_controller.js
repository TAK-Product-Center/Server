
"use strict";

angular.module('roger_federation.Workflows')
  .controller('TemplateContainerController', ['$scope', '$state', '$stateParams', 'FileSaver', 'Blob', '$log', '$uibModalInstance', 'data', loadTemplateController]);

function loadTemplateController($scope, $state, $stateParams, FileSaver, Blob, $log, $uibModalInstance, data) {

  $scope.jsonData = {};
  $scope.filename = 'template_container.json';
  
  $scope.initialize = function() {
    if (data !== undefined) {
  	  $scope.jsonData = data;
    } else {
      $scope.jsonData = "{error: 'undefined'}";
    }
  };

  $scope.download = function() {
	  var data = new Blob([JSON.stringify($scope.jsonData)], {type: 'text/json:charset=utf-8'});
	  FileSaver.saveAs(data, 'template_container.json');
	  $uibModalInstance.close('ok');
  };  
}
