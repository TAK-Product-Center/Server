
"use strict";

angular.module('fed')
  .controller('EditAttributesController', function($scope, $modalInstance, $log, growl, attributes) {

    $scope.initialize = function() {
      $scope.attributes = attributes;
    };

    $scope.submit = function() {
      $modalInstance.close('ok');
    };

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };
  });
