var services = angular.module('DataRetentionServices', []);


services.factory('RetentionPolicyService', function($resource) {
    return $resource('/Marti/api/retention/policy', {}, {
        'query': {method: "GET", isArray: false},
        'update': {method: "PUT"}
    })
});

services.factory('RetentionScheduleService', function($resource) {
    return $resource('/Marti/api/retention/service/schedule', {}, {
        'query': {method: "GET", isArray: false},
        'update': {method: "PUT"}
    })
})

services.factory('MissionArchiveService', function($resource) {
    return $resource('/Marti/api/retention/missionarchive', {}, {
        'query': {method: "GET", isArray: false}
    })
})

services.factory('MissionRestoreService', function($resource) {
    return $resource('/Marti/api/retention/restoremission', {}, {
        'query': {method: "GET", isArray: false},
        'save': {method: "POST"},
        'update': {method: "PUT"}
    })
})

services.factory('MissionArchiveConfigService', function($resource) {
    return $resource('/Marti/api/retention/missionarchiveconfig', {}, {
        'query': {method: "GET", isArray: false},
        'update': {method: "PUT"}
    })
})