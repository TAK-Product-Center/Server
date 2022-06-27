var app = angular.module('takUserManagementApp', ["ngRoute", "ngDraggable", "ngDialog"]);

app.config(function($routeProvider) {
    $routeProvider
    .when("/", {
      templateUrl : "default.view.html"
    })
    .when("/new_user", {
      templateUrl : "new_user.view.html",
      controller: "newUserController"
    })
    .when("/new_users", {
      templateUrl : "new_users.view.html",
      controller: "newUsersController"
    })
    .when("/user_edit_groups", {
      templateUrl : "user_edit_groups.view.html",
      controller: "userEditGroupController"
    })
    .when("/user_change_password", {
      templateUrl : "user_change_password.view.html",
      controller: "userChangePasswordController"
    })
    .when("/group_list_users", {
      templateUrl : "group_list_users.view.html"
    })
    .otherwise({ redirectTo: '/' });
  });
