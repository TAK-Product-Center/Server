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
	when('/createStreamingDataFeed', {
		templateUrl: 'partials/newStreamingDataFeed.html',
		controller: 'DataFeedCreationCtrl'
	}).
	when('/modifyStreamingDataFeed/:name', {
		templateUrl: 'partials/modifyStreamingDataFeed.html',
		controller: 'DataFeedModificationCtrl'
	}).
	when('/createPluginDataFeed', {
		templateUrl: 'partials/newPluginDataFeed.html',
		controller: 'DataFeedCreationCtrl'
	}).
	when('/modifyPluginDataFeed/:name', {
		templateUrl: 'partials/modifyPluginDataFeed.html',
		controller: 'DataFeedModificationCtrl'
	}).
	when('/modifyFederationDataFeed/:name', {
		templateUrl: 'partials/modifyFederationDataFeed.html',
		controller: 'DataFeedModificationCtrl'
	}).
	otherwise({
		redirectTo: '/'
	});
}]);

