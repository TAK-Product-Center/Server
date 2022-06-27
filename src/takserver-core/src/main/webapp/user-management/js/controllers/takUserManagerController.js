app.controller('takUserManagementController', function($scope, $http, ngDialog) {

    $scope.list_users = function(){
        $http({
            method : "GET",
            url : "api/list-users"
          }).then(function mySuccess(response) {
            $scope.users = angular.fromJson(response.data);
            console.log("$scope.users: "+ JSON.stringify($scope.users));
          }, function myError(response) {
            alert("Error fetching user data from server");
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
          });
    }

    $scope.list_groups = function(){
        $http({
            method : "GET",
            url : "api/list-groupnames"
        }).then(function mySuccess(response) {
            $scope.groupnames = angular.fromJson(response.data);
            console.log("$scope.groupnames: "+JSON.stringify($scope.groupnames));
        }, function myError(response) {
            alert("Error fetching user group data from server");
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });
    }

    $scope.list_users();
    $scope.list_groups();

    $scope.refresh_users = function(){
        $scope.list_users();
    }

    $scope.refresh_groups = function(){
        $scope.list_groups();
    }  

    $scope.user_edit_groups = function(username){
        $scope.current_user_to_edit_groups = username;

        $scope.list_groups_for_user = {
            "username": "", 
            "groupListIN": [],
            "groupListOUT": [],
            "groupList": []
        }

        $scope.getAssignedGroupsForUser = function(){
    
            // alert("Called getAssignedGroupsForUser() in main scope");

            $http({
                method : "GET",
                url : "api/get-groups-for-user/" + encodeURI($scope.current_user_to_edit_groups)
            }).then(function mySuccess(response) {
                
                $scope.list_groups_for_user = angular.fromJson(response.data);
                console.log("$scope.list_groups_for_user: "+ JSON.stringify($scope.list_groups_for_user));

            }, function myError(response) {
                alert("Error retrieving groups for " + $scope.current_user_to_edit_groups );
                console.error("response status: "+response.status);
                console.error("response text: "+response.statusText);
            });
        }

        $scope.getAssignedGroupsForUser();
    }

    $scope.user_change_password = function(username){

        $scope.user_to_change_password = {
            username: username,
            new_password: ''
        }
    }

    $scope.user_delete = function(username) {

        var confirmDialog = ngDialog.openConfirm({
            template:'\
                <p>Are you sure you want to delete user '+username+' ?</p>\
                <div class="ngdialog-buttons">\
                    <button type="button" class="ngdialog-button ngdialog-button-secondary" ng-click="closeThisDialog()">No</button>\
                    <button type="button" class="ngdialog-button ngdialog-button-primary" ng-click="confirm(1)">Yes</button>\
                </div>',
            plain: true
        }).then(function (confirm) {
            $http({
                method : "DELETE",
                url : "api/delete-user/" + encodeURI(username)
            }).then(function mySuccess(response) {
                if (response.status == 200){
                    alert("Successfully deleted user " + username);
                }else{
                    alert("response.status: "+ response.status);
                }
                console.log("response.data: "+ response.data);
                $scope.list_users();

            }, function myError(response) {
                alert("Error deleting user");
                console.error("response status: "+response.status);
                console.error("response text: "+response.statusText);
            });
          }, function(reject) {
            
          });

    };

    $scope.group_list_users = function(groupname){

        $http({
            method : "GET",
            url : "api/users-in-group/" + encodeURI(groupname)
        }).then(function mySuccess(response) {
            $scope.list_users_in_group = angular.fromJson(response.data);
            console.log("$scope.list_users_in_group: "+ JSON.stringify($scope.list_users_in_group));
        }, function myError(response) {
            alert("Error fetching data from server");
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });

    }

    $scope.onDropCompleteToGroupIN = function(data, evt) {
        // data = {'groupname': x.groupname, 'from': 'GroupListRepo'}
        console.log("onDropCompleteToGroupIN, data:", data);
        if ($scope.list_groups_for_user == null){
            $scope.list_groups_for_user = {
                "groupListIN": [],
                "groupListOUT": [],
                "groupList": []
            }
        }
        // $scope.list_groups_for_user: {"username":"d1","groupList":["__ANON__"],"groupListIN":[],"groupListOUT":[]}
        console.log("$scope.list_groups_for_user:", JSON.stringify($scope.list_groups_for_user));
        
        // add to this drop box
        if (!$scope.list_groups_for_user.groupListIN.includes(data.groupname)){
            $scope.list_groups_for_user.groupListIN.push(data.groupname);
        }

        // remove from previous drop box if necessary
        if (data.from == 'groupListOUT'){
            index = $scope.list_groups_for_user.groupListOUT.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupListOUT.splice(index, 1);
            }
        }else if (data.from == 'groupList'){
            index = $scope.list_groups_for_user.groupList.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupList.splice(index, 1);
            }
        }

        
    }

    $scope.onDropCompleteToGroupOUT = function(data, evt) {
        // data = {'groupname': x.groupname, 'from': 'GroupListRepo'}

        console.log("onDropCompleteToGroupOUT , data:", data);
        if ($scope.list_groups_for_user == null){
            $scope.list_groups_for_user = {
                "groupListIN": [],
                "groupListOUT": [],
                "groupList": []
            }
        }

        // add to this drop box
        if (!$scope.list_groups_for_user.groupListOUT.includes(data.groupname)){
            $scope.list_groups_for_user.groupListOUT.push(data.groupname);
        }

        // remove from previous drop box if necessary
        if (data.from == 'groupListIN'){
            index = $scope.list_groups_for_user.groupListIN.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupListIN.splice(index, 1);
            }
        }else if (data.from == 'groupList'){
            index = $scope.list_groups_for_user.groupList.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupList.splice(index, 1);
            }
        }
    }

    $scope.onDropCompleteToGroup = function(data, evt) {
        console.log("onDropCompleteToGroup , data:", data);

        if ($scope.list_groups_for_user == null){
            $scope.list_groups_for_user = {
                "groupListIN": [],
                "groupListOUT": [],
                "groupList": []
            }
        }

        // add to this drop box
        if (!$scope.list_groups_for_user.groupList.includes(data.groupname)){
            $scope.list_groups_for_user.groupList.push(data.groupname);
        }

        // remove from previous drop box if necessary
        if (data.from == 'groupListIN'){
            index = $scope.list_groups_for_user.groupListIN.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupListIN.splice(index, 1);
            }
        }else if (data.from == 'groupListOUT'){
            index = $scope.list_groups_for_user.groupListOUT.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupListOUT.splice(index, 1);
            }
        }

    }

    $scope.onDropCompleteToGroupListRepo = function(data, evt) {
        console.log("onDropCompleteToGroupListRepo , data:", data);

        if (data == null){
            return;
        }
        // remove from previous drop box if necessary
        if (data.from == 'groupListIN'){
            index = $scope.list_groups_for_user.groupListIN.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupListIN.splice(index, 1);
            }
        }else if (data.from == 'groupListOUT'){
            index = $scope.list_groups_for_user.groupListOUT.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupListOUT.splice(index, 1);
            }
        }else if (data.from == 'groupList'){
            index = $scope.list_groups_for_user.groupList.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupList.splice(index, 1);
            }
        }
    }
      
    $scope.onDragComplete = function(data, evt) {
        // console.log("onDropComplete, data:", data);
    }

    $scope.open_new_group_dialog = function () {
        ngDialog.open({
            template: 'new_group.dialog.html', 
            className: 'ngdialog-theme-default',
            controller: 'newGroupController',
            scope: $scope
        });

    };

    $scope.showSimpleMessage = function(message){
        ngDialog.open({
            template: '<p>'+message+'</p>',
            plain: true
        });
    }

    $scope.init_new_user = function (){
        $scope.new_user = {};
        $scope.list_groups_for_user = {
            "groupListIN": [],
            "groupListOUT": [],
            "groupList": []
        }
    }

    $scope.init_new_users = function (){
        $scope.new_users = {};
        $scope.list_groups_for_user = {
            "groupListIN": [],
            "groupListOUT": [],
            "groupList": []
        }
    }

});