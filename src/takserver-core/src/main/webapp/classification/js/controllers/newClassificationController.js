app.controller('newClassificationController', function($scope, $http) {

    reset_form = function(){
        // Reset the form model.
        $scope.new_classification_level = "";
        // Set back to pristine.
        //$scope.form.$setPristine();
        // Since Angular 1.3, set back to untouched state.
        //$scope.form.$setUntouched();
    }

    $scope.new_classification = function(){

        if ($scope.new_classification_level.length < 3 || $scope.new_classification_level.match("^[a-zA-Z0-9_\.\\-]+$") == null){

            alert("Classification level must be at least 3 characters and must only contain letters, number, hyphen, underscore and dot");
            return;
        }


        for (const element of $scope.classifications) {
            if (element.name == $scope.new_classification_level){
                alert("Classification level " + $scope.new_classification_level + " already exists!");
                return;
            }
        }

        $http({
            method : "POST",
            url : "/Marti/api/classification/" + encodeURI($scope.new_classification_level)
        }).then(function mySuccess(response) {

            alert("Successfully created new classification level: "+ $scope.new_classification_level);
            reset_form();
            $scope.list_classifications();

        }, function myError(response) {
            if (response.data != null){
                alert("Error creating new classification. " + response.data.message);
            } else {
                alert("Error creating new classification.");
            }
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });

        $scope.closeThisDialog(1);

    }

});