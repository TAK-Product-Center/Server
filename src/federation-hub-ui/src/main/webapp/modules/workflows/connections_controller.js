/*******************************************************************************
 * DISTRIBUTION C. Distribution authorized to U.S. Government agencies and their contractors. Other requests for this document shall be referred to the United States Air Force Research Laboratory.
 *
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
"use strict";

angular.module('roger_federation.Workflows')
    .controller('ConnectionsController', ['$rootScope', '$scope', '$http', '$stateParams', '$modalInstance', '$timeout', '$log', '$cookieStore', 'growl', 'WorkflowTemplate', 'WorkflowService', 'OntologyService', 'JointPaper', connectionsController]);

function connectionsController($rootScope, $scope, $http, $stateParams, $modalInstance, $timeout, $log, $cookieStore, growl, WorkflowTemplate, WorkflowService, OntologyService, JointPaper) {
    $scope.activeConnections = [];
    $scope.filteredActiveConnections = [];
    $scope.selectedCa = $rootScope.selectedCa ? $rootScope.selectedCa : 'All';
    $scope.knownCas = [];
    var cellView;

    pollActiveConnections()
    getCaGroups();

    function filterActiveConnections() {
        let connections = []
        $scope.activeConnections.forEach(activeConnection => {
            activeConnection.groupIdentitiesSet = new Set(activeConnection.groupIdentities)
            if ($scope.selectedCa === 'All') {
                connections.push(activeConnection)
            } else {
                if (activeConnection.groupIdentitiesSet.has($scope.selectedCa))
                    connections.push(activeConnection)
            }
        })
        $scope.filteredActiveConnections = connections
    }

    function pollActiveConnections() {
        WorkflowService.getActiveConnections().then(function(activeConnections) {
            $scope.activeConnections = activeConnections
            filterActiveConnections()
        }).catch(e => {})
        $timeout(pollActiveConnections, 2000);
    }

    function getCaGroups() {
        WorkflowService.getKnownCaGroups().then(
            function(caList) {
                $scope.knownCas = caList;
            },
            function (result) {
                console.log("Unable to load list of know CA's, " + result);
            }
        );
    };

    $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
    };

    $scope.updateFilter = function() {
        filterActiveConnections()
    }
}