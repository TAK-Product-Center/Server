/*******************************************************************************
 * DISTRIBUTION C. Distribution authorized to U.S. Government agencies and their contractors. Other requests for this document shall be referred to the United States Air Force Research Laboratory.
 *
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
"use strict";

angular.module('roger_federation.Workflows')
    .controller('AddFederationOutgoingController', ['$rootScope', '$scope', '$http', '$stateParams', '$modalInstance', '$timeout', '$log', '$cookieStore', 'growl', 'WorkflowTemplate', 'WorkflowService', 'OntologyService', 'JointPaper', addFederationOutgoingController]);

function addFederationOutgoingController($rootScope, $scope, $http, $stateParams, $modalInstance, $timeout, $log, $cookieStore, growl, WorkflowTemplate, WorkflowService, OntologyService, JointPaper) {
    $scope.editorTitle = "Add";
    $scope.outgoingNameExists = false;
    $scope.editExisting = false;
    $scope.roger_federation = undefined;
    var cellView;

    $scope.initialize = function() {
        cellView = JointPaper.inpector.options.cellView;
        $scope.roger_federation = JSON.parse(JSON.stringify(cellView.model.attributes.roger_federation)); //Clone
        if ($scope.roger_federation.name !== "") {
            $scope.editorTitle = "Modify";
            $scope.editExisting = true;
        }
    };

    $scope.submit = function() {
        let existingName = false; 

        let cells = JointPaper.graph.toJSON().cells;
        cells.forEach(cell => {
            if (cell.graphType === 'FederationOutgoingCell' && cell.id !== cellView.model.attributes.id) {
                if (cell.roger_federation.stringId === $scope.roger_federation.stringId)
                    existingName = true
            }
        })

        if (existingName) {
            $scope.outgoingNameExists = true;
        } else {
            $scope.outgoingNameExists = false;
            if (!$scope.roger_federation.outgoing_uuid)
                $scope.roger_federation.outgoing_uuid = create_UUID()

            $scope.roger_federation.name = $scope.roger_federation.stringId + '_' + $scope.roger_federation.outgoing_uuid
            $scope.roger_federation.outgoingName = $scope.roger_federation.name 
            if (JointPaper.inpector !== undefined) {
                //Construct Label
                let text = $scope.roger_federation.stringId +'\n\n' 
                    + $scope.roger_federation.host + ':' + $scope.roger_federation.port

                var shapeLabel = joint.util.breakText(text, {
                    width: JointPaper.options.maxLabelWidth
                });

                cellView.model.set('content', shapeLabel);
                
                cellView.model.attributes.attrs['.body']['opacity'] = '0.35'

                cellView.model.attributes.roger_federation = JSON.parse(JSON.stringify($scope.roger_federation));

                cellView.resize(); //Sometimes the label wraps when it shouldn't.  This seems to fix it.
            }
            $modalInstance.close('ok');
        }
    };

    $scope.cancel = function() {
        if (!$scope.editExisting && JointPaper.inpector !== undefined) {
            JointPaper.inpector.options.cellView.model.remove();
        }
        $modalInstance.dismiss('cancel');
    };

    function create_UUID(){
        var dt = new Date().getTime();
        var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = (dt + Math.random()*16)%16 | 0;
            dt = Math.floor(dt/16);
            return (c=='x' ? r :(r&0x3|0x8)).toString(16);
        });
        return uuid;
    }
}