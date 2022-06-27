var services = angular.module('securityConfigServices', []);

services.factory('securityConfigService', function($resource){
    return $resource('/Marti/api/security/config', {}, {
            'query': {method: "GET", isArray: false},
            'update': {method: "PUT"}
        }
    );
});

services.factory('authConfigService', function($resource){
    return $resource('/Marti/api/authentication/config', {}, {
            'query': {method: "GET", isArray: false},
            'update': {method: "PUT"},
            'test' : {method: "POST"}
        }
    );
});
