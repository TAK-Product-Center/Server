'use strict';

var loginControllers = angular.module('loginManagerControllers', []);

loginControllers.controller('loginController', ['$scope', '$location', '$window', '$http', '$localStorage',
    function ($scope, $location, $window, $http, $localStorage) {

        $scope.showLogin = true;
        $scope.showError = false;
        $scope.externalAuth = false;
        $scope.externalAuthName = null;

        function init() {
            $http.get('/login/authserver')
            .success(function(apiResponse) {
                console.log(apiResponse)
                $scope.externalAuth = true;
                $scope.externalAuthName = apiResponse.data;
            })
            .error(function(apiResponse){
                console.log(apiResponse)
            });
        }

        init()
        
        $scope.loginWithAuthServer = async function() {
            window.location = "/login/auth";
        }

        $scope.onSubmit = async function () {
            console.log(username.value, password.value)

            $http.post('/oauth2/token', {
                username: username.value,
                password: password.value
            })
            .success(function(apiResponse) {
                console.log(apiResponse)
                if (apiResponse.accessToken) {
                    $window.location.href = '/home'
                }
            })
            .error(function(apiResponse){
                alert('An error occurred adding the group. Please correct the errors and resubmit.');
                console.log(apiResponse)
            });
        }

        function addMinutes(date, minutes) {
            return new Date(date.getTime() + minutes*60000);
        }
    }]);