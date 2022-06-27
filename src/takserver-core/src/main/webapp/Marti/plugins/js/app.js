'use strict';

var app = angular.module('pluginManager', ['ngRoute', 'ngResource', 'ngMessages', 'ngSanitize', 'pluginManagerControllers', 'pluginManagerServices']);

app.config(['$routeProvider',
	    function($routeProvider) {
		$routeProvider.
		    when('/', {
			templateUrl: 'partials/list.html',
			controller: 'PluginListCtrl'
		    });
	    }]);
