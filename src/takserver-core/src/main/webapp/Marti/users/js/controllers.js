'use strict';

var groupManagerControllers = angular.module('groupManagerControllers', []);

groupManagerControllers.controller('EmptyCtrl', ['$scope', function ($scope) {
	// do nothing
}]);

// User List Controller
groupManagerControllers.controller('UserListCtrl', ['$scope', '$http', '$timeout', function ($scope, $http, $timeout) {

  (function poll() {
		  $http.get('/Marti/api/users/all')
		  .then(function(response){
		    $scope.users = response.data.data;
		  })
		  .catch(function(response){
		    $scope.users = null;
		  })
		  .finally(function(){
		    $timeout(poll, 2000); //wait for request to return (either error or success) before waiting for poll interval
		  });
	  })();

}]);

// User Details Controller
groupManagerControllers.controller('UserDetailsCtrl', ['$scope', '$routeParams', '$http', '$timeout', function ($scope, $routeParams, $http, $timeout) {
/**
	(function poll() {
		$http.get('/Marti/api/users/' + $routeParams.id).success(function(user) {
			  $scope.user = user.data.user;
			  $scope.groups = user.data.groups;
			})
			.error(function(data, status, headers, config) {
				$scope.user = null;
				$scope.groups = null;
			  });
		
        $timeout(poll, 2000);
       
    })();
**/
   (function poll() {
		  $http.get('/Marti/api/users/' + $routeParams.id)
		  .then(function(response){
		    $scope.user = response.data.data.user;
		    $scope.groups = response.data.data.groups;
		  })
		  .catch(function(response){
		    $scope.user = null;
		    $scope.groups = null
		  })
		  .finally(function(){
		    $timeout(poll, 2000); //wait for request to return (either error or success) before waiting for poll interval
		  });
	  })();
}]);

