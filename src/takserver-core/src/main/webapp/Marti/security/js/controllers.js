'use strict'

var securityConfigControllers = angular.module('securityConfigControllers', []);

securityConfigControllers.controller('SecurityAuthenticationController', ['$scope', '$timeout','securityConfigService', 'authConfigService', function($scope, $timeout, securityConfigService, authConfigService) {
	function getSecConfig(){
		securityConfigService.query(
				function(response){ $scope.secConfig = response.data},
				function(response){$scope.secConfig = null});
	}

	$scope.noLDAP = false;
	
	function getAuthConfig(){
		authConfigService.query(
				function(response) { 
					if(response.data != null){
						$scope.authConfig = response.data;
					}
					else{
						$scope.authConfig = null;
						$scope.noLDAP = true;
					}
				},
				function(response) { $scope.authConfig = null});
	}

	$scope.showTest = false;
	$scope.testPassed = false;
	$scope.testAuthConfig = function(){
		$scope.showTest = false;
		$scope.testPassed = false;

		authConfigService.test(
			function(response){
				$scope.testPassed = true;
				$scope.showTest = true;
			},
			function(response){
				$scope.showTest = true;
			}
		);

	}

	$scope.$on('$viewContentLoaded', function(event) {
		getSecConfig();
		getAuthConfig();
    });
}]);

securityConfigControllers.controller('SecurityModifyConfController', ['$scope', '$location', 'securityConfigService', function($scope, $location, securityConfigService){
	function getSecConfig(){
		securityConfigService.query(
				function(response){ $scope.secConfig = response.data},
				function(response){$scope.secConfig = null});
	}
	
	$scope.saveSecConfig = function(config){
		securityConfigService.update(config,
				function(response){$location.path('/');},
				function(response){alert(response.data.data)}
				);
	}
	$scope.$on('$viewContentLoaded', function(event) {
		getSecConfig();
    });
}]);

securityConfigControllers.controller('AuthModifyConfController', ['$scope', '$location', 'authConfigService', function($scope, $location, authConfigService) {
	function getAuthConfig(){
		authConfigService.query(
				function(response) { 
					if(response.data != null){
						$scope.authConfig = response.data;
					}
					else{
						$scope.authConfig = null;
					}
				},
				function(response) { $scope.authConfig = null});
	}
	$scope.saveAuthConfig = function(config){
		authConfigService.update(config, 
				function(response){$location.path('/');}, 
				function(response){}
				);
	}
	
	$scope.$on('$viewContentLoaded', function(event) {
		getAuthConfig();
    });
}]);