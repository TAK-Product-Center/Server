'use strict';

var app = angular.module('wizard', ['ngRoute', 'ngResource', 'setupControllers', 'setupServices', 'ngAnimate', 'ui.bootstrap', 'ui.router']);

app.filter('encodeURIComponent', function() {
    return window.encodeURIComponent;
});


app.config(function ($stateProvider) {
    $stateProvider
    .state('security', {
        url: '/security',
        views: {
            "@": {
                templateUrl: "partials/security.html",
                controller: "SecurityCtrl"
            }
        }
    })
    .state('security.viewSec', {
        views: {
            "security_config@security": {
                templateUrl: 'partials/security.viewSec.html',
                controller: 'CheckSecConfigCtrl'
            }
        }
    })
    .state('security.modifySec', {
        url: '/modifySec',
        views: {
            "security_config": {
                templateUrl: 'partials/security.modifySec.html',
                controller: 'SecurityCtrl'
            }
        }
    })
    .state('security.ldap', {
        url: '/ldap',
        views: {
            "security_config": {
                templateUrl: "partials/security.viewSec.html",
                controller: 'CheckSecConfigCtrl'
            },
            "security_auth": {
                templateUrl: 'partials/security.ldap.html',
                controller: 'SecurityCtrl'
            }
        }
    })
    .state('security.ldap.view', {
        url: '',
        templateUrl: 'partials/security.ldap.view.html',
        controller: 'SecurityCtrl'
    })
    .state('security.ldap.modify', {
        url: '/modify',
        templateUrl: 'partials/security.ldap.modify.html',
        controller: 'SecurityCtrl'
    })
    .state('federation', {
        url: '/federation',
        templateUrl: 'partials/federation.html',
        controller: 'FederationCtrl'
    })
});

////Show state tree
//app.run(function($uiRouter) {
//    var Visualizer = window['ui-router-visualizer'].Visualizer;
//    $uiRouter.plugin(Visualizer);
//});