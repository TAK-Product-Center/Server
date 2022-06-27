'use strict';

var app = angular.module('databaseManager', ['ngRoute', 'ngResource',  'ngMessages', 'databaseManagerControllers', 'databaseManagerServices']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.config(['$routeProvider',
                    function($routeProvider) {
	$routeProvider.
	when('/', {
		templateUrl: 'partials/displayConfig.html',
		controller: 'DatabaseDisplayConfigCtrl'
	}).
	when('/modifyConfig', {
	    templateUrl: 'partials/editConfig.html',
	    controller: 'DatabaseModConfigCtrl'
	}).
	otherwise({
		redirectTo: '/'
	});
}]);

