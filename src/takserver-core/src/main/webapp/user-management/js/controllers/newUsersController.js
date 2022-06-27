app.controller('newUsersController', function($scope, $http) {

    $scope.reset_form = function(){
        // Reset the form model.
        $scope.new_users = {};
        // reset list_groups_for_user
        $scope.list_groups_for_user = {
            "groupListIN": [],
            "groupListOUT": [],
            "groupList": []
        }
        // Set back to pristine.
        $scope.form_new_users.$setPristine();
        // Since Angular 1.3, set back to untouched state.
        $scope.form_new_users.$setUntouched();
    }

    download = function(filename, text) {
        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
        element.setAttribute('download', filename);
      
        element.style.display = 'none';
        document.body.appendChild(element);
      
        element.click();
      
        document.body.removeChild(element);
      }

    $scope.init_new_users();
      
    $scope.create_users = function(){

        data = {
            usernameExpression: $scope.new_users.username_expression,
            startN: $scope.new_users.startN,
            endN: $scope.new_users.endN,
            groupListIN: $scope.list_groups_for_user.groupListIN,
            groupListOUT: $scope.list_groups_for_user.groupListOUT,
            groupList: $scope.list_groups_for_user.groupList
        };

        $http({
            method : "POST",
            url : "api/new-users",
            data: JSON.stringify(data)
        }).then(function mySuccess(response) {
            // Start file download.
            download("tak_users.txt", JSON.stringify(response.data));

            // alert("Created new users successfully");
            $scope.reset_form();     
            $scope.list_users();

        }, function myError(response) {
            if (response.data != null){
                alert("Error creating users. " + response.data.message);
            } else {
                alert("Error creating users.");
            }
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });
    
    }

});