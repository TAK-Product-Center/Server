var app = angular.module('takClassificationApp', ["ngRoute", "ngDraggable", "ngDialog"]);

app.config(function($routeProvider) {
    $routeProvider
    .when("/", {
      templateUrl : "default.view.html"
    })
    .when("/set_caveats_for_classification", {
      templateUrl : "set_caveats_for_classification.view.html",
      controller: "setCaveatsController"
    })
    .otherwise({ redirectTo: '/' });
  });
