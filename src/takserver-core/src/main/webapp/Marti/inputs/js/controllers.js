'use strict';

var inputManagerControllers = angular.module('inputManagerControllers', []);

inputManagerControllers.controller('InputListCtrl', ['$rootScope', '$window', '$scope', '$http', '$location', '$timeout', '$interval', 
  'InputManagerService', 'DataFeedManagerService', 'MessagingConfigService', 'securityConfigService', 'MetricsService', 'QosService',
      function ($rootScope, $window, $scope, $http, $location, $timeout, $interval, InputManagerService, DataFeedManagerService, MessagingConfigService, securityConfigService, MetricsService, QosService) {
			$scope.hasAdminRole = false;
			$scope.dbIsConnected = true;
            $scope.maxConnections = 0;
			$rootScope.deliveryTableApiSent = false;
			$rootScope.readTableApiSent = false;
			(function setAdmin() {
			  $http.get('/Marti/api/util/isAdmin').then(function(response){
		    	  $scope.hasAdminRole = response.data;
		        });
		    })();
		    
		    // QoS control

			$scope.toggleDeliveryEnabled = function() {
			    $rootScope.deliveryEnabled = !$rootScope.deliveryEnabled;
			    QosService.enableDelivery().update($rootScope.deliveryEnabled,
                	function(apiResponse) {
                   		console.log(apiResponse);
                	}
            	)
			};

			$scope.toggleReadEnabled = function() {
			    $rootScope.readEnabled = !$rootScope.readEnabled;
			    QosService.enableRead().update($rootScope.readEnabled,
                	function(apiResponse) {
                   		console.log(apiResponse);
                	}
            	)
			};

			$scope.toggleDOSEnabled = function() {
			    $rootScope.dosEnabled = !$rootScope.dosEnabled;
			    QosService.enableDOS().update($rootScope.dosEnabled,
                	function(apiResponse) {
                   		console.log(apiResponse);
                	}
            	)
			};

			$scope.saveDeliveryRateLimiter = function() {
				$rootScope.deliveryTableApiSent = true;
				$http.put('/Marti/api/qos/delivery/set', $rootScope.deliveryRateRules)
					.then(function(response){
						alert('Table saved successfully')
						$rootScope.deliveryTableApiSent = false;
					}).catch(function (error) {
						alert('Error saving table')
						$rootScope.deliveryTableApiSent = false;
					});
		}

			$scope.saveReadRateLimiter = function() {
				$rootScope.readTableApiSent = true;
				$http.put('/Marti/api/qos/read/set', $rootScope.readRateRules)
				.then(function(response){
					alert('Table saved successfully')
					$rootScope.readTableApiSent = false;
				}).catch(function (error) {
					alert('Error saving table')
					$rootScope.readTableApiSent = false;
				});
			};

			$scope.addRowDeliveryTable = function() {
				var row = {
					"clientThresholdCount": 0,
					"reportingRateLimitSeconds": 0
				}
				$rootScope.deliveryRateRules.push(row);
			};

			$scope.deleteRowDeliveryTable = function() {
				$rootScope.deliveryRateRules.pop();
			};

			$scope.addRowReadTable = function() {
				var row = {
					"clientThresholdCount": 0,
					"reportingRateLimitSeconds": 0
				}
				$rootScope.readRateRules.push(row);
			};

			$scope.deleteRowReadTable = function() {
				$rootScope.readRateRules.pop();
			};
		    
		    // populate QoS enable / disable
		    (function getQosEnabled() {
		      let conf = QosService.getQosConf().get(function(res) {
		      	console.log(res.data)

				$rootScope.deliveryRateRules = res.data.deliveryRateLimiter.rateLimitRule
				$rootScope.deliveryEnabled = res.data.deliveryRateLimiter.enabled
				
				$rootScope.readRateRules = res.data.readRateLimiter.rateLimitRule
				$rootScope.readEnabled = res.data.readRateLimiter.enabled

				$rootScope.dosRateRules = res.data.dosRateLimiter.dosLimitRule
				$rootScope.dosEnabled = res.data.dosRateLimiter.enabled
				$rootScope.dosIntervalSeconds = res.data.dosRateLimiter.intervalSeconds
		      })
		    })();
		    

		    $scope.toggleStoreForwardChatEnabled = function() {
			  $rootScope.storeForwardChatEnabled = !$rootScope.storeForwardChatEnabled;
			  console.log('updateStoreForwardChat', $rootScope.storeForwardChatEnabled);
			  if ($rootScope.storeForwardChatEnabled) {
				  $http.put('/Marti/api/inputs/storeForwardChat/enable');
			  } else {
				  $http.put('/Marti/api/inputs/storeForwardChat/disable');
			  }
		    };

		    // populate Store Forward Chat enabled / disabled
		    (function getStoreForwardChatEnabled() {
			  $http.get('/Marti/api/inputs/storeForwardChat/enabled').then(function(response){
				  console.log('StoreForwardChat enabled', response.data);
				  $rootScope.storeForwardChatEnabled = response.data;
			  });
		    })();

		    // populate QoS active rate limit
		    (function getActiveLimit() {

	    	  QosService.getActiveDeliveryRateLimit().get(function(res) {
                  for (var val in res.data) {
                    $rootScope.activeDeliveryThreshold = val;
                    $rootScope.activeDeliveryRateLimit = res.data[val];
                  }
                  
                  $timeout(getActiveLimit, 5000);
	    	  })

	    	  QosService.getActiveReadRateLimit().get(function(res) {
                  for (var val in res.data) {
                    $rootScope.activeReadThreshold = val;
                    $rootScope.activeReadRateLimit = res.data[val];
                  }
	    	  })

	    	  QosService.getActiveDOSRateLimit().get(function(res) {
                  for (var val in res.data) {
                    $rootScope.activeDOSThreshold = val;
                    $rootScope.activeDOSRateLimit = res.data[val];
                  }
	    	  })

		    })();
		    
		    // populate client count
		    (function getActiveClients() {
			  $http.get('/actuator/custom-network-metrics').then(function(response){
			  	
			  	  console.log('active clients', response.data.numClients);
			  	  
			  	  $rootScope.activeClients = response.data.numClients;
			  	  
			  	  $timeout(getActiveClients, 5000);
			  	  
		        });
		    })();
		    

		    (function pollDBStatus() {
		        MetricsService.getDatabaseMetrics().get(function (res) {
		            $scope.dbIsConnected = res.apiConnected && res.messagingConnected;
                    $scope.maxConnections = res.maxConnections;
		            $timeout(pollDBStatus, 5000);
		        }, err => {});
		    })();
		    
			function getSecConfig() {
				securityConfigService.query(
					function(response){ $scope.secConfig = response.data},
					function(response){ $scope.secConfig = null});
			}
	
    		$scope.showRmiError = false;
    		
			(function refreshMetrics() {InputManagerService.query(
					function(apiResponse) {	$scope.inputs = apiResponse.data;
											$scope.inputMetrics = [];
											$scope.streamingDataFeeds = [];
											$scope.pluginDataFeeds = [];
											$scope.federationDataFeeds = [];
											let inputLength = apiResponse.data.length;
											for (let i = 0; i < inputLength; i ++) {
												let loopInput = apiResponse.data[i];
												if (loopInput.input.type != null){
													if (loopInput.input.type === 'Streaming') {
														$scope.streamingDataFeeds.push(loopInput);
													} else if (loopInput.input.type === 'Plugin') {
														$scope.pluginDataFeeds.push(loopInput);
													} else if (loopInput.input.type === 'Federation') {
														$scope.federationDataFeeds.push(loopInput);
													}
												} else {
													$scope.inputMetrics.push(loopInput);
												}
											}

											$timeout(refreshMetrics,2000)}, 
					function() {$scope.showRmiError = true;})})();

					
			$scope.deleteObject = function (inputName) {
	        	
	        	if (inputName.trim() != '') {
		        	if (confirm('Proceed to delete the selected input? (Input Name: ' + inputName + ')')) {
			        	var ims = new InputManagerService();
			        	ims.$delete({id:inputName}, 
		        			function(data) {
		        				$scope.inputMetrics = InputManagerService.query(function(apiResponse) {$scope.inputMetrics = apiResponse.data});},
		        			function(data) {
		        				alert('An error occurred deleting the input definition.')}
			        	);
		        	}
	        	}
	        };

			$scope.deleteDataFeed = function (dataFeedName) {
	        	
	        	if (dataFeedName.trim() != '') {
		        	if (confirm('Proceed to delete the selected data feed? (Data Feed Name: ' + dataFeedName + ')')) {
			        	var dms = new DataFeedManagerService();
			        	dms.$delete({name:dataFeedName},
		        			function(data) {
		        				$scope.dataFeeds = DataFeedManagerService.query(function(apiResponse) {$scope.dataFeeds = apiResponse.data});},
		        			function(data) {
		        				alert('An error occurred deleting the data feed.')}
			        	);
		        	}
	        	}
	        };

	        $scope.modifyObject = function() {
	        	//Placeholder if we need preprocessing
	        };

	        $scope.createInput = function () {
	        	//Placeholder if we need preprocessing
	        };

	        $scope.createDataFeed = function () {
	        	//Placeholder if we need preprocessing
	        };
			

            $scope.actualNum = 0;
	        function getMsgConfig() {
 	            MessagingConfigService.query(
		            function(response) {
                        $rootScope.msgConfig = response.data;
                        if ($scope.msgConfig.connectionPoolAutoSize) {
                            $scope.actualNum = $scope.msgConfig.numAutoDbConnections;
                        } else {
                            $scope.actualNum = $scope.msgConfig.numDbConnections;
                        }
                    },
                    function(response){ $rootScope.msgConfig = null; });
	        }

            $scope.$on('$viewContentLoaded', function(event) {
                getMsgConfig();
                getSecConfig();
            });
}]);


inputManagerControllers.controller('InputCreationCtrl',
				   ['$scope', '$location', 'InputManagerService',
				    function ($scope, $location, InputManagerService) {
					$scope.input = new InputManagerService();
        				$scope.input.auth = 'X_509';
        				$scope.input.protocol = 'tls';
        				$scope.input.archive = 'true';
        				$scope.input.anongroup = 'false';
        				$scope.input.archiveOnly = 'false';
        				$scope.input.coreVersion = "2";
        				$scope.inputNameDuplicate = false;
        				$scope.serviceReportedMessages = false;
        				$scope.messages = [];
        				$scope.submitInProgress = false;
        				$scope.input.coreVersion2TlsVersions = "TLSv1.2,TLSv1.3";
        				$scope.tls = {"TLSv1":false,"TLSv1.1":false,"TLSv1.2":true,"TLSv1.3":true};
   					$scope.saveInput = function (input) {

   	        			    $scope.input.coreVersion2TlsVersions = '';
        				    if ($scope.input.coreVersion === "2"){
        					let numVersions = 0;
            					Object.entries($scope.tls).forEach(([key, value]) => {
            					    if (value) {
            						if (numVersions != 0) {
            						    $scope.input.coreVersion2TlsVersions += ',';
            						}
            						$scope.input.coreVersion2TlsVersions += key;
            						numVersions++;
            					    }
            					});

   	        				if ($scope.input.coreVersion2TlsVersions === ''){
   	        				    alert('Messaging input requires TLS version');
   	        				    return;
   	        				}
   	        			    } else {
   	        				delete $scope.input.coreVersion2TlsVersions;
   	        			    }

   	        			    $scope.submitInProgress = true;

   	        			    InputManagerService.save(input,
	        						     function(apiResponse) {
	        							 $location.path('/');},
	        						     function(apiResponse) {
	        							 $scope.serviceReportedMessages = true;
	        							 $scope.messages = apiResponse.data.messages;
	        							 alert('An error occurred saving the input definition. Please correct the errors and resubmit.');
	        							 $scope.submitInProgress = false;}
		        					    );
	        			}

   					$scope.isInputNameUnique = function(inputName) {

   	        			    if (inputName != null && inputName.trim() != '') {
	   	        			InputManagerService.get({id:inputName},
	   	        						function(apiResponse) {if (apiResponse.data != null) {$scope.inputNameDuplicate = true;} else {$scope.inputNameDuplicate = false};},
	   	        						function(apiResponse) {$scope.inputNameDuplicate = false;});
   	        			    }
   					}

					$scope.cancelInput = function() {
						$location.path("/");
					};

				    }]);

inputManagerControllers.controller('InputModificationCtrl', ['$scope', '$location', 'InputManagerService', '$routeParams',
                                                             function ($scope, $location, InputManagerService, $routeParams) {

	$scope.boolToStr = function(arg) {return arg ? 'True' : 'False'};

	$scope.queryInput = function(inputName) {
		InputManagerService.query(
				{id: $routeParams.id},
				function(apiResponse) {
					$scope.input = apiResponse.data.input;
					$scope.hideArchive = false;
					$scope.hideArchiveOnly = false;
					var protocol = apiResponse.data.input.protocol.toLowerCase();
					$scope.hideFilterGroupList = (apiResponse.data.input.auth.toLowerCase() != 'anonymous');
				},
				function(apiResponse) {
					$scope.showRmiError = true;
				}
		);
	}

	$scope.queryInput($routeParams.name);

	$scope.saveModifications = function(updatedInput) {

		if (updatedInput.archiveOnly && !updatedInput.archive) {
			alert("Cannot set archive only when archive is disabled!");
		} else {
			InputManagerService.update({id: updatedInput.name}, updatedInput,
						function(apiResponse) {
						$location.path('/');
					},
					function(apiResponse) {
						if (apiResponse.data && apiResponse.data.data && apiResponse.data.data['displayMessage']) {
							alert(apiResponse.data.data['displayMessage']);
						} else {
							alert(apiResponse.statusText);
						}
					}
			)
		};
	}

	$scope.cancelInput = function() {
		$location.path("/");
	};
		
}]);

inputManagerControllers.controller('DataFeedCreationCtrl', 
					['$scope', '$location', 'DataFeedManagerService', '$routeParams',
                   function ($scope, $location, DataFeedManagerService, $routeParams) {
					$scope.dataFeed = new DataFeedManagerService();
					$scope.dataFeed.name = '';
					$scope.dataFeed.type = 'Streaming';
					$scope.dataFeed.tags = '';
					$scope.dataFeed.auth = 'X_509';
					$scope.dataFeed.protocol = 'tls';
					$scope.dataFeed.syncCacheRetentionSeconds = "3600";
					$scope.dataFeed.archive = 'true';
					$scope.dataFeed.anongroup = 'false';
					$scope.dataFeed.archiveOnly = 'false';
					$scope.dataFeed.coreVersion = "2";
					$scope.dataFeed.protocol = 'tls';
					$scope.dataFeed.sync = 'false';
					$scope.dataFeed.federated = 'true';
					$scope.inputNameDuplicate = false;
					$scope.serviceReportedMessages = false;
					$scope.messages = [];
					$scope.submitInProgress = false;
					$scope.tls = { "TLSv1": false, "TLSv1.1": false, "TLSv1.2": true, "TLSv1.3": true };

   					$scope.saveStreamingDataFeed = function (dataFeed) {
   	        			    $scope.submitInProgress = true;
							$scope.dataFeed.type = 'Streaming';
   	        			    DataFeedManagerService.save(dataFeed,
	        						     function(apiResponse) {
	        							 	$location.path('/');},
	        						     	function(apiResponse) {
	        							 	$scope.serviceReportedMessages = true;
	        							 	$scope.messages = apiResponse.data.messages;
	        							 	alert('An error occurred saving the input definition. Please correct the errors and resubmit.');
	        							 	$scope.submitInProgress = false;}
		        					    );
	        			}

   					$scope.savePluginDataFeed = function (dataFeed) {
   	        			    $scope.submitInProgress = true;
							$scope.dataFeed.type = 'Plugin';
							$scope.dataFeed.auth = 'ANONYMOUS';
							$scope.dataFeed.protocol = 'tls';
							$scope.dataFeed.anongroup = 'false';
							$scope.dataFeed.archiveOnly = 'false';
							$scope.dataFeed.coreVersion = "2";
							$scope.dataFeed.protocol = '';
   	        			    DataFeedManagerService.save(dataFeed,
	        						     function(apiResponse) {
	        							 	$location.path('/');},
	        						     	function(apiResponse) {
	        							 	$scope.serviceReportedMessages = true;
	        							 	$scope.messages = apiResponse.data.messages;
	        							 	alert('An error occurred saving the input definition. Please correct the errors and resubmit.');
	        							 	$scope.submitInProgress = false;}
		        					    );
	        			}

   					$scope.isInputNameUnique = function(inputName) {

   	        			    if (inputName != null && inputName.trim() != '') {
	   	        			DataFeedManagerService.get({name:inputName},
	   	        						function(apiResponse) {if (apiResponse.data != null) {$scope.inputNameDuplicate = true;} else {$scope.inputNameDuplicate = false};},
	   	        						function(apiResponse) {$scope.inputNameDuplicate = false;});
   	        			    }
   					}


					$scope.cancelInput = function() {
						$location.path("/");
					}

}]);

inputManagerControllers.controller('DataFeedModificationCtrl', ['$scope', '$location', 'DataFeedManagerService', '$routeParams',
                                                             function ($scope, $location, DataFeedManagerService, $routeParams) {
	
	$scope.boolToStr = function(arg) {return arg ? 'True' : 'False'};

	$scope.queryDataFeed = function(dataFeedName) {
		DataFeedManagerService.query(
				{name: dataFeedName},
				function(apiResponse) {
					$scope.dataFeed = apiResponse.data;
					$scope.hideArchive = false;
					$scope.hideArchiveOnly = false;
					$scope.hideFederated = false;
					var protocol = apiResponse.data.protocol.toLowerCase();
					$scope.hideFilterGroupList = (apiResponse.data.auth.toLowerCase() != 'anonymous');
				},
				function(apiResponse) {
					$scope.showRmiError = true;
				}
		);
	}

	$scope.queryDataFeed($routeParams.name);

	$scope.updateDataFeed = function(updatedDataFeed) {
			updatedDataFeed.filtergroup = updatedDataFeed.filterGroups;
			updatedDataFeed.tag = updatedDataFeed.tags;
			updatedDataFeed.group = $scope.dataFeed.group;
			DataFeedManagerService.update({name: updatedDataFeed.name}, updatedDataFeed,
						function(apiResponse) {
						$location.path('/');
					},
					function(apiResponse) {
						if (apiResponse.data && apiResponse.data.data && apiResponse.data.data['displayMessage']) {
							alert(apiResponse.data.data['displayMessage']);
						} else {
							alert(apiResponse.statusText);
						}
					}
			);
	}

	$scope.cancelDataFeed = function() {
		$location.path("/");
	}

}]);
