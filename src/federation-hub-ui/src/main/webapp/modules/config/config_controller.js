
'use strict';

angular.module('roger_federation.Config')

.controller('ConfigController', ['$scope', '$rootScope', '$state', '$stateParams', 'growl', 'ConfigService', configController]);


function configController($scope, $rootScope, $state, $stateParams, growl, ConfigService) {
  $rootScope.$state = $state;
  $rootScope.$stateParams = $stateParams;

  $scope.serverInfo = {
    roger_federation: {},
    fuseki: {
      uri: ""
    }
  };

  $scope.initialize = function() {
    $scope.serverInfo = ConfigService.getroger_federationServerInfo();
  };

  $scope.submit = function(serverInfo) {
    ConfigService.setServerInfo(serverInfo);
  };
}
