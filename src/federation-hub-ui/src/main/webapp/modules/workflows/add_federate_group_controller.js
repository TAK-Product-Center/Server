
"use strict";

angular.module('roger_federation.Workflows')
    .controller('AddFederateGroupController', ['$rootScope', '$scope', '$http', '$stateParams', '$modalInstance', '$timeout', '$log', '$cookieStore', 'growl', 'WorkflowTemplate', 'WorkflowService', 'OntologyService', 'JointPaper', addFederateGroupController]);

function addFederateGroupController($rootScope, $scope, $http, $stateParams, $modalInstance, $timeout, $log, $cookieStore, growl, WorkflowTemplate, WorkflowService, OntologyService, JointPaper) {

    $scope.editorTitle = "Add";
    $scope.editExisting = false;
    $scope.submitInProgress = false;
    $scope.roger_federation = undefined;
    $scope.filters = [];
    $scope.knownCas = [];
    var cellView;

    $scope.initialize = function() {
        cellView = JointPaper.inpector.options.cellView;
        $scope.roger_federation = JSON.parse(JSON.stringify(cellView.model.attributes.roger_federation)); //Clone
        if ($scope.roger_federation.name !== "") {
            $scope.editorTitle = "Modify";
            $scope.editExisting = true;
        }

        $scope.getCaGroups();
        $scope.getKnownFilters();
    };

    $scope.getCaGroups = function () {
        WorkflowService.getKnownCaGroups().then(
            function(caList) {
                $scope.knownCas = caList;
            },
            function (result) {
                console.log("Unable to load list of know CA's, " + result);
            }
        );
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


    $scope.newFilter = function() {
        $state.go('workflows.editor.addBPMNFederatePolicy.addPolicyFilter');
    };

    $scope.uploadFile = function() {

        var file = $scope.certificateFile;

        if (file == null) {
            alert("Please provide a valid certificate file before submitting.");
        } else {
            var uploadUrl = "/fig/addNewGroupCa";

            $scope.submitInProgress = true;

            var fd = new FormData();
            fd.append('file', file);
            $http.post(uploadUrl, fd, {
                transformRequest: angular.identity,
                headers: {'Content-Type': undefined}
            })
                .success(function(){
                    growl.success("Successfully uploaded CA file");
                    // Update CA list
                    $timeout(function() {$scope.getCaGroups(); }, 3000);
                    $scope.submitInProgress = false;
                })
                .error(function(apiResponse){
                    $scope.submitInProgress = false;
                    alert('An error occurred uploading your file. Ensure your file is a CA certificate less than 1 MB in size and resubmit.');
                    $scope.messages = apiResponse.messages;
                    $scope.serviceReportedMessages = true;
                });
        }
    };

    /*
        Methods for group filter expressions
     */

    $scope.newTopFilter = function () {
        $scope.roger_federation.groupFilters.push({
            type: 'filter',
            filter: {},
            nodes: []
        });
    };

    // add node
    $scope.newSubFilter = function (scope) {
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

    /*
        Methods for managed group attributes
     */

    $scope.toggle = function (scope) {
        scope.toggle();
    };

    $scope.remove = function (scope) {
        scope.remove();
    };

    $scope.newTopAttribute = function () {
        $scope.roger_federation.attributes.push({
            type: 'attribute',
            key: null,
            value: null,
            values: [],
            nodes: []
        });
    };

    // add node
    $scope.newSubAttribute = function (scope) {
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


}