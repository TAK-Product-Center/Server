(function(){
  var app = angular.module('repeaterUI', []);
  app.directive('onFinishRender', function ($timeout) {
	    return {
	        restrict: 'A',
	        link: function (scope, element, attr) {
	            if (scope.$last === true) {
	                $timeout(function () {
	                    scope.$emit('ngRepeatFinished');
	                });
	            }
	        }
	    }
  }
  );
	    
  app.controller("repeatablesList", ['$scope', '$http', '$interval',
	function($scope, $http, $interval) {
	  var self = this;
	  self.repeatables = [];
	  
	  $scope.showCoT = true;
	  
	  // ** we need to wait until our ng-repeat finishes before calling prettyPrint() which colorizes the CoT XML
	  $scope.$on('ngRepeatFinished', function(ngRepeatFinishedEvent) {
	  		prettyPrint();
		});
	  
	  $http.get('../api/repeater/period').then(
			  function(data, status, headers, config) {
				  $scope.period = data.data.data;
			  },
			  function(data, status, headers, config) {
				  $.jnotify("Unable to load period data from server. Please contact administrator.", "error");
			  });
	  
	  $scope.updatePeriod = function() {
		  $http.post('../api/repeater/period', $scope.period).then(
				  function(data, status, headers, config) {
					  $.jnotify("New period set")
				  },
				  function(data, status, headers, config) {
					  $.jnotify("Unable to set the period. Please contact administrator.", "error");
				  });
	  }
	  
	  $scope.setMessage = function(messageText, messageType) {
		  $scope.message = messageText;
		  
		  if(messageType != undefined && messageType == "error") {
			  $scope.messageColor = "red";
		  } else {
			  $scope.messageColor = "black";
		  }
	  };
	  
	  $scope.clearMessage = function() {
		  $scope.message = "";;
		  $scope.messageColor = "black";
	  };
	  
	  $scope.retrieveList = function() {
		  $http.get('../api/repeater/list').then(
				  function(response, status, headers, config) {
					  // ** here we overwrite our existing data with the new data, but not until saving off the
					  // ** state of the checkboxes so that we can repopulate
					  var data = response.data;
					  var checkboxMap = {};
					  var isDifferent = false;
					  for(var i = 0; i < self.repeatables.length; i++) {
						  checkboxMap[self.repeatables[i].callsign] = document.getElementById("repeatable_" + i).checked;
						  
						  // ** check if anything is present in old list, but not in new list
						  var found = false;
						  for(j = 0; j < data.data.length; j++) {
							  if(self.repeatables[i].callsign == data.data[j].callsign) {
								  found = true
							  }
						  }
						  if(!found) {
							  isDifferent = true;
						  }
					  }
					  
					  for(var i = 0; i <data.data.length; i++) {
						  if(checkboxMap[data.data[i].callsign] == true) {
							  data.data[i].checked = true;
						  }
						  
						  data.data[i].xml = vkbeautify.xml(data.data[i].xml);
						  
						  data.data[i].formattedDate = new Date(data.data[i].dateTimeActivated).toString();
						  
						  var found = false;
						  for(j = 0; j < self.repeatables.length; j++) {
							  if(data.data[i].callsign == self.repeatables[j].callsign) {
								  found = true
							  }
						  }
						  if(!found) {
							  isDifferent = true;
						  }
					  }
					  
					  if(isDifferent) {
						  self.repeatables = data.data;
					  }
					  
					  if(self.repeatables.length == 0) {
						  $scope.setMessage("There are currently no alarm messages.");
					  } else {
						  $scope.clearMessage();
					  }
				  },
				  function(data, status, headers, config) {
				    $scope.setMessage("Unable to load list of repeating messages from the server. Please contact administrator.", "error");
				  });
	  };
	  
	  $scope.deleteSelectedRepeatables = function() {
		  var count = self.repeatables.length;
		  for(var i = 0; i < count; i++) {
			  if(document.getElementById("repeatable_" + i).checked) {
				  var uid = document.getElementById("repeatable_" + i).dataset.uid;
				  $http.get('../api/repeater/remove/' + uid).then(
						  function(data, status, headers, config) {
							  //$scope.retrieveList();
						  },
						  function(data, status, headers, config) {
							  $.jnotify("Unable to delete " + uid + ". Please contact administrator.", "error");
						  });
			  }
		  }
		  
	  }
	  
	  $scope.retrieveList();
	  
	  $interval($scope.retrieveList, 1000);
	}]
  );
})();
