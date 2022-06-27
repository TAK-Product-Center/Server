'use strict';

var databaseManagerControllers = angular.module('databaseManagerControllers', []);

databaseManagerControllers.controller('DatabaseDisplayConfigCtrl',
    ['$scope', '$http', '$timeout', 'DatabaseConfigService', 'MetricsService', 'DatabaseMetricsService',
        function($scope, $http, $timeout, DatabaseConfigService, MetricsService, DatabaseMetricsService) {
            $scope.hasAdminRole = false;
            $scope.dbIsConnected = true;
            $scope.maxConnections = 0;
            $scope.dbVersion;
            (function pollDBStatus() {
                MetricsService.getDatabaseMetrics().get(function (res) {
                    $scope.dbIsConnected = res.apiConnected && res.messagingConnected;
                    $scope.maxConnections = res.maxConnections;
                    $scope.dbVersion = res.serverVersion;
                    $timeout(pollDBStatus, 5000);
                }, err => {});
            })();
            (function setAdmin() {
              $http.get('/Marti/api/util/isAdmin').then(function(response){
                  $scope.hasAdminRole = response.data;
                });
            })();

            $scope.numCotEvents = 0;
            $scope.numCotImages = 0;
            (function getCotCounts() {
                DatabaseMetricsService.get(function(res) {
                    console.log(res);
                    $scope.numCotEvents = res.data.cotEvents;
                    $scope.numCotImages = res.data.cotImages;
                }, err => {});
            })();

            $scope.vacuumDB = function() {
                var req = {
                    method: 'POST',
                    url: '/Marti/DBAdmin',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
                    data: "vacuum"
                }
                $http(req).then(
                    function(response) {
                        alert("Successfully sent vacuum request to DB");
                    },
                    function(response) {
                        console.log("Failed to vacuum DB");
                    }
                )
            }

            $scope.reindexDB = function() {
                var req = {
                    method: 'POST',
                    url: '/Marti/DBAdmin',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
                    data: "reindex"
                }
                $http(req).then(
                    function(response) {
                        alert("Successfully sent re-index request to DB");
                    },
                    function(response) {
                        console.log("Failed to re-index DB");
                    }
                )
            }

            $scope.dbConfig = null;
            $scope.actualNum;
            $scope.tooltip = "";
            DatabaseConfigService.query(
                function(response) {
                    $scope.dbConfig = response.data;
                    if ($scope.dbConfig.connectionPoolAutoSize) {
                        $scope.actualNum = $scope.dbConfig.numAutoDbConnections;
                        $scope.tooltip = "This value is automatically determined by TAK Server at startup based on compute power of this machine."
                    } else {
                        $scope.actualNum = $scope.dbConfig.numDbConnections;
                        $scope.tooltip = "When set, this will configure TAK Server to use a number of database connections (" + $scope.dbConfig.numAutoDbConnections + ") based on the compute power available to this machine.";
                    }
                },
                function(response) {
                    $scope.dbConfig = null;
                }
            )
        }]);

databaseManagerControllers.controller('DatabaseModConfigCtrl',
    ['$scope', '$timeout', '$http', '$location', 'DatabaseConfigService', 'MetricsService',
        function($scope, $timeout, $http, $location, DatabaseConfigService, MetricsService) {

            $scope.hasAdminRole = false;
            $scope.dbIsConnected = true;
            $scope.maxConnections = 0;
            $scope.dbVersion;
            (function pollDBStatus() {
                MetricsService.getDatabaseMetrics().get(function (res) {
                    $scope.dbIsConnected = res.apiConnected && res.messagingConnected;
                    $scope.maxConnections = res.maxConnections;
                    $scope.dbVersion = res.serverVersion;
                    console.log($scope.actualNum + " >? " + $scope.maxConnections);
                    $timeout(pollDBStatus, 5000);
                }, err => {});
            })();
            (function setAdmin() {
              $http.get('/Marti/api/util/isAdmin').then(function(response){
                  $scope.hasAdminRole = response.data;
                });
            })();

            function getDbConfig() {
                DatabaseConfigService.query(
                    function(response) {
                        $scope.dbConfig = response.data;
                        if ($scope.dbConfig.connectionPoolAutoSize) {
                            $scope.actualNum = $scope.dbConfig.numAutoDbConnections;
                        } else {
                            $scope.actualNum = $scope.dbConfig.numDbConnections;
                        }
                    },
                    function(response) {
                        $scope.dbConfig = null; 
                    }
                );
            }
            
            $scope.changeNumDbConn = function() {
                if ($scope.dbConfig.connectionPoolAutoSize) {
                    $scope.actualNum = $scope.dbConfig.numAutoDbConnections;
                    $scope.dbConfig.numDbConnections = $scope.actualNum;
                } else {
                    $scope.actualNum = $scope.dbConfig.numDbConnections;
                }
            }

            $scope.saveDatabaseConfig = function(config) {
                $scope.submitInProgress = true;

                DatabaseConfigService.update(config,
                    function(apiResponse) {
                        $location.path('/');
                    },
                    function(apiResponse) {
                        $scope.serviceReportedMessages = true;
                        $scope.messages = apiResponse.data.messages;
                        $scope.submitInProgress = false;
                    }
                );
            }

            $scope.$on('$viewContentLoaded', function(event) {
                getDbConfig();
            });
        }]);
