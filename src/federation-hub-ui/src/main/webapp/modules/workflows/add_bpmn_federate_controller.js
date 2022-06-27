
"use strict";

angular.module('roger_federation.Workflows')
    .controller('AddBpmnFederateController', ['$rootScope', '$scope', '$stateParams', '$modalInstance', '$log', '$cookieStore', 'growl', 'WorkflowTemplate', 'WorkflowService', 'OntologyService', 'JointPaper', addBpmnFederateController]);

function addBpmnFederateController($rootScope, $scope, $stateParams, $modalInstance, $log, $cookieStore, growl, WorkflowTemplate, WorkflowService, OntologyService, JointPaper) {

    $scope.editorTitle = "Add";
    $scope.editExisting = false;
    $scope.knownGroups = [];
    $scope.roger_federation = undefined;
    var cellView;

    $scope.initialize = function() {
        cellView = JointPaper.inpector.options.cellView;
        $scope.roger_federation =  JSON.parse(JSON.stringify(cellView.model.attributes.roger_federation)); //Clone
        if ($scope.roger_federation.name !== "") {
            $scope.editorTitle = "Modify";
            $scope.editExisting = true;
        }
        setKnownGroups();
    };

    $scope.submit = function() {
        if (JointPaper.inpector !== undefined) {
            //Construct Label
            var shapeLabel = joint.util.breakText($scope.roger_federation.stringId, {
                width: JointPaper.options.maxLabelWidth
            });
            cellView.model.set('content', shapeLabel);
            cellView.model.attributes.roger_federation = JSON.parse(JSON.stringify($scope.roger_federation));
            cellView.resize(); //Sometimes the label wraps when it shouldn't.  This seems to fix it.
        }
        $modalInstance.close('ok');
    };

    $scope.cancel = function() {
        if (!$scope.editExisting && JointPaper.inpector !== undefined) {
            JointPaper.inpector.options.cellView.model.remove();
        }
        $modalInstance.dismiss('cancel');
    };

    $scope.generateGraphNameFromFedId = function(oldValue) {
        if (oldValue === $scope.roger_federation.stringId || $scope.roger_federation.stringId === "" || $scope.roger_federation.stringId === undefined) {
            $scope.roger_federation.stringId = $scope.roger_federation.name;
        }
    };

    $scope.toggle = function (scope) {
        scope.toggle();
    };

    $scope.remove = function (scope) {
        scope.remove();
    };

    $scope.newTopNode = function () {
        $scope.roger_federation.attributes.push({
            type: 'attribute',
            key: null,
            value: null,
            values: [],
            nodes: []
        });
    };

    // add node
    $scope.newSubItem = function (scope) {
        var nodeData = scope.$modelValue;
        nodeData.nodes.push({
            type: 'attribute',
            key: null,
            value: null,
            values: [],
            nodes: []
        });

        updateRol();
    };

    $scope.newValue = function (scope) {
        var nodeData = scope.$modelValue;
        nodeData.values.push("");

    }

    var setKnownGroups = function() {
        var cells = JointPaper.graph.attributes.cells.models;
        for (var i = 0; i < cells.length; i++) {
            if (cells[i].attributes.graphType === "GroupCell") {
                $scope.knownGroups.push(cells[i].attributes);
            }
        }
    }

    // regenerate ROL from tree and save to server
    // this could be optimized by only regenerating changed subtrees
    var updateRol = function() {
//    var rol = treeToRol($scope.data)
//    $log.debug('rol after tree change');
//    $log.debug(rol);
//
//    // Set new ROL on graph item
//    $scope.graphItem.rolExpressions[0] = rol;
//
//    var gi = $scope.graphItem;
//
//    // replace item with its id, backend expects this
//    gi.item = $scope.itemId;
//    var giJson = JSON.stringify(gi);
//    $log.debug("graph item json to save: " + giJson);
//    // save to server
//    WorkflowService.updateWorkflowGraphItem($scope.graphItemId, giJson);
//
//    $log.debug('graph item saved');
    };

    $scope.updateRol = updateRol;

    $scope.toggle = function (scope) {
        scope.toggle();
    };

    $scope.remove = function (scope) {
        scope.remove();
    };

    $scope.newTopNode = function () {
        $scope.roger_federation.attributes.push({
            type: 'attribute',
            key: null,
            value: null,
            values: [],
            nodes: []
        });
    };

    // add node
    $scope.newSubItem = function (scope) {
        var nodeData = scope.$modelValue;
        nodeData.nodes.push({
            type: 'attribute',
            key: null,
            value: null,
            values: [],
            nodes: []
        });

        updateRol();
    };

    $scope.newValue = function (scope) {
        var nodeData = scope.$modelValue;
        nodeData.values.push("");

    }

    // regenerate ROL from tree and save to server
    // this could be optimized by only regenerating changed subtrees
    var updateRol = function() {
//    var rol = treeToRol($scope.data)
//    $log.debug('rol after tree change');
//    $log.debug(rol);
//
//    // Set new ROL on graph item
//    $scope.graphItem.rolExpressions[0] = rol;
//
//    var gi = $scope.graphItem;
//
//    // replace item with its id, backend expects this
//    gi.item = $scope.itemId;
//    var giJson = JSON.stringify(gi);
//    $log.debug("graph item json to save: " + giJson);
//    // save to server
//    WorkflowService.updateWorkflowGraphItem($scope.graphItemId, giJson);
//
//    $log.debug('graph item saved');
    };

    $scope.updateRol = updateRol;

}
