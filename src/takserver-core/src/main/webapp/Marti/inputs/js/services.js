var services = angular.module('inputManagerServices', []).factory('InputManagerService', function($resource) {
  return $resource('/Marti/api/inputs/:id', { id: '@_id' }, {
	  		'query': {method: "GET", isArray: false},
	  		'update': {method: "PUT", params: {id: '@id'}, isArray: false, cache: false}
  		}
  );
});

services.factory('MessagingConfigService', function($resource){
    return $resource('/Marti/api/inputs/config', {}, {
            'query': {method: "GET", isArray: false},
            'update': {method: "PUT"}
        }
    );
});

services.factory('securityConfigService', function($resource){
    return $resource('/Marti/api/security/config', {}, {
            'query': {method: "GET", isArray: false},
            'update': {method: "PUT"}
        }
    );
});

services.service('MetricsService', function($resource, $http) {
    this.getDatabaseMetrics = function() {
        return $resource('/actuator/takserver-database');
    };
});

services.factory('RateRuleService', function($resource) {
    return $resource('/Marti/api/qos/ratelimit/rules', {}, {
            'query': {method: "GET", isArray: false} // Rate rules are currently read-only
        }
    );
});

services.service('QosService', function($resource, $http) {
    this.enableDelivery = function(enable) {
        return $resource('/Marti/api/qos/delivery/enable', {}, {
            'update': {method: "PUT"}
        })
    };
    this.enableRead = function(enable) {
        return $resource('/Marti/api/qos/read/enable', {}, {
            'update': {method: "PUT"}
        })
    };
    this.enableDOS = function() {
        return $resource('/Marti/api/qos/dos/enable', {}, {
            'update': {method: "PUT"}
        })
    };
    this.getQosConf = function() {
        return $resource('/Marti/api/qos/conf');
    };
    this.getActiveDeliveryRateLimit = function() {
        return $resource('/Marti/api/qos/ratelimit/delivery/active');
    };
    this.getActiveReadRateLimit = function() {
        return $resource('/Marti/api/qos/ratelimit/read/active');
    };
    this.getActiveDOSRateLimit = function() {
        return $resource('/Marti/api/qos/ratelimit/dos/active');
    };
});



