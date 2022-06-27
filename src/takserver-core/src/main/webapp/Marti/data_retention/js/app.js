'use strict';

var app = angular.module('dataRetention', ['ngRoute', 'ngResource',  'ngMessages', 'DataRetentionControllers', 'DataRetentionServices', 'ui.bootstrap']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
        when('/', {
            templateUrl: 'partials/viewPolicies.html',
            controller: 'ViewPoliciesCtrl'
        }).
        when('/mission-archive', {
            templateUrl: 'partials/missionArchive.html',
            controller: 'MissionArchiveCtrl'
        }).
        otherwise({
            redirectTo: '/'
        });
    }]);