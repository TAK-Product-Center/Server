
"use strict";

angular.module('roger_federation.Core')
  .controller('ApplicationController', ['$http', '$scope', '$rootScope', '$state', '$cookieStore', 'AuthenticationService', 'Session', 'ConfigService', applicationController]);

function applicationController($http, $scope, $rootScope, $state, $cookieStore, AuthenticationService, Session, ConfigService) {

  // Setup some common display values.
  var session = $cookieStore.get("session");
  if (session) {
    $scope.currentUser = session.username;
  }

  $scope.$watch(function() {
      return AuthenticationService.getUserId();
    },
    function(newValue) {
      $scope.currentUser = newValue;
    });

  var heartBeat = function() {
//    ConfigService.getServerStatus().then(
//      function(res) {
//        setTimeout(heartBeat, 3000);
//        $scope.serverStatus = res;
//      },
//      function(reason) {
//        throw reason;
//      });
  };
  heartBeat();
}
