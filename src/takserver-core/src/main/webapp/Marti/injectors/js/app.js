'use strict';

var app = angular.module('injectorManager', ['ngAnimate', 'ui.bootstrap', 'ngRoute', 'ngResource',  'ngMessages', 'injectorManagerControllers', 'injectorManagerServices']);

app.config(['$routeProvider',
                    function($routeProvider) {
	$routeProvider.
	when('/', {
		templateUrl: 'partials/list.html',
		controller: 'InjectorListCtrl'
	}).
	when('/createInjector', {
		templateUrl: 'partials/new.html',
		controller: 'InjectorCreationCtrl'
	}).
	otherwise({
		redirectTo: '/'
	});
}]);

