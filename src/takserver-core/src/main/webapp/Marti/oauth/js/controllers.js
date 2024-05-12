'use strict';

var oauth2ManagerControllers = angular.module('OAuth2ManagerControllers', []);

oauth2ManagerControllers.controller('TokenListCtrl', ['$scope', '$location', 'TokenServices',
    function ($scope, $location, TokenServices) {

        $scope.showRmiError = false;

        $scope.getAll = function() {
            TokenServices.tokens.query(
                function(apiResponse) {$scope.tokens = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.toggle = function () {
            angular.forEach($scope.tokens, function(token) { token.selected = !token.selected; });
        }

        $scope.revokeToken = function(token) {
            if (confirm('Are you sure you want to revoke the token?')) {
                TokenServices.tokens.revoke({token:token.token},
                    function(apiResponse) {$scope.getAll();},
                    function() {alert('An unexpected error occurred revoking the token.');});
            }
        }

        $scope.getSelected = function () {
            var tokens = '';
            angular.forEach($scope.tokens, function(token) {
                if (token.selected) {
                    tokens += token.token + ',';
                }
            });
            return tokens;
        }

        $scope.revokeSelected = function () {
            var tokens = $scope.getSelected();
            if (tokens.length == 0) {
                return;
            }

            if (!confirm("Are you sure you want to revoke the selected tokens?")) {
                return;
            }

            TokenServices.revoke.revoke({tokens:tokens},
                function(apiResponse) {$scope.getAll();},
                function() {alert('An unexpected error occurred revoking the token.');});
        }

        $scope.reverse = false;

        $scope.sortBy = function(sortPropertyName) {
            $scope.reverse = ($scope.sortPropertyName === sortPropertyName) ? !$scope.reverse : false;
            $scope.sortPropertyName = sortPropertyName;
        };

        $scope.getAll();
    }]);


oauth2ManagerControllers.controller('ViewTokenCtrl', ['$scope', '$location', 'ViewTokenService', '$routeParams',
    function ($scope, $location, ViewTokenService, $routeParams) {

        $scope.showRmiError = false;

        $scope.b64DecodeUnicode = function(str) {
            return decodeURIComponent(Array.prototype.map.call(atob(str), c =>
                '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
            ).join(''));
        }

        $scope.token = $routeParams.token;

        $scope.parsedToken = JSON.parse(
            $scope.b64DecodeUnicode(
                $scope.token.split('.')[1].replace('-', '+').replace('_', '/')
            )
        )

        $scope.backToTokens = function() {
            $location.path("/");
        };
    }]);


