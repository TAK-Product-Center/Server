'use strict';

var menubarControllers = angular.module('menubarControllers', []);

menubarControllers.controller('IsSecureCtrl', [
    '$rootScope',
    '$scope',
    'isSecureService',
    function($rootScope, $scope, isSecureService) {
        $rootScope.alerts = [];


        console.log("trying!")
        isSecureService.query(function(response) {
            if (response.data == "true") {
                $rootScope.alerts.push({
                    msg : "We are Secure!"
                });
            } else {
                console.log(response);
                $rootScope.alerts
                .push({
                    msg : 'The following ports support unsecure connections: ' + response.data,
                    type : 'danger'
                });
            }
        });

        $rootScope.closeAlert = function(index) {
            console.log("closing");
            $rootScope.alerts.splice(index, 1);
        }
    } ]);

var menubarServices = angular.module('menubarServices', []);

menubarServices.factory('isSecureService', function($resource) {
    return $resource('/Marti/api/security/isSecure', {}, {
        'query': {method: "GET", isArray: false}
    });
});