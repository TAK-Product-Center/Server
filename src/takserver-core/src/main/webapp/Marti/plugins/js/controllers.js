'use strict';

var pluginManagerControllers = angular.module('pluginManagerControllers', []);

pluginManagerControllers.controller('PluginListCtrl', [
    '$scope',
    '$timeout',
    'AllPluginsInfoService',
    'PluginEnabledService',
    'PluginStartedService',
    'AllPluginStartedService',
    'PluginArchiveService',
    function($scope, $timeout, AllPluginsInfoService, PluginEnabledService, PluginStartedService, AllPluginStartedService, PluginArchiveService) {

	var unsorted_class = "glyphicon glyphicon-sort";
	var ascend_class = "glyphicon glyphicon-arrow-up";
	var descend_class = "glyphicon glyphicon-arrow-down";

	$scope.sort_class = {'name': unsorted_class, 'sender': unsorted_class}

	var sort_directions = {'name': 1, 'sender': 1};
	var current_sort = '';

	(function pollPluginInfo() {
	    AllPluginsInfoService.get(
		function (res) {
		    for (var plugin of res.data) {
                if (plugin['receiver'] && plugin['sender']) {
                    plugin['direction'] = '<span class="glyphicon glyphicon-log-out"></span>, <span class="glyphicon glyphicon-log-in"></span>'
                } else if (plugin['receiver']) {
				    plugin['direction'] = '<span class="glyphicon glyphicon-log-in"></span>';
				} else if (plugin['sender']) {
				    plugin['direction'] = '<span class="glyphicon glyphicon-log-out"></span>';
				} else if (plugin['interceptor']) {
				    plugin['direction'] = '<span class="glyphicon glyphicon-flash"></span>';
				} else {
				    plugin['direction'] = "";
				}
				if (plugin.exceptionMessage != null) {
				    plugin['status'] = "Startup Error: " + plugin.exceptionMessage;
				    plugin['status-class'] = "bg-danger";
				} else {
				    if (plugin['started']){
    				    plugin['status'] = "Started";
                        plugin['status-class'] = "";
                        plugin['start_stop_command'] = "Stop";                
				    }else{
				        plugin['status'] = "Stopped";
                        plugin['status-class'] = "";
                        plugin['start_stop_command'] = "Start";     
				    }
				}				
		    }
		    $scope.pluginMetadata = res.data;
		    sort();
		    $scope.showPluginError = false;
		    $timeout(pollPluginInfo, 2000);
		},
		function(res) {
		    $scope.pluginMetadata = [];
		    $scope.showPluginError = true;
		    $timeout(pollPluginInfo, 10000);
		}
	    );
	})();

	$scope.status_class = function(exceptionMessage) {
	    if (exceptionMessage != null) {
		return "bg-danger";
	    }
	    return "";
	};

	$scope.sender_receiver_title = function(row) {
        if (row.sender && row.receiver) {
            return "sender receiver";
        } else if (row.sender) {
		  return "sender";
	    } else if (row.receiver) {
		  return "receiver";
	    } else if (row.interceptor) {
		  return "interceptor";
	    }
	};

	$scope.toggle_sort = function (attribute) {
	    sort_directions[attribute] *= -1
	    current_sort = attribute;
	    for (var key in $scope.sort_class) {
		$scope.sort_class[key] = unsorted_class;
	    }
	    if (sort_directions[attribute] == 1) {
		$scope.sort_class[attribute] = ascend_class;
	    } else {
		$scope.sort_class[attribute] = descend_class;
	    }
	    sort();
	}

	$scope.handlePluginEnabledUpdate = function (plugin, input) {
        input.currentTarget.disabled = true

   		PluginEnabledService.save({
            name: plugin.name,
            status: !plugin.enabled
        },
        function (apiResponse) {},
        function () {
            alert('An unexpected error occurred changing the plugin enabled status.');
        });
	}
	
    $scope.handlePluginStartedUpdate = function (plugin, input) {
        input.currentTarget.disabled = true

        PluginStartedService.save({
            name: plugin.name,
            status: !plugin.started
        },
        function (apiResponse) {},
        function () {
            alert('An unexpected error occurred changing the plugin started status.');
        });
    }

	$scope.handleArchiveUpdate = function (plugin, input) {
        input.currentTarget.disabled = true 

   		PluginArchiveService.save({
            name: plugin.name,
            archiveEnabled: !plugin.archiveEnabled
        },
        function (apiResponse) {},
        function () {
            alert('An unexpected error occurred changing the plugin archive setting.');
        });
	}

	$scope.handleAllStatus = function (status) {
		document.getElementById('startButton').disabled = true
		document.getElementById('stopButton').disabled = true
   		
   		AllPluginStartedService.save({
            status: status
        },
        function (apiResponse) {},
        function () {
            alert('An unexpected error occurred changing the plugins status.');
        });

        setTimeout(function() {
        	document.getElementById('startButton').disabled = false
			document.getElementById('stopButton').disabled = false
        }, 2000)
	}

	function sort() {
	    if (current_sort === '') {
		return;
	    }
	    var sort_dir = sort_directions[current_sort];
	    $scope.pluginMetadata.sort(function(first, second) {
		if (first[current_sort] < second[current_sort]) {
		    return sort_dir;
		} else if (first[current_sort] > second[current_sort]) {
		    return -1 * sort_dir;
		} else {
		    return 0;
		}
	    });
	}
    }
]);
