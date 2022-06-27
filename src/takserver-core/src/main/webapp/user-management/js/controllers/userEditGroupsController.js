app.controller('userEditGroupController', function($scope, $http) {

    console.log("Switched to user edit group controller");

    $scope.saveGroupsForUser = function() {

        data = {
            username: $scope.list_groups_for_user.username,
            groupListIN: $scope.list_groups_for_user.groupListIN,
            groupListOUT: $scope.list_groups_for_user.groupListOUT,
            groupList: $scope.list_groups_for_user.groupList
        }

        $http({
            method : "PUT",
            url : "api/update-groups",
            data: JSON.stringify(data)
        }).then(function mySuccess(response) {
            if (response.status == 200){
                alert("Successfully updated groups for user " + $scope.list_groups_for_user.username);
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