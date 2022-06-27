
"use strict";

angular.module('roger_federation.Workflows')
    .controller('AddBpmnFederatePolicyController', ['$rootScope', '$state', '$scope', '$stateParams', '$modalInstance', '$log', '$cookieStore', 'growl', 'WorkflowTemplate', 'WorkflowService', 'OntologyService', 'JointPaper', addBpmnFederatePolicyController]);

function addBpmnFederatePolicyController($rootScope, $state, $scope, $stateParams, $modalInstance, $log, $cookieStore, growl, WorkflowTemplate, WorkflowService, OntologyService, JointPaper) {

    $scope.editorTitle = "Add";
    $scope.editExisting = false;
    $scope.roger_federation = undefined;
    $scope.filters = [];
    var cellView;

    $scope.initialize = function() {
        cellView = JointPaper.inpector.options.cellView;
        $scope.roger_federation = JSON.parse(JSON.stringify(cellView.model.attributes.roger_federation)); //Clone
        if ($scope.roger_federation.name !== undefined) {
            $scope.editorTitle = "Modify";
            $scope.editExisting = true;
        } else {
            var sourceName = cellView.sourceView.model.attributes.roger_federation.stringId;
            var destName = cellView.targetView.model.attributes.roger_federation.stringId;
            $scope.roger_federation = {
                name: sourceName + " -> " + destName,
                type: "Federate Policy",
                edgeFilters: []
            };
        }
        $scope.getKnownFilters();
    };

    $scope.submit = function() {
        if (JointPaper.inpector !== undefined) {
            //Construct Label
            var linkLabel = $scope.roger_federation.name;

            JointPaper.inpector.options.cellView.model.set('labels', [{
                position: 0.5,
                attrs: {
                    text: {
                        text: linkLabel
                    }
                }
            }]);
            cellView.model.attributes.roger_federation = JSON.parse(JSON.stringify($scope.roger_federation));
        }

        $modalInstance.close('ok');
    };

    $scope.newFilter = function() {
        $state.go('workflows.editor.addBPMNFederatePolicy.addPolicyFilter');
    };

    $scope.cancel = function() {
        if (!$scope.editExisting && JointPaper.inpector !== undefined) {
            JointPaper.inpector.options.cellView.model.remove();
        }
        $modalInstance.dismiss('cancel');
    };

    $scope.toggle = function (scope) {
        $scope.toggle();
    };

    $scope.remove = function (scope) {
        $scope.remove();
    };

    $scope.newTopNode = function () {
        $scope.roger_federation.edgeFilters.push({
            type: 'filter',
            filter: {},
            nodes: []
        });
    };

    // add node
    $scope.newSubItem = function (scope) {
        var nodeData = scope.$modelValue;
        nodeData.nodes.push({
            type: 'filter',
            filter: {},
            nodes: []
        });

    };

    $scope.getKnownFilters = function() {
        $scope.filters = WorkflowService.getKnownFilters();
    };

    $scope.isArgumentEditable = function(arg) {
        return !(arg.type == "roger.message.Message");
    };
}
