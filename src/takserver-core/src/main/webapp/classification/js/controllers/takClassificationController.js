app.controller('takClassificationController', function($scope, $http, ngDialog) {

    $scope.list_classifications = function(){
        $http({
            method : "GET",
            url : "/Marti/api/classification"
          }).then(function mySuccess(response) {
            response_data = angular.fromJson(response.data);
            // response_data = {
            //     "data": [{
            //         "level": "CLASSIFIED",
            //         "caveats":[{"name":"CUI"}, {"name":"ABC"}]
            //     },
            //     {
            //         "level": "UNCLASSIFIED",
            //         "caveats":[]
            //     }]
            // };
            $scope.classifications = response_data.data;
            console.log("$scope.classifications: "+ JSON.stringify($scope.classifications));
          }, function myError(response) {
            alert("Error fetching classification data from server");
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
          });
    }

    $scope.list_caveats = function(){
        $http({
            method : "GET",
            url : "/Marti/api/caveat"
        }).then(function mySuccess(response) {
            response_data = angular.fromJson(response.data);
            // response_data = {
            //     "data": [{
            //         "name": "CUI"
            //     },
            //     {
            //         "name": "ABC"
            //     }]
            // };
            $scope.caveats = response_data.data;
            console.log("$scope.caveats: "+JSON.stringify($scope.caveats));
        }, function myError(response) {
            alert("Error fetching list of caveats from server");
            console.error("response status: "+response.status);
            console.error("response text: "+response.statusText);
        });
    }

    $scope.list_classifications();
    $scope.list_caveats();

    $scope.refresh_classifications = function(){
        $scope.list_classifications();
    }

    $scope.refresh_caveats = function(){
        $scope.list_caveats();
    }

    $scope.reset_middle_panel = function(){
        $scope.current_classification_level_to_set_caveats = undefined;
        $scope.selected_caveats_for_current_classification = [];
    }

    $scope.refresh_middle_panel = function(){
        
        if ($scope.current_classification_level_to_set_caveats == undefined || $scope.current_classification_level_to_set_caveats == ""){
            return;
        } else{
            $scope.find_caveats_for_classification($scope.current_classification_level_to_set_caveats);
        }
    }


    $scope.find_caveats_for_classification = function(level){
        
        $scope.current_classification_level_to_set_caveats = level;

        // var found = false;
        // for (let item in $scope.classifications){
        //     if ($scope.classifications[item].level == level){
        //         caveat_list = $scope.classifications[item].caveats;
        //         found = true;
        //         break;
        //     }
        // }
        // if (found) {
        //     $scope.selected_caveats_for_current_classification = caveat_list; //[{"name":"CUI"}, {"name":"ABC"}]
        // } else{ 
        //    $scope.selected_caveats_for_current_classification = [];
        //    alert("Could not find classification level " + level);
        // }  

        $http({
            method : "GET",
            url : "/Marti/api/classification/" + encodeURI(level)
          }).then(function mySuccess(response) {
            response_data = angular.fromJson(response.data);
            
            if (response_data.data == undefined){ // No such classification exists
                $scope.reset_middle_panel();
            }else{
                $scope.selected_caveats_for_current_classification = response_data.data.caveats; //[{"name":"CUI"}, {"name":"ABC"}]
            }

          }, function myError(response) {
             $scope.reset_middle_panel();
             alert("Error fetching caveats from server");
          });

    }

    $scope.delete_classification = function(level) {

        var confirmDialog = ngDialog.openConfirm({
            template:'\
                <p>Are you sure you want to delete classification '+level+' ?</p>\
                <div class="ngdialog-buttons">\
                    <button type="button" class="ngdialog-button ngdialog-button-secondary" ng-click="closeThisDialog()">No</button>\
                    <button type="button" class="ngdialog-button ngdialog-button-primary" ng-click="confirm(1)">Yes</button>\
                </div>',
            plain: true
        }).then(function (confirm) {
            $http({
                method : "DELETE",
                url : "/Marti/api/classification/" + encodeURI(level)
            }).then(function mySuccess(response) {
                if (response.status == 200){
                    alert("Successfully deleted classification " + level);
                }else{
                    alert("response.status: "+ response.status);
                }
                console.log("response.data: "+ response.data);
                $scope.list_classifications();
                $scope.refresh_middle_panel();

            }, function myError(response) {
                alert("Error deleting classification");
                console.error("response status: "+response.status);
                console.error("response text: "+response.statusText);
            });
          }, function(reject) {
            
          });

    };

    $scope.delete_caveat = function(caveat_name){

        var confirmDialog = ngDialog.openConfirm({
            template:'\
                <p>Are you sure you want to delete caveat '+caveat_name+' ?</p>\
                <div class="ngdialog-buttons">\
                    <button type="button" class="ngdialog-button ngdialog-button-secondary" ng-click="closeThisDialog()">No</button>\
                    <button type="button" class="ngdialog-button ngdialog-button-primary" ng-click="confirm(1)">Yes</button>\
                </div>',
            plain: true
        }).then(function (confirm) {

            $http({
                method : "DELETE",
                url : "/Marti/api/caveat/" + encodeURI(caveat_name)
            }).then(function mySuccess(response) {
                if (response.status == 200){
                    alert("Successfully deleted caveat " + caveat_name);
                }else{
                    alert("response.status: "+ response.status);
                }
                console.log("response.data: "+ response.data);
                $scope.list_caveats();
                $scope.refresh_middle_panel();

            }, function myError(response) {
                alert("Error deleting caveat");
                console.error("response status: "+response.status);
                console.error("response text: "+response.statusText);
            });
          }, function(reject) {
            
          });

    }

    $scope.onDropCompleteToSelectedCaveats = function(data, evt) {
        console.log("onDropCompleteToSelectedCaveats , data:", data);

        if ($scope.selected_caveats_for_current_classification == null){
            $scope.selected_caveats_for_current_classification = [];
        }

        // add to this drop box
        if (!$scope.selected_caveats_for_current_classification.includes(data.caveat)){
            $scope.selected_caveats_for_current_classification.push(data.caveat);
        }

    }

    $scope.onDropCompleteToCaveatRepo = function(data, evt) {
        console.log("onDropCompleteToCaveatRepo , data:", data);

        if (data == null){
            return;
        }
        // remove from previous drop box if necessary
        if (data.from == 'selectedCaveats'){
            index = $scope.selected_caveats_for_current_classification.indexOf(data.caveat);
            if (index != -1){
                $scope.selected_caveats_for_current_classification.splice(index, 1);
            }
        }
    }
      
    $scope.onDragComplete = function(data, evt) {
        //console.log("onDropComplete, data:", data);
    }

    $scope.open_new_caveat_dialog = function () {
        ngDialog.open({
            template: 'new_caveat.dialog.html', 
            className: 'ngdialog-theme-default',
            controller: 'newCaveatController',
            scope: $scope
        });

    };

    $scope.open_new_classification_dialog = function () {
        ngDialog.open({
            template: 'new_classification.dialog.html', 
            className: 'ngdialog-theme-default',
            controller: 'newClassificationController',
            scope: $scope
        });

    };

    $scope.showSimpleMessage = function(message){
        ngDialog.open({
            template: '<p>'+message+'</p>',
            plain: true
        });
    }

});