app.controller('groupEditUsersController', function($scope, $http) {

    console.log("Switched to group edit users controller");

    $scope.saveUsersForGroup = function() {
        
        data = {
            groupname: $scope.list_users_in_group.groupname,
            usersInGroupList: $scope.list_users_in_group.usersInGroupList,
            usersInGroupListIN: $scope.list_users_in_group.usersInGroupListIN,
            usersInGroupListOUT: $scope.list_users_in_group.usersInGroupListOUT
        }

        $http({
            method : "PUT",
            url : "api/update-group-users",
            data: JSON.stringify(data)
        }).then(function mySuccess(response) {
            if (response.status == 200){
                alert("Successfully updated users for group " + $scope.list_users_in_group.groupname);
            }else{
                alert("response.status: "+ response.status);
            }

        }, function myError(response) {
            alert("Error fetching data from server");
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });      
        
    }

});