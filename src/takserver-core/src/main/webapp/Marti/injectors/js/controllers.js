'use strict';

var injectorManagerControllers = angular.module('injectorManagerControllers', []);

injectorManagerControllers.controller('InjectorListCtrl', ['$scope', '$location', 'InjectorManagerService',
      function ($scope, $location, InjectorManagerService) {

    		$scope.showRmiError = false;
    		
			InjectorManagerService.query(function(apiResponse) {$scope.injectors = apiResponse.data;}, 
					function() {$scope.showRmiError = true;});

			$scope.deleteObject = function (uid, toInject) {

				if (uid.trim() != '' && toInject.trim() != '') {
		        	if (confirm('Delete the selected injector? \n\nUID: ' + uid + '\nInjection:' + toInject + ')')) {
			        	var ims = new InjectorManagerService();
			        	ims.$delete({uid:uid, toInject:toInject}, 
		        			function(data) {
		        				$scope.injectors = InjectorManagerService.query(function(apiResponse) {$scope.injectors = apiResponse.data});},
		        			function(data) {
		        				alert('An error occurred deleting the injector.')}
			        	);
		        	}
	        	}
	        };
        }]);


injectorManagerControllers.controller('InjectorCreationCtrl', ['$scope', '$location', 'InjectorManagerService', '$modal',
         function ($scope, $location, InjectorManagerService, $modal) {
			$scope.injector = new InjectorManagerService();
			
        	$scope.injector.uid = '';
        	$scope.injector.toInject = '';

        	$scope.serviceReportedMessages = false;

        	$scope.messages = [];
        	$scope.submitInProgress = false;

			$scope.cancelInjector = function() {
				$location.path("/");
			};
			
			$scope.searchUid = function() {

				var modalInstance = $modal.open({
				      animation: true,
				      templateUrl: 'partials/uidSearch.html',
				      controller: 'UIDSearchCtrl',
				      size: 'lg'
				});
				
				 modalInstance.result.then(
						 function (selectedItem) {
				      		$scope.injector.uid = selectedItem;
				    	}, function () {//Modal cancelled
				    	});
			}
			
   	        $scope.saveInjector = function (injection) {

   	        	$scope.submitInProgress = true;

   	        	InjectorManagerService.save(injection,
	        			function(apiResponse) {
	        				$location.path('/');},
	        			function(apiResponse) {
	        				$scope.serviceReportedMessages = true;
	        				$scope.messages = apiResponse.data.messages;
	        				alert('An error occurred saving the injection. Please correct the errors and resubmit.');
	        				$scope.submitInProgress = false;}
		        	);
	        	}
        }]);

injectorManagerControllers.controller('UIDSearchCtrl', ['$scope', '$location', '$timeout', 'UIDSearchService', '$modalInstance',
     	     function ($scope, $location, $timeout, UIDSearchService, $modalInstance) {

				$scope.showRmiError = false;
     	   		$scope.uidResults = null;
     			
     	   		$scope.criteria = new UIDSearchService();
     	   		
            	$scope.criteria.startDate = '';
            	$scope.criteria.endDate = '';
            	
            	$scope.uidSelection = {};

     			$scope.searchUIDs = function(startDateValue, endDateValue) {
     		   		UIDSearchService.query({startDate:startDateValue, endDate:endDateValue},
     						function(apiResponse) {$scope.uidResults = apiResponse.data;}, 
     						function() {$scope.showRmiError = true;});
     			};

     			$scope.selectUID = function() {
     				if (typeof $scope.uidSelection.uid === 'undefined') {
     					alert("Please select a UID in the search results. To search for UIDs, provide a filter and click the Search button.");
     				} else {
     					$modalInstance.close($scope.uidSelection.uid);
     				}
     			}

     			$scope.cancel = function () {
     				$modalInstance.dismiss('cancel');
     			};			
     		}]);
