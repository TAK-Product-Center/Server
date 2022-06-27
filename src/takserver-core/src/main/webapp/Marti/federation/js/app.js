'use strict';

var app = angular.module('federationManager', ['ngAnimate', 'ui.bootstrap','ngRoute', 'ngResource',  'ngMessages', 'federationManagerControllers', 'federationManagerServices']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.filter('formatEnabled', function() {
    return function(booleanValue) {return booleanValue ? 'Enabled' : 'Disabled';};
});

app.filter('incomingOutgoing', function() {
    return function(clientValue) {return clientValue ? 'Self' : 'Remote';};
});

app.directive('errorflagvalidation', function() {
    return {
	require: 'ngModel',
	link: function(scope, elm, attrs, ctrl) {
	    ctrl.$validators.errorflagvalidation = function(modelValue, viewValue) {
		if (modelValue) {
		    return false;
		} else {
		    return true;
		}
	    };
	}
    };
});

app.config(['$routeProvider',
            function($routeProvider) {
		$routeProvider.
		    when('/', {
			templateUrl: 'partials/federates.html',
			controller: 'FederatesListCtrl'
		    }).
		    when('/createOutgoingConnection', {
			templateUrl: 'partials/newOutgoingConnection.html',
			controller: 'OutgoingConnectionCreationCtrl'
		    }).
		    when('/listFederates', {
			templateUrl: 'partials/federates.html',
			controller: 'FederatesListCtrl'
		    }).
		    when('/editFederateGroups/:id', {
			templateUrl: 'partials/federateGroups.html',
			controller: 'FederateGroupsCtrl'
		    }).
		    when('/listFederateContacts/:id', {
			templateUrl: 'partials/federateContacts.html',
			controller: 'FederateContactsListCtrl'
		    }).
		    when('/editFederateDetails/:id', {
			templateUrl: 'partials/editFederateDetails.html',
			controller: 'FederateDetailsCtrl'
		    }).
		    when('/searchGroups', {
			templateUrl: 'partials/groupSearch.html',
			controller: 'GroupSearchCtrl'
		    }).
		    when('/modifyConfig',{
			templateUrl: 'partials/modifyFederationConfig.html',
			controller: 'FederationConfigCtrl'
		    }).
		    otherwise({
			redirectTo: '/'
		    });
	    }]);

