app.controller('userChangePasswordController', function($scope, $http) {

    $scope.reset_form = function(){
        // Reset the form model.
        $scope.user_to_change_password = {
            username: '',
            new_password: ''
        }
        // Set back to pristine.
        $scope.form_change_password.$setPristine();
        // Since Angular 1.3, set back to untouched state.
        $scope.form_change_password.$setUntouched();
    }

    $scope.request_password_change = function(){

        data = {
            username: $scope.user_to_change_password.username,
            password: $scope.user_to_change_password.new_password
        };
        
        $http({
            method : "PUT",
            url : "api/change-user-password",
            data: JSON.stringify(data)
        }).then(function mySuccess(response) {
            if (response.status == 200){
                alert("Successfully changed password for user " + $scope.user_to_change_password.username);
                $scope.reset_form();
            }else{
                alert("response.status: "+ response.status);
            }
            console.log("response.data: "+ response.data);

        }, function myError(response) {

            if (response.data != null){
                alert("Error changing password. " + response.data.message);
            } else {
                alert("Error changing password.");
            }

            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });
    
    }

});