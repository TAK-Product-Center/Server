app.controller('newUserController', function($scope, $http) {

    $scope.reset_form = function(){
        // Reset the form model.
        $scope.new_user = {};
        // Reset list_groups_for_user
        $scope.list_groups_for_user = {
            "groupListIN": [],
            "groupListOUT": [],
            "groupList": []
        }
        // Set back to pristine.
        $scope.form.$setPristine();
        // Since Angular 1.3, set back to untouched state.
        $scope.form.$setUntouched();
    }

    $scope.init_new_user();

    $scope.create_user = function(){

        data = {
            username: $scope.new_user.username,
            password: $scope.new_user.password,
            groupListIN: $scope.list_groups_for_user.groupListIN,
            groupListOUT: $scope.list_groups_for_user.groupListOUT,
            groupList: $scope.list_groups_for_user.groupList
        };

        $http({
            method : "POST",
            url : "api/new-user",
            data: JSON.stringify(data)
        }).then(function mySuccess(response) {

            alert("Created user "+ $scope.new_user.username + " successfully");
            $scope.reset_form();
            $scope.list_users();

        }, function myError(response) {
            if (response.data != null){
                alert("Error creating user. " + response.data.message);
            } else {
                alert("Error creating user.");
            }
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });
    
    }

});