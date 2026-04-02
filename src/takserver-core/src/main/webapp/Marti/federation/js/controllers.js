'use strict';

var federationManagerControllers = angular.module('federationManagerControllers', []);

federationManagerControllers.controller('FederatesListCtrl', ['$scope',
    '$location',
    '$timeout',
    '$http',
    'FederatesService',
    'OutgoingConnectionsService',
    'OutgoingConnectionStatusService',
    'ActiveConnectionsService',
    'FederateDetailsService',
    'FederationConfigService',
    'FederationClearEventsService',
    'MetricsService',

    function (
        $scope,
        $location,
        $timeout,
        $http,
        FederatesService,
        OutgoingConnectionsService,
        OutgoingConnectionStatusService,
        ActiveConnectionsService,
        FederateDetailsService,
        FederationConfigService,
        FederationClearEventsService,
        MetricsService
    ) {
        $scope.showRmiError = false;
        $scope.hasAdminRole = false;
        $scope.dbIsConnected = true;
        $scope.maxConnections = 0;
        $scope.actualNum = 0;

        $scope.federationTokens = []

        $scope.generateToken = function() {
            let now = Date.now();
            let expiration = $scope.newTokenExpiration === -1 ? -1 
                : $scope.newTokenExpiration + now

            $http({
                url: '/Marti/api/generateAndSaveFederationJwtToken',
                method: "POST",
                data: {
                  "name": $scope.newTokenName,
                  "expiration": expiration
                }
            })
            .then(function(apiResponse) {
                $scope.federationTokens.push(apiResponse.data.data)
                $scope.newTokenName = ''
                FederatesService.query(
                    function (apiResponse) {
                        $scope.federates = apiResponse.data;
                        $scope.showRmiError = false;
                    },
                    function () {
                        $scope.showRmiError = true;
                });
            }, 
            function(apiResponse) { 
               alert('An error occured generating federation token');
            });
        };

        $scope.federateNameExists = function(name) {
            if (!$scope.federates) {
                return false
            }
            else {
                return $scope.federates.find(f => f.name === name)
            }
        }

        $scope.previewToken = function(token) {
            return token.substring(0,8) + '...' + token.substring(token.length-8);
        };

        $scope.getDisplayDate = function(milliseconds) {
            return milliseconds === -1 ? -1 : new Date(milliseconds)
        };

        $scope.copyToken = function(token) {
            if (navigator && navigator.clipboard)
                navigator.clipboard.writeText(token)
        };

        (function dynamicUpdate() {
            ActiveConnectionsService.query(
                function (apiResponse) {
                    $scope.activeConnections = apiResponse.data;
                    $scope.showRmiError = false;
                },
                function () {
                    $scope.showRmiError = true;
                })

            OutgoingConnectionsService.query(
                function (apiResponse) {
                    $scope.outgoingConnections = apiResponse.data;
                    $scope.showRmiError = false;
                },
                function () {
                    $scope.showRmiError = true;
                });

            FederatesService.query(
                function (apiResponse) {
                     apiResponse.data.forEach(f => {
                         if (f.tokenFederate) {
                             f.isTokenExpired = f.tokenExpiration === -1 ? false 
                                 : new Date() > new Date(f.tokenExpiration)
                         }
                     })

                    $scope.federates = apiResponse.data;
                    $scope.showRmiError = false;
                },
                function () {
                    $scope.showRmiError = true;
                });

            $timeout(dynamicUpdate, 2000);
        })();

        (function setAdmin() {
            $http.get('/Marti/api/util/isAdmin').then(function (response) {
                $scope.hasAdminRole = response.data;
            })
        })();

        (function pollDBStatus() {
            MetricsService.getDatabaseMetrics().get(function (res) {
                $scope.dbIsConnected = res.apiConnected && res.messagingConnected;
                $scope.maxConnections = res.maxConnections;
                $timeout(pollDBStatus, 5000);
            }, err => {});
        })();
        
        (function getDbConfig() {
            $http.get('/Marti/api/inputs/config').then(
                function(response) {
                    if (response.data.data.connectionPoolAutoSize) {
                        $scope.actualNum = response.data.data.numAutoDbConnections;
                    } else {
                        $scope.actualNum = response.data.data.numDbConnections;
                    }
                }
            )
        })();

        function getFedConfig() {
            FederationConfigService.query(
                function (response) {
                    $scope.fedConfig = response.data;
                    if ($scope.fedConfig.missionFederationDisruptionToleranceRecencySeconds % 86400 === 0) {
                        $scope.fedConfig.missionFederationDisruptionToleranceRecencySeconds /= 86400;
                        $scope.fedConfig.missionFederationDisruptionToleranceRecencySeconds += " days";
                    } else if ($scope.fedConfig.missionFederationDisruptionToleranceRecencySeconds % 3600 === 0) {
                        $scope.fedConfig.missionFederationDisruptionToleranceRecencySeconds /= 3600;
                        $scope.fedConfig.missionFederationDisruptionToleranceRecencySeconds += " hours";
                    } else if ($scope.fedConfig.missionFederationDisruptionToleranceRecencySeconds === -1) {
                        $scope.fedConfig.missionFederationDisruptionToleranceRecencySeconds = "unlimited";
                    } else {
                        $scope.fedConfig.missionFederationDisruptionToleranceRecencySeconds += " seconds";
                    }
                    for (let missionInterval of $scope.fedConfig.missionInterval) {
                        if (missionInterval.recencySeconds % 86400 === 0) {
                            missionInterval.recencySeconds /= 86400;
                            missionInterval.recencySeconds += " days";
                        } else if (missionInterval.recencySeconds % 3600 === 0) {
                            missionInterval.recencySeconds /= 3600;
                            missionInterval.recencySeconds += " hours";
                        } else if (missionInterval.recencySeconds === -1) {
                            missionInterval.recencySeconds = "unlimited";
                        } else {
                            missionInterval.recencySeconds += " seconds";
                        }
                    }
                },
                function (response) {
                    $scope.fedConfig = null;
                });
        }

        $scope.$on('$viewContentLoaded', function (event) {
            getFedConfig();
        });

        $scope.changeConnectionStatus = function (connectionName, newStatus) {
            OutgoingConnectionStatusService.save({
                    name: connectionName,
                    status: newStatus
                },
                function (apiResponse) {},
                function () {
                    alert('An unexpected error occurred changing the outgoing status.');
                });
        }

        $scope.deleteOutgoingConnection = function (connectionName) {

            if (confirm('Are you sure you want to delete the outgoing connection: ' + connectionName)) {
                var ocs = new OutgoingConnectionsService();

                ocs.$delete({
                        name: connectionName
                    },
                    function (apiResponse) {},
                    function () {
                        alert('An unexpected error occurred deleting the outgoing connection.');
                    });
            }
        }

        $scope.deleteFederate = function (federateIdParam) {

            if (confirm('Are you sure you want to delete the federate: ' + federateIdParam)) {
                FederateDetailsService.remove({
                        federateId: federateIdParam
                    },
                    function (apiResponse) {},
                    function () {
                        alert("An unexpected error occured deleting the federate");
                    });
            }
        }

        $scope.clearFederationEvents = function() {
            FederationClearEventsService.clear(
                function (response) {alert('Federation Events Successfully Cleared');},
                function (response) {alert('There was an error clearing Federation Events');}
            );
        };

        $scope.showGroupMappingWarning = function(activeConnection) {
            let groupMappingEnabled = activeConnection.federateConfig.federatedGroupMapping;
            let autoMappingEnabled = activeConnection.federateConfig.automaticGroupMapping 
            let fallbackEnabled = activeConnection.federateConfig.fallbackWhenNoGroupMappings 
            let numMappings = activeConnection.federateConfig.inboundGroupMapping ? activeConnection.federateConfig.inboundGroupMapping.length : 0

            if (groupMappingEnabled) {
                if (fallbackEnabled || autoMappingEnabled) return false;

                if (numMappings === 0) return true
            } else {
                return false
            }
        }
    }
]);

federationManagerControllers.controller('FederateGroupsCtrl', ['$scope',
    '$location',
    '$http',
    'FederateGroupsService',
    'FederateGroupsMapService',
    'FederateGroupsMapAddService',
    'FederateGroupsMapRemoveService',
    'FederateRemoteGroupsService',
    'FederateGroupConfigurationService',
    'FederateDetailsService',
    '$routeParams',
    '$modal',
    function (
        $scope,
        $location,
        $http,
        FederateGroupsService,
        FederateGroupsMapService,
        FederateGroupsMapAddService,
        FederateGroupsMapRemoveService,
        FederateRemoteGroupsService,
        FederateGroupConfigurationService,
        FederateDetailsService,
        $routeParams,
        $modal
    ) {
        $scope.direction = "BOTH";
        $scope.federateId = $routeParams.id;
        $scope.federateName = $routeParams.name;

        $scope.federateGroups = [];
        $scope.federateGroupsMap = [];
        $scope.federateRemoteGroups = {};
        $scope.groupHopLimits = {};
        $scope.submitInProgress = false;

        $scope.getFederateGroups = function () {
            FederateGroupsService.query({
                    federateId: $scope.federateId
                },
                function (apiResponse) {
                    $scope.federateGroups = apiResponse.data;
                },
                function (apiResponse) {
                    alert('An unexpected error occurred retrieving the list of federate groups.');
                });
        }

        $scope.getFederateGroupsMap = function () {
                    $scope.federateGroupsMap = FederateGroupsMapService.query({
                            federateId: $scope.federateId
                        },
                        function (apiResponse) {
                            $scope.federateGroupsMap = apiResponse.data;
                        },
                        function (apiResponse) {
                            alert('An unexpected error occurred retrieving the list of federate groups.');
                        });
        }

        $scope.getFederateRemoteGroups = function () {
            FederateRemoteGroupsService.query({
                    federateId: $scope.federateId
                },
                function (apiResponse) {
                    $scope.federateRemoteGroups = apiResponse.data;
                },
                function (apiResponse) {
                    alert('An unexpected error occurred retrieving the list of federate groups.');
                });
        }

        $scope.backToFederates = function () {
            window.history.back();
        };

        $scope.addGroup = function (group, direction) {

            var objectExists = false;

            if (group == null || group.trim() == '' || direction == null || direction.trim() == '') {
                alert("Please select a group and a direction above and retry your request.");
            } else {
                for (var i = 0; i < $scope.federateGroups.length; i++) {
                    if ($scope.federateGroups[i].group === group &&
                        ($scope.federateGroups[i].direction === direction || direction == 'BOTH')) {
                        objectExists = true;
                        break;
                    }
                }

                if (objectExists) {
                    alert("You've already added this group/direction combination to this federate.");
                } else {
                    var federateGroupAssociation = {};
                    federateGroupAssociation["federateId"] = $scope.federateId;
                    federateGroupAssociation["group"] = group;
                    federateGroupAssociation["direction"] = direction;

                    FederateGroupsService.save(federateGroupAssociation,
                        function (apiResponse) {
                            $scope.getFederateGroups();
                            $scope.getFederateGroupHopLimits();
                        },
                        function (apiResponse) {
                            $scope.serviceReportedMessages = true;
                            $scope.messages = apiResponse.data.messages;
                            alert('An error occurred adding the group. Please correct the errors and resubmit.');
                            $scope.submitInProgress = false;
                        }
                    );
                }
            }
        }

        $scope.addGroupMap = function (remoteGroup, localGroup) {
                 if (remoteGroup == null || remoteGroup.trim() == '' || localGroup == null || localGroup.trim() == '') {
                    alert("Please add a remote group and a local group  and retry your request.");
                 } else {
                     FederateGroupsMapAddService.add({
                     federateId: $scope.federateId,
                     remoteGroup: remoteGroup,
                     localGroup: localGroup
                     },
                     function (apiResponse) {
                        // group mapping was added successfully. if this was the first mapping added,
                        // automatically enable group mapping for the user and alert them
                        if (Object.keys($scope.federateGroupsMap).length === 0) {
                            FederateDetailsService.query({
                                federateId: $scope.federateId
                            },
                            function (apiResponse) {
                                if (!apiResponse.data.federatedGroupMapping) {
                                    apiResponse.data.federatedGroupMapping = true
                                    FederateDetailsService.update(apiResponse.data,
                                        function (apiResponse) {
                                            alert('Federated Group mapping has been automatically Enabled! Go to the Fedeate Settings to Disable it.');
                                        },
                                        function (apiResponse) {
                                            alert('An error occurred saving the federate details.');
                                        }
                                    );
                                }
                            },
                            function () {
                                alert('An error occurred fetching the federate details.');
                            });
                        }

                         $scope.getFederateGroupsMap();
                     },
                     function (apiResponse) {
                          alert('An error occurred adding the group. Please correct the errors and resubmit.');
                          $scope.serviceReportedMessages = true;
                          $scope.messages = apiResponse.data.messages;
                          $scope.submitInProgress = false;
                     }
                     );
               } 
        }

        $scope.deleteGroupMap = function (remoteGroup, localGroup) {

                    FederateGroupsMapRemoveService.remove({
                        federateId: $scope.federateId,
                        remoteGroup: remoteGroup,
                        localGroup: localGroup
                        },
                        function (apiResponse) {
                            $scope.getFederateGroupsMap();
                        },
                        function (apiResponse) {
                            alert("An unexpected error occurred deleting the requested federate group.");
                        });
        }

        $scope.deleteObject = function (group, direction) {

            var fgs = new FederateGroupsService();

            fgs.$delete({
                    federateId: $scope.federateId,
                    group: group,
                    direction: direction
                },
                function (apiResponse) {
                    $scope.getFederateGroups();
                    $scope.getFederateGroupHopLimits();
                },
                function (apiResponse) {
                    alert("An unexpected error occurred deleting the requested federate group.");
                });

            // if the group is outbound, also delete it's group hop limit entry
            if (direction === "OUT") {
                $scope.deleteHopLimitForGroup(group)
            }
        }

        $scope.save = function () {
            $scope.submitInProgress = true;

            FederateGroupConfigurationService.save(null,
                function (apiResponse) {
                    $location.path('/');
                },
                function (apiResponse) {
                    $scope.serviceReportedMessages = true;
                    $scope.messages = apiResponse.data.messages;
                    alert('An error occurred saving the group configuration. Please correct the errors and resubmit.');
                    $scope.submitInProgress = false;
                }
            );
        }

        $scope.searchGroups = function () {

            var modalInstance = $modal.open({
                animation: true,
                templateUrl: 'partials/groupSearch.html',
                controller: 'GroupSearchCtrl',
                size: 'lg'
            });

            modalInstance.result.then(
                function (selectedItem) {
                    $scope.group = selectedItem;
                },
                function () { //Modal cancelled
                });
        }

        $scope.getHopLimitForGroup = function (group) {
            let foundVal =  $scope.groupHopLimits[group];
            return foundVal ? foundVal : '-1'
        }

        $scope.getFederateGroupHopLimits = function() {
            $scope.groupHopLimits = {}
            $http.get('/Marti/api/federate-outbound-groups-hop-limit/' + $scope.federateId).then(function (response) {
                let groupHopLimits = response.data.data;

                groupHopLimits.forEach(groupHopLimit => {
                    $scope.groupHopLimits[groupHopLimit.groupName] = groupHopLimit.hopLimit
                })
            })
        }

        $scope.setHopLimitForGroup = function (elemId, groupName) {
            if (!groupName || !elemId) return;

            let hopInput = document.getElementById(elemId)
            let hops = hopInput.value

            $http({
                url: '/Marti/api/federate-outbound-groups-hop-limit/'+ $scope.federateId,
                method: "POST",
                data: {
                        groupName: groupName,
                        hopLimit: hops
                    }
            })
            .then(function(apiResponse) {
                $scope.groupHopLimits[groupName] = parseInt(hops)
            }, 
            function(apiResponse) { 
               alert('An error occured setting hop limit');
            });
        }

        $scope.deleteHopLimitForGroup = function (groupName) {
            $http({
                url: '/Marti/api/federate-outbound-groups-hop-limit/'+ $scope.federateId + '?group=' + groupName,
                method: "DELETE",
            })
            .then(function(apiResponse) {
                delete $scope.groupHopLimits[groupName]
            }, 
            function(apiResponse) { 
               alert('An error occured setting hop limit');
            });
        }

        $scope.getAvailableHopLimitGroups = function() {
            let groups = $scope.federateGroups
                    .filter(g => g.direction === "OUTBOUND")
                    .filter(g => !(g.group in $scope.groupHopLimits))
                    .map(g => g.group)

            return groups
        }

        $scope.getFederateGroups();
        $scope.getFederateGroupsMap();
        $scope.getFederateRemoteGroups();
        $scope.getFederateGroupHopLimits();
    }
]);

federationManagerControllers.controller('FederateMissionsCtrl', ['$scope',
    '$location',
    'FederateDetailsService',
    'MissionListService',
    // 'FederateMissionsUpdateService',
    '$routeParams',
    '$http',
    '$modal',
    function (
        $scope,
        $location,
        FederateDetailsService,
        MissionListService,
        // FederateMissionsUpdateService,
        $routeParams,
        $http,
        $modal
    ) {
        $scope.federateId = $routeParams.id;
        $scope.federateName = $routeParams.name;
        $scope.submitInProgress = false;
        $scope.vbm_filter_checkbox = false;
        $scope.mission_federate_default = null;

        function getFederateMissionsFromConfig() {
            
         };

        function getAllVbmCopMissionsWithFederateSetting() {
            
            var federateMissionsFromConfig;

            FederateDetailsService.query({
                federateId: $scope.federateId
            },
            function (apiResponse) {
                console.log(apiResponse.data);
                
                if (apiResponse.data.mission == null){
                    federateMissionsFromConfig = [];
                }else{
                    federateMissionsFromConfig = apiResponse.data.mission;
                    if (apiResponse.data.missionFederateDefault == null){
                        $scope.mission_federate_default = true; //default value for mission_federate_default is true
                    }else{
                        $scope.mission_federate_default = apiResponse.data.missionFederateDefault;
                    }
                    
                }         
                $scope.showRmiError = false;

                MissionListService.query(
                     function(apiResponse2) {
                         var list_of_all_missions = apiResponse2.data;
                         $scope.missions = [];
                         var found;
                         for (var mission of list_of_all_missions) {
                            found = false;
                            for (var x of federateMissionsFromConfig){
                                if (x.name == mission.name){
                                    $scope.missions.push({'name': mission.name , 'enabled': x.enabled, 'tool': mission.tool });
                                    found = true;
                                    break;
                                }
                            }
                            if (found == false){
                                $scope.missions.push({'name': mission.name , 'enabled': null, 'tool': mission.tool});
                            } 
                         }
                         console.log($scope.missions);
                     },
                     function(apiResponse2) {
                        $scope.missions = [];
                        console.log('error');
                        console.log(apiResponse2);
                        alert('Error when querying mission for federate');
                     }
                );   
                
            },
            function () {
                $scope.showRmiError = true;
            });      
        };

        $scope.backToFederates = function () {
            console.log('back to federates');
            window.history.back();
        };

        $scope.cancel = function () {
            $scope.backToFederates();
        };

        $scope.vbm_filter_func = function (mission) { 
            if ($scope.vbm_filter_checkbox == false || ($scope.vbm_filter_checkbox == true && mission.tool.toLowerCase() == 'vbm')){
                return mission;
            }
        };

        $scope.saveFederateMissions = function () {
            $scope.submitInProgress = true;

            console.log($scope.missions); 
            var missions_with_defined_value_only = []; // filter out the missions whose values are not set to either true or false
            for (var mission of $scope.missions) {
                if (mission.enabled == true || mission.enabled == false){
                    missions_with_defined_value_only.push(mission);
                }
            }
            console.log(missions_with_defined_value_only); 

            // Note: Could not find a way to set both federateId in request path and the data in the request body using FederateMissionsUpdateService
            if ($scope.mission_federate_default == null){
                alert("Need to set default value for mission federate");
                return;
            }

            $http({
                url: '/Marti/api/federatemissions/'+ $scope.federateId,
                method: "PUT",
                data: {
                    missionFederateDefault: $scope.mission_federate_default,
                    missions: missions_with_defined_value_only
                    }
            })
            .then(function(apiResponse) {
                    console.log(apiResponse.data);
                    $scope.showRmiError = false;
                    $scope.submitInProgress = false;
                    alert("Successfully updated mission federation");
            }, 
            function(apiResponse) { 
                    $scope.showRmiError = true;
                    alert("Error saving the federate missions");
                    $scope.submitInProgress = false;
            });

        }

        getAllVbmCopMissionsWithFederateSetting();
    }
]);

federationManagerControllers.controller('OutgoingConnectionCreationCtrl', ['$scope',
    '$location',
    'OutgoingConnectionsService',
    function (
        $scope,
        $location,
        OutgoingConnectionsService
    ) {

        $scope.newOutgoingConnections = [];

        var firstNewOutgoing = new OutgoingConnectionsService();
        firstNewOutgoing.reconnectInterval = 30;
        firstNewOutgoing.enabled = 'true';
        firstNewOutgoing.protocolVersion = '2';
        firstNewOutgoing.maxRetriesDisplay = 0;
        firstNewOutgoing.unlimitedRetries = true;
        firstNewOutgoing.fallback = null;
        firstNewOutgoing.maxRetriesFallbackError = false;
        firstNewOutgoing.id = 0;

        $scope.newOutgoingConnections.push(firstNewOutgoing);


        OutgoingConnectionsService.query(
            function (apiResponse) {
                $scope.allOutgoingConnections = apiResponse.data;
                $scope.showRmiError = false;
            },
            function () {
                $scope.showRmiError = true;
            });

        $scope.displayNameDuplicate = false;
        $scope.serviceReportedMessages = false;
        $scope.messages = [];
        $scope.submitInProgress = false;

        $scope.cancel = function () {
            $location.path("/");
        };

        $scope.createFallback = function (outgoingConnection) {
            outgoingConnection.fallback = "create_new_fallback_connection"
            var fallbackConnection = new OutgoingConnectionsService();
            fallbackConnection.id = outgoingConnection.id + 1;
            fallbackConnection.parent = outgoingConnection.displayName;
            fallbackConnection.enabled = 'false';
            fallbackConnection.reconnectInterval = 30;
            fallbackConnection.protocolVersion = '2';
            fallbackConnection.maxRetriesDisplay = 0;
            fallbackConnection.unlimitedRetries = true;
            fallbackConnection.fallback = null;
            fallbackConnection.maxRetriesFallbackError = false;
            $scope.newOutgoingConnections.push(fallbackConnection);

            $scope.checkMaxRetries(outgoingConnection);
        }

        $scope.removeFallbacks = function (outgoingConnection) {
            $scope.newOutgoingConnections.length = outgoingConnection.id + 1;
            if (outgoingConnection.fallback === "create_new_fallback_connection") {
                outgoingConnection.fallback = null;
            }
            $scope.checkMaxRetries(outgoingConnection);
        }

        $scope.saveOutgoingConnections = function (outgoingConnections) {
            for (i = 0; i + 1 < outgoingConnections.length; i++) {
                var og = outgoingConnections[i];

                og.fallback = outgoingConnections[i + 1].displayName;
                $scope.saveOutgoingConnection(og);
                if ($scope.serviceReportedMessages) {
                    console.log("one of the saves failed: " + outgoingConnection.displayName);
                    return;
                }
            }
            var final_og = outgoingConnections[outgoingConnections.length - 1];
            $scope.saveOutgoingConnection(final_og);
        }

        $scope.saveOutgoingConnection = function (outgoingConnection) {
            $scope.submitInProgress = true;
            OutgoingConnectionsService.save(outgoingConnection,
                function (apiResponse) {
                    $location.path('/');
                },
                function (apiResponse) {
                    $scope.serviceReportedMessages = true;
                    $scope.messages = apiResponse.data.messages;
                    alert('An error occurred saving the outgoing connection. Please correct the errors and resubmit.');
                    $scope.submitInProgress = false;
                }
            );
        }

        $scope.checkMaxRetries = function (outgoingConnection) {
            if (outgoingConnection.unlimitedRetries == true && outgoingConnection.fallback) {
                outgoingConnection.maxRetriesFallbackError = true;
            } else {
                outgoingConnection.maxRetriesFallbackError = false;
            }
        }

        $scope.isDisplayNameUnique = function (outgoingConnection) {
            if (!outgoingConnection.displayName) {
                return;
            }
            var displayName = outgoingConnection.displayName.toUpperCase();
            if (displayName != null && displayName.trim() != '') {
                outgoingConnection.displayNameDuplicate = false;

                for (i in $scope.newOutgoingConnections) {
                    if ($scope.newOutgoingConnections[i].displayName != null &&
                        displayName === $scope.newOutgoingConnections[i].displayName.toUpperCase() &&
                        outgoingConnection.id != $scope.newOutgoingConnections[i].id) {
                        outgoingConnection.displayNameDuplicate = true;
                    }
                }
                for (i in $scope.allOutgoingConnections) {
                    if (displayName === $scope.allOutgoingConnections[i].displayName.toUpperCase()) {
                        outgoingConnection.displayNameDuplicate = true;
                    }
                }
            }
        }

        $scope.toggleUseToken = function(outgoingConnection) {
            if (outgoingConnection.useToken && !outgoingConnection.tokenType) {
                outgoingConnection.tokenType = 'manual'
            }
        }
    }
]);

federationManagerControllers.controller('OutgoingConnectionModificationCtrl', ['$scope',
    '$location',
    '$routeParams',
    'OutgoingConnectionsService',
    function (
        $scope,
        $location,
        $routeParams,
        OutgoingConnectionsService
    ) {

        $scope.originalOutgoingDisplayName = $routeParams.id
        $scope.allOutgoingConnections = [];
        $scope.originalOutgoing = {}
        $scope.modifiedOutgoing = {}

        OutgoingConnectionsService.query(
            function (apiResponse) {
                $scope.allOutgoingConnections = apiResponse.data;
                $scope.showRmiError = false;

                $scope.originalOutgoing = $scope.allOutgoingConnections.find(outgoing => { 
                    return outgoing.displayName === $scope.originalOutgoingDisplayName
                })

                $scope.modifiedOutgoing = JSON.parse(JSON.stringify($scope.originalOutgoing))
            },
            function () {
                $scope.showRmiError = true;
            });

        $scope.cancel = function () {
            $location.path("/");
        };

        $scope.saveOutgoingConnection = function (outgoingConnection) {
            // if outgoing is enabled, warn that current changes will restart connection
            if ($scope.originalOutgoing.enabled && $scope.modifiedOutgoing.enabled) {
                if (confirm('Config changes will restart the connection')) {
                    // nothing to do
                } else {
                  alert('Save Canceled.');
                  return;
                }
            }

            $scope.submitInProgress = true;
            OutgoingConnectionsService.update({
                    'original': $scope.originalOutgoing,
                    'update': $scope.modifiedOutgoing
                },
                function (apiResponse) {
                     $location.path('/');
                },
                function (apiResponse) {
                    $scope.serviceReportedMessages = true;
                    $scope.messages = apiResponse.data.messages;
                    alert('An error occurred saving the outgoing connection. Please correct the errors and resubmit.');
                    $scope.submitInProgress = false;
                }
            );
        }

        $scope.checkMaxRetries = function (outgoingConnection) {
            if (outgoingConnection.unlimitedRetries == true && outgoingConnection.fallback) {
                outgoingConnection.maxRetriesFallbackError = true;
            } else {
                outgoingConnection.maxRetriesFallbackError = false;
            }
        }

        $scope.isDisplayNameUnique = function (outgoingConnection) {
            if (!outgoingConnection.displayName) {
                return;
            }
            var displayName = outgoingConnection.displayName.toUpperCase();
            if (displayName != null && displayName.trim() != '') {
                outgoingConnection.displayNameDuplicate = false;

                for (i in $scope.allOutgoingConnections) {
                    if (displayName === $scope.allOutgoingConnections[i].displayName.toUpperCase()) {
                        // ignore duplicate with original self
                        if (displayName === $scope.originalOutgoing.displayName.toUpperCase())
                            continue

                        outgoingConnection.displayNameDuplicate = true;
                        return
                    }
                }
            }
        }

        $scope.toggleUseToken = function(outgoingConnection) {
            if (outgoingConnection.useToken && !outgoingConnection.tokenType) {
                outgoingConnection.tokenType = 'manual'
            }
        }
    }
]);


federationManagerControllers.controller('FederateContactsListCtrl', ['$scope',
    '$location',
    '$timeout',
    'FederateContactsService',
    '$routeParams',
    function (
        $scope,
        $location,
        $timeout,
        FederateContactsService,
        $routeParams
    ) {
        $scope.showRmiError = false;

        $scope.federateId = $routeParams.id;
        $scope.federateName = $routeParams.name;

        FederateContactsService.query({
                federateId: $scope.federateId
            },
            function (apiResponse) {
                $scope.federateContacts = apiResponse.data;
                $scope.showRmiError = false;
            },
            function () {
                $scope.showRmiError = true;
            });

        $scope.backToFederates = function () {
            $location.path("/");
        };

    }
]);

federationManagerControllers.controller('GroupSearchCtrl', ['$scope',
    '$location',
    '$timeout',
    'GroupSearchService',
    'GroupPrefixLookupService',
    '$modalInstance',
    function (
        $scope,
        $location,
        $timeout,
        GroupSearchService,
        GroupPrefixLookupService,
        $modalInstance
    ) {
        $scope.showRmiError = false;
        $scope.groupResults = null;
        $scope.groupNameFilter = "";
        $scope.groupSelection = {};

        $scope.searchGroups = function (filterValue) {
            GroupSearchService.query({
                    groupNameFilter: filterValue
                },
                function (apiResponse) {
                    $scope.groupResults = apiResponse.data;
                    $scope.showRmiError = false;
                },
                function () {
                    $scope.showRmiError = true;
                });
        };

        $scope.getGroupPrefix = function () {
            GroupPrefixLookupService.get({},
                function (apiResponse) {
                    $scope.groupNameFilter = apiResponse.data;
                    $scope.showRmiError = false;
                },
                function () {
                    $scope.showRmiError = true;
                });
        }

        $scope.selectGroup = function () {
            if (typeof $scope.groupSelection.dn === 'undefined') {
                alert("Please select a group in the search results. To search for groups, click the Search button.");
            } else {
                $modalInstance.close($scope.groupSelection.dn);
            }
        }

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };

        $scope.getGroupPrefix();
    }
]);

federationManagerControllers.controller('FederateDetailsCtrl', ['$scope',
    '$location',
    '$http',
    'FederateDetailsService',
    '$routeParams',
    '$modal',
    function (
        $scope,
        $location,
        $http,
        FederateDetailsService,
        $routeParams,
        $modal
    ) {
        $scope.federateId = $routeParams.id;
        $scope.federateName = $routeParams.name;

        $scope.submitInProgress = false;

        $scope.regenerateToken = function() {
            let now = Date.now();
            let expiration = $scope.federateDetails.newTokenExpiration === -1 ? -1 
                : $scope.federateDetails.newTokenExpiration + now


            $http({
                url: '/Marti/api/generateFederationJwtToken',
                method: "POST",
                data: {
                  "name": $scope.federateDetails.name,
                  "expiration": expiration
                }
            })
            .then(function(apiResponse) {
                $scope.federateDetails.id = apiResponse.data.data.token
                $scope.federateDetails.tokenExpiration = expiration
                alert('Federation token has been regenerated. Please save the form.');
            }, 
            function(apiResponse) { 
               alert('An error occured generating federation token');
            });
        };

        $scope.cancel = function () {
            $location.path("/");
        };

        $scope.boolToStr = function (arg) {
            return arg ? 'True' : 'False'
        };

        FederateDetailsService.query({
                federateId: $scope.federateId
            },
            function (apiResponse) {
                $scope.federateDetails = apiResponse.data;
                $scope.showRmiError = false;
            },
            function () {
                $scope.showRmiError = true;
            });

        $scope.saveFederateDetails = function (federateDetails) {
            $scope.submitInProgress = true;

            FederateDetailsService.update(federateDetails,
                function (apiResponse) {
                    $location.path('/');
                },
                function (apiResponse) {
                    $scope.serviceReportedMessages = true;
                    $scope.messages = apiResponse.data.messages;
                    alert('An error occurred saving the federate details. Please correct the errors and resubmit.');
                    $scope.submitInProgress = false;
                }
            );
        }
    }
]);

federationManagerControllers.controller(
    'FederationConfigCtrl',
    ['$scope',
     '$location',
     'FederationConfigService',
     'MissionListService',
     function (
         $scope,
         $location,
         FederationConfigService,
         MissionListService)
     {
         function getFedConfig() {
             FederationConfigService.query(
                 function (response) {
                     $scope.fedConfig = response.data;
                     $scope.fedConfig.coreVersion = $scope.fedConfig.coreVersion.toString();
                     $scope.tls = {};
                     $scope.fedConfig.v1Tls.forEach(function (item) {
                         $scope.tls[item.tlsVersion] = true;
                     });
                     handleRecencySeconds($scope.fedConfig);
                 },
                 function (response) {
                     $scope.fedConfig = null;
                 });
         };

         function handleRecencySeconds(fedConfig) {
             let defaultTime = fedConfig.missionFederationDisruptionToleranceRecencySeconds;
             $scope.unlimited = false;
             if (defaultTime % 86400 === 0) {
                 $scope.cache_length_num = defaultTime / 86400;
                 $scope.cache_length_type = "days";
             } else if (defaultTime % 3600 === 0) {
                 $scope.cache_length_num = defaultTime / 3600;
                 $scope.cache_length_type = "hours";
             } else if (defaultTime == -1) {
                 $scope.cache_length_num = 2;
                 $scope.cache_length_type = "days";
                 $scope.unlimited = true;
             } else {
                 $scope.cache_length_num = defaultTime;
                 $scope.cache_length_type = "seconds";
             }
             if (fedConfig.missionInterval.length === 0) {
                 $scope.mission_caches = [
                     {
                         "name": "",
                         "time": $scope.cache_length_num,
                         "time_type": $scope.cache_length_type,
                         "unlimited": $scope.unlimited
                     }
                 ];
             } else {
                 for (let missionInterval of fedConfig.missionInterval) {
                     if (missionInterval.recencySeconds === -1) {
                         missionInterval.time = $scope.cache_length_num;
                         missionInterval.time_type = $scope.cache_length_type;
                         missionInterval.unlimited = true;
                     } else if (missionInterval.recencySeconds % 86400 === 0) {
                         missionInterval.time = missionInterval.recencySeconds / 86400;
                         missionInterval.time_type = "days";
                         missionInterval.unlimited = false;
                     } else if (missionInterval.recencySeconds % 3600 === 0) {
                         missionInterval.time = missionInterval.recencySeconds / 3600;
                         missionInterval.time_type = "hours";
                         missionInterval.unlimited = false;
                     } else {
                         missionInterval.time = missionInterval.recencySeconds;
                         missionInterval.time_type = "seconds";
                         missionInterval.unlimited = false;
                     }
                 }
                 $scope.mission_caches = fedConfig.missionInterval;
             }
         }

         $scope.$on('$viewContentLoaded', function (event) {
             getFedConfig();
         });

         $scope.addPort = function () {
             var newPort = new FederationConfigService();
             newPort.tlsVersion = "TLSv1.2";
             $scope.fedConfig.v1Ports.push(newPort);
         };

         $scope.setCoreVersion = function (version) {
             $scope.fedConfig.coreVersion = version;
         };

         $scope.handleTlsCheckbox = function (tls) {
             if ($scope.fedConfig.v1Tls.length === 0) {
                 $scope.fedConfig.v1Tls.push({
                     tlsVersion: tls
                 });
             } else {
                 $scope.fedConfig.v1Tls.forEach(function (item, index, object) {
                     if (item.tlsVersion === tls) {
                         object.splice(index, 1);
                     } else if (index === $scope.fedConfig.v1Tls.length - 1) {
                         $scope.fedConfig.v1Tls.push({
                             tlsVersion: tls
                         });
                     }
                 });

             }
         };

         $scope.cache_length_type = "days";
         $scope.cache_length_num = 2;
         $scope.unlimited = false;

         $scope.mission_list = [];
         $scope.used_missions = {};
         $scope.mission_caches = [];


         $scope.addMissionCache = function() {
             $scope.determineUsedMissions();
             $scope.mission_caches.push(
                 {
                     "name": "",
                     "time": $scope.cache_length_num,
                     "time_type": $scope.cache_length_type,
                     "unlimited": $scope.unlimited
                 }
             );
         };

         $scope.removeMissionCache = function(index) {
             $scope.mission_caches.splice(index, 1);
             $scope.determineUsedMissions();
             if ($scope.mission_caches.length === 0) {
                 $scope.addMissionCache();
             }
         };

         $scope.determineUsedMissions = function() {
             for (var mission of $scope.mission_list) {
                 $scope.used_missions[mission] = false;
             }
             for (var mission_cache of $scope.mission_caches) {
                 if (mission_cache.name ) {
                     $scope.used_missions[mission_cache.name] = true;
                 }
             }
         };


         (function() {
             MissionListService.query(
                 function(response) {
                     for (var mission of response.data) {
                         $scope.mission_list.push(mission.name);
                         $scope.used_missions[mission.name] = false;
                     }
                 },
                 function(response) {
                     console.log('error');
                     console.log(response);
                 }
             )
         })();

         function saveRecencySeconds(config) {
             let time = $scope.cache_length_num;
             if ($scope.unlimited) {
                 time = -1;
             } else if ($scope.cache_length_type === "days") {
                 time *= 86400;
             } else if ($scope.cache_length_type === "hours") {
                 time *= 3600;
             }

             config.missionFederationDisruptionToleranceRecencySeconds = time;

             config.missionInterval = [];
             for (let missionInterval of $scope.mission_caches) {
                 if (missionInterval.name === "") {
                     continue;
                 }
                 let time = 0;
                 if (missionInterval.unlimited) {
                     time = -1;
                 } else if (missionInterval.time_type === "days") {
                     time = missionInterval.time * 86400;
                 } else if (missionInterval.time_type === "hours") {
                     time = missionInterval.time * 3600;
                 } else {
                     time = missionInterval.time;
                 }
                 config.missionInterval.push({
                     "name": missionInterval.name,
                     "recencySeconds": time
                 });
             }

         }

         $scope.addFileExtension = function (fileExtension) {
            fileExtension = fileExtension.trim().toLowerCase();
            if (! $scope.fedConfig.fileExtension.includes(fileExtension)) {
                var length = $scope.fedConfig.fileExtension.length;
                // backend code does not support multiple file lists yet, so clear out the array
                $scope.fedConfig.fileExtension.splice(0, length);
                $scope.fedConfig.fileExtension.push(fileExtension);
            }
            // reset the fileExt data model in the form
            $scope.fileExtension = "";
         };

         $scope.saveFederationConfig = function (config) {
             $scope.submitInProgress = true;

             saveRecencySeconds(config);

             FederationConfigService.update(config,
                                            function (apiResponse) {
                                                $location.path('/');
                                            },
                                            function (apiResponse) {
                                                alert(apiResponse.data.data)
                                            }
                                           );
         };
     }
    ]);
