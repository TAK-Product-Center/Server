var services = angular.module('loginManagerServices', [])

    .factory('loginService', function($resource) {
        return {
            config: $resource('/login/authserver', {}, {
                'get': {method: "GET", isArray: false },
            })
        };
    });
