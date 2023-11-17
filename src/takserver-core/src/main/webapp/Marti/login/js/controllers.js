'use strict';

var loginControllers = angular.module('loginManagerControllers', []);

loginControllers.controller('loginController', ['$scope', '$location', 'loginService', '$window',
    function ($scope, $location, loginService, $window) {

        $scope.showLogin = true;
        $scope.showError = false;
        $scope.externalAuth = false;
        $scope.externalAuthName = null;

        $scope.getConfig = async function() {
            loginService.config.get(
                function(apiResponse) {
                    console.log(apiResponse);
                    $scope.externalAuth = true;
                    $scope.externalAuthName = apiResponse.data;
                },
                function() { $scope.showRmiError = true; });
        }

        $scope.loginWithAuthServer = async function() {
            window.location = "/login/auth";
        }

        $scope.onSubmit = async function (){
            var xhr = new XMLHttpRequest();
            var url = "/oauth/token?grant_type=password" +
                "&username=" + encodeURIComponent(username.value) +
                "&password=" + encodeURIComponent(password.value);
            xhr.responseType = 'json';
            xhr.open("POST", url, true);
            xhr.onload = function() {
                if (xhr.status == 200) {
                    window.location = document.referrer
                } else {
                    $scope.showError = true;
                    $scope.$apply();
                }
            };

            xhr.send();
        }

        $scope.getConfig();
    }]);