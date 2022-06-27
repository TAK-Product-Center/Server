'use strict';

var app = angular.module('securityConfigManager', ['ngRoute', 'ngResource',  'ngMessages', 'securityConfigControllers', 'securityConfigServices']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.config(['$routeProvider',
                    function($routeProvider) {
	$routeProvider.
	when('/', {
		templateUrl: 'partials/viewConfig.html',
		controller: 'SecurityAuthenticationController'
	}).
	when('/modifyAuthConfig', {
	    templateUrl: 'partials/modifyAuth.html',
	    controller: 'AuthModifyConfController' 
	}).
	when('/modifySecConfig', {
	    templateUrl: 'partials/modifySec.html',
	    controller: 'SecurityModifyConfController'
	}).
	otherwise({
		redirectTo: '/'
	});
}]);