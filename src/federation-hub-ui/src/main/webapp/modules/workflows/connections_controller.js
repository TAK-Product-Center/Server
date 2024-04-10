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
    $scope.flows = [];
    var cellView;

    pollActiveConnections()
    getCaGroups();
    pollDataFlows();

    function filterActiveConnections() {
        let connections = []
        $scope.activeConnections.forEach(activeConnection => {
            activeConnection.groupIdentitiesSet = new Set(activeConnection.groupIdentities)
            activeConnection["bRead"] = 0
            activeConnection["reads"] = 0
            activeConnection["bWritten"] = 0
            activeConnection["writes"] = 0
            if($scope.flows != []){
                var _flows = $scope.flows;
                for(var flow of _flows){
                    console.log(activeConnection.connectionId);
                    console.log(flow["targetId"]);
                    if(flow["sourceId"] == activeConnection.connectionId){
                        activeConnection["bWritten"] = activeConnection["bWritten"] + flow["bytesWritten"]
                        activeConnection["writes"] = activeConnection["writes"] + flow["messagesWritten"]
                        activeConnection["bRead"] = activeConnection["bRead"] + flow["bytesRead"]
                        activeConnection["reads"] = activeConnection["reads"] + flow["messagesRead"]
                    }
                }
            }

            if ($scope.selectedCa === 'All') {
                connections.push(activeConnection)
            } else {
                if (activeConnection.groupIdentitiesSet.has($scope.selectedCa))
                    connections.push(activeConnection)
            }
        })
        $scope.filteredActiveConnections = connections
    }

    function pollDataFlows() {
        WorkflowService.getDataFlowStats().then(function(dataFlows) {
            $scope.flows = dataFlows.channelInfos;
          });
        $timeout(pollDataFlows, 2000);
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

    $scope.disconnect = function(ac) {        
        $scope.filteredActiveConnections = $scope.filteredActiveConnections.filter(function( obj ) {
          return obj.connectionId !== ac.connectionId;
        });

        WorkflowService.disconnectFederate(ac.connectionId).then(
            function() {
               
            },
            function (result) {
                console.log("Unable to disconnect federate, " + result);
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