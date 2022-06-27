
'use strict';

angular.module('roger_federation.Authentication')

.controller('AuthenticationController', ["$scope", "$rootScope", "$state", '$modalInstance', "$cookieStore", "AuthenticationService", "AUTH_EVENTS", 'ConfigService',
  function($scope, $rootScope, $state, $modalInstance, $cookieStore, AuthenticationService, AUTH_EVENTS, ConfigService) {

    $scope.credentials = {};

    AuthenticationService.logout();

    var isServerInfoSet = false;
    var heartBeatTimer;

    $scope.initialize = function() {
      $scope.serverInfo = ConfigService.getroger_federationServerInfo();
      $scope.credentials = {
        username: 'roger_federation',
        password: 'roger_federation'
      };

      var heartBeat = function() {
        ConfigService.getServerStatus().then(
          function(res) {
            heartBeatTimer = setTimeout(heartBeat, 2000);
            $scope.serverStatus = res;
            if ($scope.serverStatus.roger_federationOK) {
              if (isServerInfoSet === false) {
                ConfigService.setServerInfoFromHAL();
                isServerInfoSet = true;
              }
            } else {
              isServerInfoSet = false;
            }
          },
          function(reason) {
            throw reason;
          });
      };
      heartBeat();
    };

    $scope.$on('$destroy', function() {
      clearTimeout(heartBeatTimer); //destroy heartBeat timer
    });

    $scope.$on(AUTH_EVENTS.loginFailed, function(event, data) {
      $scope.error = data;
    });

    //	    $scope.$on(AUTH_EVENTS.loginSuccess, function() {
    //		$state.go('home');
    //	    });

    $scope.submit = function(credentials) {
      if (AuthenticationService.login(credentials)) {
        $rootScope.$broadcast(AUTH_EVENTS.loginSuccess);
        $modalInstance.close('ok');
        $scope.$close(true);
      } else {
        $rootScope.$broadcast(AUTH_EVENTS.loginFailed, response);
      };
      /*
      		AuthenticationService.login(credentials).then(function() {
      		    $rootScope.$broadcast(AUTH_EVENTS.loginSuccess);
      		}, function(response) {
      		    $rootScope.$broadcast(AUTH_EVENTS.loginFailed, response);
      		});
      */


    };
  }
]);
