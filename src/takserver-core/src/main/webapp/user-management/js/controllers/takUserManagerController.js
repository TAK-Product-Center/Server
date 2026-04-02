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
    
    $scope.reset_draggables = function(){
        $scope.isEditingGroupUsers = false
        $scope.isEditingUserGroups = false
    }

    $scope.list_users();
    $scope.list_groups();
    $scope.reset_draggables();

    $scope.refresh_users = function(){
        $scope.list_users();
    }

    $scope.refresh_groups = function(){
        $scope.list_groups();
    }  

    $scope.user_edit_groups = function(username){
        $scope.current_user_to_edit_groups = username;

        $scope.isEditingGroupUsers = false
        $scope.isEditingUserGroups = true
        
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

    $scope.group_edit_users = function(groupname){

      $scope.current_group_to_edit_users = groupname;

      $scope.isEditingUserGroups = false
      $scope.isEditingGroupUsers = true

      $scope.list_users_in_group = {
          "groupname": "", 
          "usersInGroupListIN": [],
          "usersInGroupListOUT": [],
          "usersInGroupList": []
      }

      $scope.getAssignedUsersForGroup = function(){
  
        $http({
            method : "GET",
            url : "api/users-in-group/" + encodeURI($scope.current_group_to_edit_users)
        }).then(function mySuccess(response) {
            $scope.list_users_in_group = angular.fromJson(response.data);
            console.log("$scope.list_users_in_group: "+ JSON.stringify($scope.list_users_in_group));
        }, function myError(response) {
            alert("Error fetching data from server");
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });
      }

      $scope.getAssignedUsersForGroup();

    }

    $scope.isUserListDraggable = function(){
      return $scope.isEditingGroupUsers;
    }
    
    $scope.isGroupListDraggable = function(){
      return $scope.isEditingUserGroups;
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

        // remove from previous drop box if necessary.
        // if the group was dragged in from the GroupListRepo,
        // remove from other columns if necessary.
        if (data.from == 'GroupListRepo' || data.from == 'groupListOUT'){
            index = $scope.list_groups_for_user.groupListOUT.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupListOUT.splice(index, 1);
            }
        }
         
        if (data.from == 'GroupListRepo' || data.from == 'groupList'){
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

        // remove from previous drop box if necessary.
        // if the group was dragged in from the GroupListRepo,
        // remove from other columns if necessary.
        if (data.from == 'GroupListRepo' || data.from == 'groupListIN'){
            index = $scope.list_groups_for_user.groupListIN.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupListIN.splice(index, 1);
            }
        }
        
        if (data.from == 'GroupListRepo' || data.from == 'groupList'){
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

        // remove from previous drop box if necessary.
        // if the group was dragged in from the GroupListRepo,
        // remove from other columns if necessary.
        if (data.from == 'GroupListRepo' || data.from == 'groupListIN'){
            index = $scope.list_groups_for_user.groupListIN.indexOf(data.groupname);
            if (index != -1){
                $scope.list_groups_for_user.groupListIN.splice(index, 1);
            }
        }
        
        if (data.from == 'GroupListRepo' || data.from == 'groupListOUT'){
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

    $scope.onDropCompleteUserToGroupIN = function(data, evt) {
        // data = {'username': x, 'from': 'UserListRepo'}
        console.log("onDropCompleteUserToGroupIN, data:", data);
        if ($scope.list_users_in_group == null){
            $scope.list_users_in_group = {
                "usersInGroupListIN": [],
                "usersInGroupListOUT": [],
                "usersInGroupList": []
            }
        }
        
        // add to this drop box
        if (!$scope.list_users_in_group.usersInGroupListIN.includes(data.username)){
            $scope.list_users_in_group.usersInGroupListIN.push(data.username);
        }

        // remove from previous drop box if necessary.
        // if the group was dragged in from the GroupListRepo,
        // remove from other columns if necessary.
        if (data.from == 'UserListRepo' || data.from == 'usersInGroupListOUT'){
            index = $scope.list_users_in_group.usersInGroupListOUT.indexOf(data.username);
            if (index != -1){
                $scope.list_users_in_group.usersInGroupListOUT.splice(index, 1);
            }
        }
        
        if (data.from == 'UserListRepo' || data.from == 'usersInGroupList'){
            index = $scope.list_users_in_group.usersInGroupList.indexOf(data.username);
            if (index != -1){
                $scope.list_users_in_group.usersInGroupList.splice(index, 1);
            }
        }
    }

    $scope.onDropCompleteUserToGroupOUT = function(data, evt) {
        // data = {'username': x, 'from': 'UserListRepo'}

        console.log("onDropCompleteUserToGroupOUT , data:", data);
        if ($scope.list_users_in_group == null){
            $scope.list_users_in_group = {
                "usersInGroupListIN": [],
                "usersInGroupListOUT": [],
                "usersInGroupList": []
            }
        }

        // add to this drop box
        if (!$scope.list_users_in_group.usersInGroupListOUT.includes(data.username)){
            $scope.list_users_in_group.usersInGroupListOUT.push(data.username);
        }

        // remove from previous drop box if necessary.
        // if the group was dragged in from the GroupListRepo,
        // remove from other columns if necessary.
        if (data.from == 'UserListRepo' || data.from == 'usersInGroupListIN'){
            index = $scope.list_users_in_group.usersInGroupListIN.indexOf(data.username);
            if (index != -1){
                $scope.list_users_in_group.usersInGroupListIN.splice(index, 1);
            }
        }
        
        if (data.from == 'UserListRepo' || data.from == 'usersInGroupList'){
            index = $scope.list_users_in_group.usersInGroupList.indexOf(data.username);
            if (index != -1){
                $scope.list_users_in_group.usersInGroupList.splice(index, 1);
            }
        }
    }

    $scope.onDropCompleteUserToGroup = function(data, evt) {
        console.log("onDropCompleteUserToGroup , data:", data);

        if ($scope.list_users_in_group == null){
            $scope.list_users_in_group = {
                "usersInGroupListIN": [],
                "usersInGroupListOUT": [],
                "usersInGroupList": []
            }
        }

        // add to this drop box
        if (!$scope.list_users_in_group.usersInGroupList.includes(data.username)){
            $scope.list_users_in_group.usersInGroupList.push(data.username);
        }

        // remove from previous drop box if necessary.
        // if the group was dragged in from the GroupListRepo,
        // remove from other columns if necessary.
        if (data.from == 'UserListRepo' || data.from == 'usersInGroupListIN'){
            index = $scope.list_users_in_group.usersInGroupListIN.indexOf(data.username);
            if (index != -1){
                $scope.list_users_in_group.usersInGroupListIN.splice(index, 1);
            }
        }
        
        if (data.from == 'UserListRepo' || data.from == 'usersInGroupListOUT'){
            index = $scope.list_users_in_group.usersInGroupListOUT.indexOf(data.username);
            if (index != -1){
                $scope.list_users_in_group.usersInGroupListOUT.splice(index, 1);
            }
        }

    }

    $scope.onDropCompleteToUserListRepo = function(data, evt) {
        console.log("onDropCompleteToUserListRepo , data:", data);

        if (data == null){
            return;
        }
        // remove from previous drop box if necessary
        if (data.from == 'usersInGroupListIN'){
            index = $scope.list_users_in_group.usersInGroupListIN.indexOf(data.username);
            if (index != -1){
                $scope.list_users_in_group.usersInGroupListIN.splice(index, 1);
            }
        }else if (data.from == 'usersInGroupListOUT'){
            index = $scope.list_users_in_group.usersInGroupListOUT.indexOf(data.username);
            if (index != -1){
                $scope.list_users_in_group.usersInGroupListOUT.splice(index, 1);
            }
        }else if (data.from == 'usersInGroupList'){
            index = $scope.list_users_in_group.usersInGroupList.indexOf(data.username);
            if (index != -1){
                $scope.list_users_in_group.usersInGroupList.splice(index, 1);
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
    
        $scope.isEditingGroupUsers = false
        $scope.isEditingUserGroups = true
        
        $scope.new_user = {};
        $scope.list_groups_for_user = {
            "groupListIN": [],
            "groupListOUT": [],
            "groupList": []
        }
    }

    $scope.init_new_users = function (){
    
        $scope.isEditingGroupUsers = false
        $scope.isEditingUserGroups = true
        
        $scope.new_users = {};
        $scope.list_groups_for_user = {
            "groupListIN": [],
            "groupListOUT": [],
            "groupList": []
        }
    }

});