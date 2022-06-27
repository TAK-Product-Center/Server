'use strict';

var app = angular.module('deviceProfileManager', ['ngRoute', 'ngResource',  'ngMessages', 'deviceProfileManagerControllers', 'deviceProfileManagerServices']);

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
            templateUrl: 'partials/deviceProfiles.html',
            controller: 'deviceProfilesListCtrl'
        }).
        when('/editProfile/:name', {
            templateUrl: 'partials/editProfile.html',
            controller: 'editProfileCtrl'
        }).
        when('/sendProfile/:name', {
            templateUrl: 'partials/sendProfile.html',
            controller: 'sendProfileCtrl'
        }).
        otherwise({
            redirectTo: '/'
        });
    }]);