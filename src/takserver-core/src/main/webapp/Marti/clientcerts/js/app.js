'use strict';

var app = angular.module('clientCertificateManager', ['ngRoute', 'ngResource',  'ngMessages', 'clientCertificateManagerControllers', 'clientCertificateManagerServices']);

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
            templateUrl: 'partials/clientCertificates.html',
            controller: 'ClientCertificatesListCtrl'
        }).
        when('/viewCertificate/:hash', {
            templateUrl: 'partials/viewCertificate.html',
            controller: 'ViewCertificateCtrl'
        }).
        otherwise({
            redirectTo: '/'
        });
    }]);