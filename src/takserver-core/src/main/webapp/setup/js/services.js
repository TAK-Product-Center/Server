var setupServices = angular.module('setupServices', [])

setupServices.factory('securityVerifyConfigService', function($resource){
    return $resource('/Marti/api/security/verifyConfig', {}, {
        'query': {method: "GET", isArray: false}
    });
});

setupServices.factory('securityConfigService', function($resource){
    return $resource('/Marti/api/security/config', {}, {
        'query': {method: "GET", isArray: false},
        'update': {method: "PUT"}
    }
    );
});

setupServices.factory('authConfigService', function($resource){
    return $resource('/Marti/api/authentication/config', {}, {
        'query': {method: "GET", isArray: false},
        'update': {method: "PUT"},
        'test' : {method: "POST"}
    }
    );
});

setupServices.factory('FederationConfigService', function($resource){
    return $resource('/Marti/api/federationconfig', {}, {
      'query': {method: "GET", isArray: false},
      'update': {method: "PUT"}
    });
});

setupServices.factory('FederationVerifyConfigService', function($resource){
    return $resource('/Marti/api/federationconfig/verify', {}, {
        'query': {method: "GET", isArray: false}
    });
});

setupServices.factory('InputManagerService', function($resource) {
    return $resource('/Marti/api/inputs/:id', { id: '@_id' }, {
        'query': {method: "GET", isArray: false},
        'update': {method: "PUT", params: {id: '@id'}, isArray: false, cache: false}
    }
    );
});

setupServices.factory('MessagingConfigService', function($resource){
    return $resource('/Marti/api/inputs/config', {}, {
        'query': {method: "GET", isArray: false},
        'update': {method: "PUT"}
    }
    );
});

setupServices.factory('isSecureService', function($resource) {
    return $resource('/Marti/api/security/isSecure', {}, {
        'query': {method: "GET", isArray: false}
    });
});