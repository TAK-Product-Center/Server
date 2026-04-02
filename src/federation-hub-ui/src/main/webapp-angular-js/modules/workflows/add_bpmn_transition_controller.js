
"use strict";

angular.module('roger_federation.Workflows')
  .controller('AddBPMNTransitionController', ['$scope', '$modalInstance', '$log', 'growl', 'JointPaper', addBPMNTransitionController]);

function addBPMNTransitionController($scope, $modalInstance, $log, growl, JointPaper) {
  $scope.unitsOfTime = ["seconds", "minutes", "hours", "days"];
  $scope.editorTitle = "Flow Transition";
  $scope.transition = {
    type: "None",
    transitionTime: new Date(),
    eventDefinition: "",
    interaction: "Implicit"
  };
  var cellView;

  $scope.initialize = function() {
    if (JointPaper.inpector !== undefined) {
      cellView = JointPaper.inpector.options.cellView;
      $scope.roger_federation = JSON.parse(JSON.stringify(cellView.model.attributes.roger_federation));
      var selectedTransition = $scope.roger_federation.transition;
      if (selectedTransition !== undefined) { //Edit Transition
        $scope.editorTitle = "Modify Workflow Transition";
        $scope.transition.type = selectedTransition.type;
        $scope.transition.interaction = selectedTransition.interaction;
        if (selectedTransition.transitionTime !== undefined) {
          $scope.transition.transitionTime = new Date(selectedTransition.transitionTime);
        }
        console.log($scope.transition.transitionTime);
        if (selectedTransition.eventDefinition !== undefined) {
          $scope.transition.eventDefinition = selectedTransition.eventDefinition;
        }
      }
    }
  };

  $scope.isValid = function() {
    var transitionType = $scope.transition.type;
    if (transitionType === "Manual") {

    } else if (transitionType === "Temporal") {

    } else if (transitionType === "Event Based") {
      if ($scope.transition.eventDefinition === "") {
        growl.error("Enter an event definition to continue.");
        return false;
      }
    }
    return true;
  };

  $scope.submit = function() {
    var transitionType = $scope.transition.type;
    var linkLabel = transitionType;
    if ($scope.isValid()) {
      if (transitionType === "None") {
        $scope.transition = undefined;
        linkLabel = "";
      } else if (transitionType === "Manual") {
        linkLabel += "\n" + $scope.transition.interaction;
        delete $scope.transition.eventDefinition;
        delete $scope.transition.transitionTime;
      } else if (transitionType === "Temporal") {
        linkLabel = transitionType + "\n(" + moment($scope.transition.transitionTime).format('MM/DD/YYYY HH:mm') + ")";
        linkLabel += "\n" + $scope.transition.interaction;
        // $scope.transition.transitionTime = $scope.transition.transitionTime;
        delete $scope.transition.eventDefinition;
      } else if (transitionType === "Event Based") {
        linkLabel = transitionType + "\n(" + $scope.transition.eventDefinition + ")";
        linkLabel += "\n" + $scope.transition.interaction;
        delete $scope.transition.transitionTime;
      }

      var timer = $scope.roger_federation.timer;
      if (timer !== undefined && timer.value > 0) {
        linkLabel += "\nTimer: " + timer.value + " " + timer.units;
        if (timer.description !== undefined && timer.description !== "") {
          linkLabel += "\n" + timer.description;
        }
      }

      var priority = $scope.roger_federation.priority;
      if (priority !== undefined && priority > 0) {
        linkLabel += "\nPriority: " + priority + "";
      }

      $scope.roger_federation.transition = $scope.transition;
      cellView.model.attributes.roger_federation = JSON.parse(JSON.stringify($scope.roger_federation));
      JointPaper.inpector.options.cellView.model.set('labels', [{
        position: 0.5,
        attrs: {
          text: {
            text: linkLabel
          }
        }
      }]);

      $modalInstance.close('ok');
    }
  };

  $scope.cancel = function() {
    $modalInstance.dismiss('cancel');
  };
}
