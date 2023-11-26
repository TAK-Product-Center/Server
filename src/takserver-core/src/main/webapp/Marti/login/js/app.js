'use strict';

var app = angular.module('loginManager', ['ngRoute', 'ngResource',  'ngMessages', 'loginManagerControllers', 'loginManagerServices']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
        when('/', {
            templateUrl: '/Marti/login/partials/login.html',
            controller: 'loginController'
        }).
        otherwise({
            redirectTo: '/'
        });
    }]);