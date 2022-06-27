'use strict';

var app = angular.module('inputManager', ['ngRoute', 'ngResource',  'ngMessages', 'inputManagerControllers', 'inputManagerServices']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.config(['$routeProvider',
                    function($routeProvider) {
	$routeProvider.
	when('/', {
		templateUrl: 'partials/list.html',
		controller: 'InputListCtrl'
	}).
	when('/createInput', {
		templateUrl: 'partials/new.html',
		controller: 'InputCreationCtrl'
	}).
	when('/modifyInput/:id', {
		templateUrl: 'partials/modify.html',
		controller: 'InputModificationCtrl'
	}).
	otherwise({
		redirectTo: '/'
	});
}]);

