
app.controller('fileController', 
    function($scope, $http) {

		$scope.init_slider_value = function(value){
			var slider = document.getElementById("sliderId");
			var output = document.getElementById("currentValue");
			slider.value = value;
			output.innerHTML = value;
		}


		$scope.get_current_config = function(){
	        $http({
	            method : "GET",
	            url : "/files/api/config"
	          }).then(function mySuccess(response) {

				$scope.uploadSizeLimit = response.data.uploadSizeLimit;
				$scope.init_slider_value(response.data.uploadSizeLimit);
				
	          }, function myError(response) {
	            alert("Error fetching data from server");
	            console.error("response status: "+response.status);
	            console.error("response text: "+response.statusText);
	          });
		}
		
  		$scope.get_current_config();

	    $scope.save_changes = function() {

	    	data = {
	            uploadSizeLimit: $scope.uploadSizeLimit,
       		};	

			$http({
	            method : "POST",
	            url : "/files/api/config",
	            data: JSON.stringify(data)
	          }).then(function mySuccess(response) {

	                if (response.status == 200){
		                alert("Successfully changed File Configuration");
		            }else{
		                alert("response.status: "+ response.status);
		            }

	          }, function myError(response) {
	                if (response.data != null){
		                alert("Error changing File Configuration. " + response.data.message);
		            } else {
		                alert("Error changing File Configuration.");
		            }

		            console.error("response status: "+response.status);
		            console.error("response text: "+response.statusText);
	          });

		};
		

		
		var slider = document.getElementById("sliderId");
		var output = document.getElementById("currentValue");
		

		slider.value = $scope.uploadSizeLimit;
		output.innerHTML = slider.value;

		slider.oninput = function() {
			output.innerHTML = this.value;
		}

    }
);
