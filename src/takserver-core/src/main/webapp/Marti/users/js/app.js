'use strict';

var contactPath = angular.module('groupManager', ['ngRoute', 'groupManagerControllers']);

contactPath.config(['$routeProvider',
                    function($routeProvider) {
	$routeProvider.
	when('/', {
		templateUrl: 'partials/empty.html',
		controller: 'EmptyCtrl'
	}).
	when('/view/:id', {
		templateUrl: 'partials/view.html',
		controller: 'UserDetailsCtrl'
	}).
	otherwise({
		redirectTo: '/'
	});
}]);

