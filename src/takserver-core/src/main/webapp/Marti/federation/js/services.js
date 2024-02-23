var services = angular.module('federationManagerServices', []).factory('FederatesService', function($resource) {
  return $resource('/Marti/api/federates/:id', { id: '@_id' }, {
	  		'query': {method: "GET", isArray: false}
  		}
  );
});

services.factory('FederateGroupsService', function($resource) {
	  return $resource('/Marti/api/federategroups/:federateId', {}, {
		  		'query': {method: "GET", isArray: false}
	  		}
	  );
	});

services.factory('FederateGroupsMapService', function($resource) {
           return $resource('/Marti/api/federategroupsmap/:federateId', {}, {
              		'query': {method: "GET", isArray: false}
           		}
       	  );
       	});

services.factory('FederateGroupConfigurationService', function($resource) {
	  return $resource('/Marti/api/federategroupconfig', {}, {});
	});

services.factory('OutgoingConnectionsService', function($resource) {
	  return $resource('/Marti/api/outgoingconnections/:name', {}, {
		  'query': {method: "GET", isArray: false},
		  'update': {method: "PUT"}
	  });
	});

services.factory('OutgoingConnectionStatusService', function($resource) {
	  return $resource('/Marti/api/outgoingconnectionstatus/:name?newStatus=:status', {name:'@name', status: '@status'}, {});
	});

services.factory('ActiveConnectionsService', function($resource) {
	  return $resource('/Marti/api/activeconnections', {}, {
		  'query': {method: "GET", isArray: false}
	  });
	});

services.factory('FederateContactsService', function($resource) {
	  return $resource('/Marti/api/federatecontacts/:federateId', {}, {
		  'query': {method: "GET", isArray: false}
	  });
	});

services.factory('GroupSearchService', function($resource) {
	  return $resource('/Marti/api/groups', {}, {
		  'query': {method: "GET", isArray: false}
	  });
	});

services.factory('GroupPrefixLookupService', function($resource) {
	  return $resource('/Marti/api/groupprefix', {}, {});
	});

services.factory('FederateDetailsService', function($resource) {
	  return $resource('/Marti/api/federatedetails/:federateId', {}, {
	      'query': {method: "GET", isArray: false},
	      'remove': {method: "DELETE"},
	      'update': {method: "PUT"}
	  });
	});

services.factory('FederationConfigService', function($resource){
      return $resource('/Marti/api/federationconfig', {}, {
        'query': {method: "GET", isArray: false},
        'update': {method: "PUT"}
      });
});

services.service('MetricsService', function($resource, $http) {
    this.getDatabaseMetrics = function() {
        return $resource('/actuator/takserver-database');
    };
});

services.factory('FederateGroupsMapAddService', function($resource) {
           return $resource('/Marti/api/federategroupsmap/:federateId', {federateId: '@federateId', remoteGroup: '@remoteGroup', localGroup: '@localGroup'}, {
            	'add': {method: "POST", isArray: false}
           });
});

services.factory('FederateGroupsMapRemoveService', function($resource) {
           return $resource('/Marti/api/federategroupsmap/:federateId', {federateId: '@federateId', remoteGroup: '@remoteGroup', localGroup: '@localGroup'}, {
            	'remove': {method: "DELETE", isArray: false}
           });
});

services.factory('FederateRemoteGroupsService', function($resource) {
	  return $resource('/Marti/api/federateremotegroups/:federateId', {}, {
	      'query': {method: "GET", isArray: false}
	  });
});

services.factory('FederationClearEventsService', function($resource) {
    return $resource('/Marti/api/clearFederationEvents/', {}, {
	'clear': {method: "GET", isArray: false}
    })
});

services.factory('MissionListService', function($resource) {
    return $resource('/Marti/api/missions', {passwordProtected: 'true', defaultRole: 'true'}, {
	'query': {method: "GET", isArray: false}
    })
});

// services.factory('FederateMissionsUpdateService', function($resource) {
// 	return $resource('/Marti/api/federatemissions/:federateId', {}, {
// 		'update': {method: "PUT", isArray: true}
// 	});
// });

