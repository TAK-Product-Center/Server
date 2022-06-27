
'use strict';

angular.module('roger_federation.Logout')

.controller('LogoutController',
	['$scope', '$rootScope', '$state', '$modalInstance', 'AuthenticationService', function ($scope, $rootScope, $state, $modalInstance, AuthenticationService) {
	    $rootScope.currentPage = 'Logout';

	    $scope.logout = function () {
		AuthenticationService.logout();
		$modalInstance.close('ok');
		$scope.$close(true);
	    };


	    $scope.cancel = function () {
		$scope.$dismiss();
		$modalInstance.dismiss('cancel');
	    };

	}]);