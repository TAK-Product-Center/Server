'use strict';

var app = angular.module('metricsManager', ['ngRoute', 'ngResource', 'metricsManagerControllers', 'metricsManagerServices']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.config(['$routeProvider', function($routeProvider) {
	$routeProvider.when('/', {
		templateUrl: 'partials/dashboard.html',
	    controller: 'DashboardCtrl'
	}).when('/connection/:uid', {
		templateUrl: 'partials/connection.html',
	    controller: 'ConnectionCtrl'
	}).otherwise({
		redirectTo: '/'
	});
}]);

