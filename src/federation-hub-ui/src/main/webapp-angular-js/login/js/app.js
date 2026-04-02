'use strict';

var app = angular.module('loginManager', ['ngCookies','ngRoute', 'ngStorage', 'ngResource',  'ngMessages', 'loginManagerControllers']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
        when('/', {
            templateUrl: '/login/partials/login.html',
            controller: 'loginController'
        }).
        otherwise({
            redirectTo: '/'
        });
    }]);