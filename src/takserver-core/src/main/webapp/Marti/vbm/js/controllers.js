
app.controller('vbmController', 
    function($scope, $http) {

		$scope.get_current_config = function(){
	        $http({
	            method : "GET",
	            url : "/vbm/api/config"
	          }).then(function mySuccess(response) {

	            console.log("current_config: "+ response.data);

	            var current_config = angular.fromJson(response.data);

	            $scope.vbm_enabled = current_config.vbmEnabled;
	            $scope.sa_disabled = current_config.sadisabled;
	            $scope.chat_disabled = current_config.chatDisabled;
	            $scope.ism_strict_enforcing = current_config.ismStrictEnforcing;
				$scope.ism_url = current_config.ismUrl;
	            $scope.network_classification = current_config.networkClassification;

	          }, function myError(response) {
	            alert("Error fetching data from server");
	            console.error("response status: "+response.status);
	            console.error("response text: "+response.statusText);
	          });
		}
	    
	    $scope.get_current_config();

	    $scope.save_changes = function() {

	    	data = {
	            vbmEnabled: $scope.vbm_enabled,
	            sadisabled: $scope.sa_disabled,
	            chatDisabled: $scope.chat_disabled,
				ismStrictEnforcing: $scope.ism_strict_enforcing,
				ismUrl: $scope.ism_url,
				networkClassification: $scope.network_classification
       		};	

			$http({
	            method : "POST",
	            url : "/vbm/api/config",
	            data: JSON.stringify(data)
	          }).then(function mySuccess(response) {

	                if (response.status == 200){
		                alert("Successfully changed VBM Configuration");
		            }else{
		                alert("response.status: "+ response.status);
		            }

	          }, function myError(response) {
	                if (response.data != null){
		                alert("Error changing VBM Configuration. " + response.data.message);
		            } else {
		                alert("Error changing VBM Configuration.");
		            }

		            console.error("response status: "+response.status);
		            console.error("response text: "+response.statusText);
	          });

		};

    }
);
