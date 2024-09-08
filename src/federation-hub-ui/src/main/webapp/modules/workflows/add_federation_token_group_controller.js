/*******************************************************************************
 * DISTRIBUTION C. Distribution authorized to U.S. Government agencies and their contractors. Other requests for this document shall be referred to the United States Air Force Research Laboratory.
 *
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
"use strict";

angular.module('roger_federation.Workflows')
    .controller('AddFederationTokenGroupController', ['$rootScope', '$scope', '$http', '$stateParams', '$modalInstance', '$timeout', '$log', '$cookieStore', 'growl', 'WorkflowTemplate', 'WorkflowService', 'OntologyService', 'JointPaper', addFederationTokenGroupController]);

function addFederationTokenGroupController($rootScope, $scope, $http, $stateParams, $modalInstance, $timeout, $log, $cookieStore, growl, WorkflowTemplate, WorkflowService, OntologyService, JointPaper) {
    $scope.editorTitle = "Add";
    $scope.tokenNameExists = false;
    $scope.editExisting = false;
    $scope.roger_federation = undefined;
    var cellView;

    $scope.initialize = function() {
        cellView = JointPaper.inpector.options.cellView;
        $scope.roger_federation = JSON.parse(JSON.stringify(cellView.model.attributes.roger_federation)); //Clone
        if ($scope.roger_federation.name !== "") {
            $scope.editorTitle = "Modify";
            $scope.editExisting = true;
        } else {
            $scope.roger_federation.tokens = []
            $scope.roger_federation.expiration = -1
        }
    };

    $scope.submit = function() {
        let existingName = false; 

        let cells = JointPaper.graph.toJSON().cells;
        cells.forEach(cell => {
            if (cell.graphType === 'FederationTokenGroupCell' && cell.id !== cellView.model.attributes.id) {
                if (cell.roger_federation.stringId === $scope.roger_federation.stringId)
                    existingName = true
            }
        })

        if (existingName) {
            $scope.tokenNameExists = true;
        } else {
            $scope.tokenNameExists = false;

            $scope.roger_federation.name = $scope.roger_federation.stringId + '_token_group'
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
        }
    };

    $scope.cancel = function() {
        if (!$scope.editExisting && JointPaper.inpector !== undefined) {
            JointPaper.inpector.options.cellView.model.remove();
        }
        $modalInstance.dismiss('cancel');
    };

    $scope.showActiveConnections = function() {
        if (!$scope.editExisting && JointPaper.inpector !== undefined) {
            JointPaper.inpector.options.cellView.model.remove();
        }
        $rootScope.selectedCa = $scope.roger_federation.name
        $state.go('workflows.editor.connections');
    }

    $scope.previewToken = function(token) {
        return token.substring(0,8) + '...' + token.substring(token.length-8);;
    }

    $scope.copyToken = function(token) {
        navigator.clipboard.writeText(token)
        growl.info("Token Copied to Clipboard!");
    }

    $scope.generateToken = function() {
        if (!$scope.editExisting) {
            growl.error("Please save this node to the graph before generating tokens.");
            return
        }

        if (!$scope.roger_federation.expiration || !$scope.roger_federation.name) {
            growl.error("Cannot generate a token until a name and expiration are set!");
            return
        }

        let now = Date.now();

        let expiration = $scope.roger_federation.expiration === -1 ? -1 
                : $scope.roger_federation.expiration + now

        WorkflowService.generateJwtToken({
            clientFingerprint: now +'_' + $scope.roger_federation.stringId + '_token_fingerprint',
            clientGroup: $scope.roger_federation.name,
            expiration: expiration
        }).then(res => {
            $scope.roger_federation.tokens.push({
                token: res.token,
                expiration: expiration
            })
            growl.info("Token generated! Don't forget to save the policy to keep your changes!");
        }).catch(err => {
            growl.error("Error with token request", err);
        })
    }

    $scope.getDisplayDate = function(milliseconds) {
        return milliseconds === -1 ? -1 : new Date(milliseconds)
    }

    $scope.deleteToken = function(token) {
        $scope.roger_federation.tokens = $scope.roger_federation.tokens.filter(t => t !== token);
    };
}