app.controller('newCaveatController', function($scope, $http) {

    reset_form = function(){
        // Reset the form model.
        $scope.new_caveat_name = "";
        // Set back to pristine.
        //$scope.form.$setPristine();
        // Since Angular 1.3, set back to untouched state.
        //$scope.form.$setUntouched();
    }

    $scope.new_caveat = function(){

        if ($scope.new_caveat_name.length < 3 || $scope.new_caveat_name.match("^[a-zA-Z0-9_\.\\-]+$") == null){

            alert("Caveat name must be at least 3 characters and must only contain letters, number, hyphen, underscore and dot");
            return;
        }


        for (const element of $scope.caveats) {
            if (element.name == $scope.new_caveat_name){
                alert("Caveat " + $scope.new_caveat_name + " already exists!");
                return;
            }
        }

        $http({
            method : "POST",
            url : "/Marti/api/caveat/" + encodeURI($scope.new_caveat_name)
        }).then(function mySuccess(response) {

            alert("Successfully created new caveat "+ $scope.new_caveat_name);
            reset_form();
            $scope.list_caveats();

        }, function myError(response) {
            if (response.data != null){
                alert("Error creating caveat. " + response.data.message);
            } else {
                alert("Error creating caveat.");
            }
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });

        $scope.closeThisDialog(1);

    }

});