app.controller('setCaveatsController', function($scope, $http) {

    $scope.setCaveatsForClassification = function() {

        if ($scope.current_classification_level_to_set_caveats == undefined){
            alert("Please select Classification to edit from the left panel");
            return;
        }

        data = {
            level: $scope.current_classification_level_to_set_caveats,
            caveats: $scope.selected_caveats_for_current_classification
        }

        $http({
            method : "PUT",
            url : "/Marti/api/classification",
            data: JSON.stringify(data)
        }).then(function mySuccess(response) {
            if (response.status == 200){
                alert("Successfully set caveats for classification level " + data.level);
                $scope.reset_middle_panel();
            }else{
                alert("response.status: "+ response.status);
            }

        }, function myError(response) {
            alert("Error setting caveats for classification level " + data.level);
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });      
        
    }

});