var services = angular.module('databaseManagerServices', []).factory('DatabaseConfigService', function($resource){
    return $resource('/Marti/api/inputs/config', {}, {
            'query': {method: "GET", isArray: false},
            'update': {method: "PUT"}
        }
    );
});


services.service('MetricsService', function($resource) {
    this.getDatabaseMetrics = function() {
        return $resource('/actuator/takserver-database');
    };
});

services.service('DatabaseMetricsService', function($resource) {
    return $resource('/Marti/api/database/cotCount');
})