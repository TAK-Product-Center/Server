app.controller('newGroupController', function($scope) {

    reset_form = function(){
        // Reset the form model.
        $scope.new_groupname = "";
        // Set back to pristine.
        $scope.form.$setPristine();
        // Since Angular 1.3, set back to untouched state.
        $scope.form.$setUntouched();
    }

    $scope.add_group_label = function(){

        if ($scope.new_groupname.length < 4 || $scope.new_groupname.match("^[a-zA-Z0-9_\.\\-]+$") == null){

            alert("Group name must be at least 4 characters and must only contain letters, number, hyphen, underscore and dot");
            return;
        }


        for (const element of $scope.groupnames) {
            if (element.groupname == $scope.new_groupname){
                alert("Group " + $scope.new_groupname + " already exists!");
                return;
            }
        }

        $scope.groupnames.push(
            {
                "groupname": $scope.new_groupname
            }    
        );
        alert("Added a group label " + $scope.new_groupname);
        reset_form();
        $scope.closeThisDialog(1);

    }

});