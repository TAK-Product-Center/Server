'use strict';

var clientDashboardControllers = angular.module('clientDashboardControllers', []);

// User List Controller
clientDashboardControllers.controller('ClientDashboardController', ['$rootScope', '$scope', '$window','$http', '$timeout', '$uibModal', function ($rootScope, $scope, $window, $http, $timeout, $uibModal) {

  $scope.isAdmin = false;
  $scope.hasAdminRole = false;
  $scope.dbIsConnected = true;
  $scope.selectedPage = 1;
  $scope.clientsPerPage = 50;
  $scope.maxConnections = 0;
  $scope.actualNum = 0;

  $scope.visualizeConnection = function(uid){
    $window.location.href = '/Marti/metrics/index.html#!/connection/' + uid;
  }
	$scope.deleteSubscription = function(sub) {
	    $http.delete('/Marti/api/subscriptions/delete/' + sub.subscriptionUid)
	    .then(function(response){
	        $rootScope.alerts.push({msg: 'Subscription ' + sub.username + ' ' + sub.callsign + ' deleted'});
	    })
	    .catch(function(response){
	        $rootScope.alerts.push({msg: 'Subscription failed to delete', type: 'danger'});
	    });
	  };

    $scope.toggleIncognito = function(sub) {
        $http.post('/Marti/api/subscriptions/incognito/' + sub.subscriptionUid)
            .then(function(response){
            })
            .catch(function(response){
                $rootScope.alerts.push({msg: 'Subscription failed to toggle incognito!', type: 'danger'});
            });
    };

    (function setAdmin() {
	  $http.get('/Marti/api/util/isAdmin').then(function(response){
		
    	  $scope.hasAdminRole = response.data;
    	   console.log($scope.hasAdminRole)
      })
    })();

  (function pollDB() {
      $http.get('/actuator/takserver-database')
      .then(function(response){
          $scope.dbIsConnected = response.data.apiConnected && response.data.messagingConnected;
          $scope.maxConnections = response.data.maxConnections;
      })
      .finally(function(){
        $timeout(pollDB, 5000); //wait for request to return (either error or success) before waiting for poll interval
      });
    })();
    
    (function getDbConfig() {
        $http.get('/Marti/api/inputs/config').then(
            function(response) {
                console.log(response.data);
                console.log(response);
                if (response.data.data.connectionPoolAutoSize) {
                    $scope.actualNum = response.data.data.numAutoDbConnections;
                } else {
                    $scope.actualNum = response.data.data.numDbConnections;
                }
            }
        )
    })();
      
	  // poll for active subscriptions
	(function poll() {
      $http.get('/actuator/custom-network-metrics')
      .then(function(response){
        $scope.numClients = response.data['numClients']
        
        if (!$scope.clientsPerPage) {
          $scope.clientsPerPage = 50
        }

        let clientsPerPage = $scope.clientsPerPage ? $scope.clientsPerPage : 50

        $scope.maxPage = Math.max(1,Math.ceil($scope.numClients/clientsPerPage))

        if ($scope.selectedPage > $scope.maxPage || !$scope.selectedPage) {
          $scope.selectedPage = $scope.maxPage
        }
      })

      let page = $scope.selectedPage ? $scope.selectedPage : 1
      let clientsPerPage = $scope.clientsPerPage ? $scope.clientsPerPage : 50

		  $http.get('/Marti/api/subscriptions/all?page=' + page + '&limit=' + $scope.clientsPerPage)
		  .then(function(response){
		    $scope.subs = response.data.data;
		  })
		  .catch(function(response){
		    $scope.subs = [];
		  })
		  .finally(function(){
		    $timeout(poll, 2000); //wait for request to return (either error or success) before waiting for poll interval
		  });
	  })();

	  // Alerts / notifications
	  $rootScope.alerts = [];
	  
	  $scope.closeAlert = function(index) {
		  $rootScope.alerts.splice(index, 1);
	  };
	  
	  $scope.getHealthIcon = function(diffMs) {
		  
		  // 10 seconds
		  if (diffMs < 10 * 1000) {
			  return makeIconUrl('current');
		  }
		  
		  // 3 minutes
		  if (diffMs < 3 * 60 * 1000) {
			  return makeIconUrl('stale');
		  }
		  
		  return makeIconUrl('dead');
	  }
	  
	  function makeIconUrl(state) {
		  return '../images/' + state + '_presence.png';
	  }

      //Object to store column preferences
      $rootScope.columnChecks = {};
      //Used to get column preferences (if they exist)
      function loadPreferences(){
          var strColPrefs = localStorage.getItem('colPrefs');
          console.log(strColPrefs);
          if(strColPrefs != null){
             $rootScope.columnChecks = JSON.parse(strColPrefs);
          }
          else{
        	  setDefaultColPrefs();
          }
      }
      //Saves column preferences to localStorage
      function savePreferences(){
    	  //Need to check state of columnChecks Object, if it hasn't been initialized for some reason we need to initialize it before saving
    	  var testProp = 'health'; //Check for a known property that will be on the object if it was initialized correctly
    	  if(!$rootScope.columnChecks.hasOwnProperty(testProp)){
    		  setDefaultColPrefs();
    	  }
          var strColPrefs = JSON.stringify($rootScope.columnChecks);
          localStorage.setItem('colPrefs', strColPrefs);
      }

      //Initializes the column prefs objects to show all columns by default
      function setDefaultColPrefs(){
             $rootScope.columnChecks.health = true;
             $rootScope.columnChecks.callsign = true;
             $rootScope.columnChecks.username = true;
             $rootScope.columnChecks.dn = true;
             $rootScope.columnChecks.groups = true;
             $rootScope.columnChecks.lastReportMS = true;
             $rootScope.columnChecks.takClient = true;
             $rootScope.columnChecks.takVersion = true;
             $rootScope.columnChecks.role = true;
             $rootScope.columnChecks.team = true;
             $rootScope.columnChecks.ip = true;
             $rootScope.columnChecks.pendingWrites = true;
             $rootScope.columnChecks.numProcessed = true;
             $rootScope.columnChecks.protocol = true;
             $rootScope.columnChecks.xpath = true;
             $rootScope.columnChecks.uid = true;
             $rootScope.columnChecks.subscription = true;
             $rootScope.columnChecks.appFramerate = false;
             $rootScope.columnChecks.battery = false;
             $rootScope.columnChecks.batteryStatus = false;
             $rootScope.columnChecks.batteryTemp = false;
             $rootScope.columnChecks.deviceDataRx = false;
             $rootScope.columnChecks.deviceDataTx = false;
             $rootScope.columnChecks.heapCurrentSize = false;
             $rootScope.columnChecks.heapFreeSize = false;
             $rootScope.columnChecks.heapMaxSize = false;
             $rootScope.columnChecks.deviceIPAddress = false;
             $rootScope.columnChecks.storageAvailable = false;
             $rootScope.columnChecks.storageTotal = false;
             $rootScope.columnChecks.handlerType = false;
      }
      $scope.open = function(){
        var modalInstance = $uibModal.open({
          animation: false,
          templateUrl: 'checkboxModal.html',
          controller: 'ModalInstanceCtrl',
          controllerAs: '$ctrl',
          size: 'sm',
          scope: $scope
        });
        //Stub functions to handle success/failure of modal promise, mainly here to suppress unhandled rejection errors
        modalInstance.result.then(function() {}, function () {});
      }

    $scope.toggleMenu = function (sub) {
        var dropdown = document.getElementById(sub.subscriptionUid);

        // if we're going to show a div, close make sure everything else is closed
        if (!dropdown.classList.contains("show")) {
            $scope.hideAllDropDowns();
        }

        dropdown.classList.toggle("show");
    }

    $scope.hideAllDropDowns = function () {
        var dropdowns = document.getElementsByClassName("dropdown-content");
        var i;
        for (i = 0; i < dropdowns.length; i++) {
            var openDropdown = dropdowns[i];
            if (openDropdown.classList.contains('show')) {
                openDropdown.classList.remove('show');
            }
        }
    }

    window.onclick = function(event) {
        if (!event.target.matches('.dropbtn')) {
            $scope.hideAllDropDowns();
        }
    }

    //Handle saving and loading of column preferences between sessions
 
    $(window).ready(loadPreferences)
    $(window).on('beforeunload', savePreferences)
}]);

clientDashboardControllers.controller('ModalInstanceCtrl', function ($uibModalInstance) {
  var $ctrl = this;

  $ctrl.ok = function () {
    $uibModalInstance.close();
  };
});

clientDashboardControllers.controller('AddStaticSubscriptionCtrl', ['$rootScope', '$scope', '$http', '$uibModal', function ($rootScope, $scope, $http, $uibModal){

	(function setAdmin() {
		  $http.get('/Marti/api/util/isAdmin').then(function(response){
	    	  $scope.hasAdminRole = response.data;
	      })
      })();
	
    $scope.staticSub = {};

    function resetStaticSub(){
      $scope.staticSub.uid = '';
      $scope.staticSub.protocol = '';
      $scope.staticSub.subaddr = '';
      $scope.staticSub.subport = '';
      $scope.staticSub.xpath = '';
      $scope.staticSub.filterGroups = '';
      $scope.staticSub.iface = '';
    }

    $scope.addSubscription = function(data) {
        $http.post('/Marti/api/subscriptions/add', JSON.stringify(data), {headers: {'Content-Type': 'application/json'}})
        .then(function(response){
          $rootScope.alerts.push({msg: 'Subscription ' + data.uid + ' added', type: 'success'});
        })
        .catch(function(response){
          console.log(response);
          $rootScope.alerts.push({msg: 'Subscription failed to add', type: 'danger'});
        });
    };

    $scope.open = function(){
      resetStaticSub();
     
      var modalInstance = $uibModal.open({
        animation: false,
        templateUrl: 'subFormModal.html',
        controller: 'formModalInstanceCtrl',
        controllerAs: '$ctrl',
        scope: $scope
      });
      modalInstance.result.then(function() {}, function() {});
    }
}]);

clientDashboardControllers.controller('formModalInstanceCtrl', function($uibModalInstance, $scope){
    var $ctrl = this;

    $ctrl.submit = function(){
        $scope.addSubscription($scope.staticSub);
        $uibModalInstance.close();
    }
});
