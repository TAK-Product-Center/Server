'use strict';

var app = angular.module('OAuth2Manager', ['ngRoute', 'ngResource',  'ngMessages', 'OAuth2ManagerControllers', 'OAuth2ManagerServices']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
        when('/', {
            templateUrl: 'partials/tokens.html',
            controller: 'TokenListCtrl'
        }).
        when('/viewToken/:token', {
            templateUrl: 'partials/viewToken.html',
            controller: 'ViewTokenCtrl'
        }).
        otherwise({
            redirectTo: '/'
        });
    }]);