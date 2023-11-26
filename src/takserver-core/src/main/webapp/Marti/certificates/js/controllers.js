'use strict';

var certificateManagerControllers = angular.module('certificateManagerControllers', []);

certificateManagerControllers.controller('FederateCertificatesListCtrl', ['$scope', '$location', 'FederateCertificatesService', 'FederateCAHopsService',
      function ($scope, $location, FederateCertificatesService, FederateCAHopsService) {
	
    		$scope.showRmiError = false;

    		$scope.getFederateCertificates = function() {
        		FederateCertificatesService.query(
    					function(apiResponse) {$scope.federateCertificates = apiResponse.data;}, 
    					function() {$scope.showRmiError = true;});    			
    		}
			
			$scope.deleteCertificate = function(fingerPrint, serialNumber) {
				if (confirm('Are you sure you want to delete the certificate (Serial Number = ' + serialNumber + ')')) {
					var fcs = new FederateCertificatesService();
					
					fcs.$delete({id:fingerPrint},
							function(apiResponse) {$scope.getFederateCertificates();},
							function() {alert('An unexpected error occurred deleting the certificate.');});
				}
			}

            $scope.saveHops = function(fc){
                if (fc.maxHops === 0) 
                    return

                var federateCAHopsAssociation = {};
                federateCAHopsAssociation["caId"] = fc.fingerPrint;
                federateCAHopsAssociation["maxHops"] = fc.maxHops;
                FederateCAHopsService.save(federateCAHopsAssociation);
            }


            $scope.changeHops = function(fc) {

            }

			$scope.getFederateCertificates();

        }]);

certificateManagerControllers.controller('FederateCertificateUploadCtrl', ['$scope', '$location', '$http', function($scope, $location, $http) {

	$scope.messages = [];
	$scope.submitInProgress = false;

    $scope.uploadFile = function(){

    	var file = $scope.certificateFile;
        
        if (file == null) {
        	alert("Please provide a valid certificate file before submitting.");
        } else {
	        var uploadUrl = "/Marti/api/federatecertificates";
	        
			$scope.submitInProgress = true;
			
	        var fd = new FormData();
	        fd.append('file', file);
	        $http.post(uploadUrl, fd, {
	            transformRequest: angular.identity,
	            headers: {'Content-Type': undefined}
	        })
	        .then(function(){
	        	$location.path("/");
	        },
	        function(apiResponse){
				$scope.submitInProgress = false;
	        	alert('An error occurred uploading your file. Ensure your file is a CA certificate less than 1 MB in size and resubmit.');
				$scope.messages = apiResponse.messages;
	        	$scope.serviceReportedMessages = true;
	        });
        }
    };
    
    $scope.cancel = function() {
    	$location.path("/");
    }

}]);

certificateManagerControllers.controller('FederateCAGroupsCtrl', ['$scope', '$location', 'FederateCAGroupsService', '$routeParams',

    function ($scope, $location, FederateCAGroupsService, $routeParams){
        $scope.direction = "BOTH";
        $scope.caId = $routeParams.id;

        $scope.federateCAGroups = [];
        $scope.submitInProgress = false;

        $scope.getFederateCAGroups = function(){
            $scope.federateCAGroups = FederateCAGroupsService.query({caId:$scope.caId},
            function(apiResponse) {$scope.federateCAGroups = apiResponse.data;},
            function(apiResponse) {alert('An unexpected error occurred retrieving the list of federate CA groups');});
        }

        $scope.backToCAs = function(){
            $location.path("/");
        };

        $scope.addGroup = function(group, direction){
            var objectExists = false;

            if(group == null || group.trim() == '' || direction == null || direction.trim() == ''){
                alert('Please select a group and a direction above and retry your request')
            }
            else{
                for(var i = 0; i < $scope.federateCAGroups.length; i++){
                    if($scope.federateCAGroups[i].group === group &&
                        ($scope.federateCAGroups[i].direction === direction || direction == 'BOTH')){
                        objectExists = true;
                        break;
                    }
                }

                if(objectExists){
                    alert("You've already added this group/direction combination to this CA")
                }
                else{
                    var federateCAGroupAssociation = {};
                    federateCAGroupAssociation["caId"] = $scope.caId;
                    federateCAGroupAssociation["group"] = group;
                    federateCAGroupAssociation["direction"] = direction;
                    FederateCAGroupsService.save(federateCAGroupAssociation,
                        function(apiResponse){
                            $scope.getFederateCAGroups();},
                        function(apiResponse) {
                            $scope.serviceReportedMessages = true;
                            $scope.messages = apiResponse.data.messages;
                            alert('An error occurred adding the group. Please correct the errors and resubmit.');
                            $scope.submitInProgress = false;}
                        );
                }
            }
        }

        $scope.deleteObject = function(group, direction){
            var federateCAGroupService = new FederateCAGroupsService();

            federateCAGroupService.$delete({caId:$scope.caId, group:group, direction:direction},
                function(apiResponse) {$scope.getFederateCAGroups();},
                function(apiResponse) {alert("An unexpected error occurred deleting the requested CA group");});
        }

        $scope.getFederateCAGroups();

    }]);



