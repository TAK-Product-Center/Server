'use strict';

var app = angular.module('certificateManager', ['ngRoute', 'ngResource',  'ngMessages', 'certificateManagerControllers', 'certificateManagerServices']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});

app.directive('fileModel', ['$parse', function ($parse) {
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {
            var model = $parse(attrs.fileModel);
            var modelSetter = model.assign;
            
            element.bind('change', function(){
                scope.$apply(function(){
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
}]);

app.config(['$routeProvider',
                    function($routeProvider) {
	$routeProvider.
	when('/', {
		templateUrl: 'partials/federateCertificates.html',
		controller: 'FederateCertificatesListCtrl'
	}).
	when('/uploadCertificate', {
		templateUrl: 'partials/newCertificate.html',
		controller: 'FederateCertificateUploadCtrl'
	}).	
	when('/editFederateCAGroups/:id', {
	    templateUrl: 'partials/federateCAGroups.html',
	    controller: 'FederateCAGroupsCtrl'
	}).
	otherwise({
		redirectTo: '/'
	});
}]);