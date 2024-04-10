var services = angular.module('OAuth2ManagerServices', [])

    .factory('TokenServices', function($resource) {
        return {
            tokens: $resource('/Marti/api/token/:token', {token: '@_token'}, {
                'query': {method: "GET", isArray: false},
                'revoke': {method: "DELETE", isArray: false}
            }),

            revoke: $resource('/Marti/api/token/revoke/:tokens', {tokens: '@_tokens'}, {
                'revoke': {method: "DELETE", isArray: false}
            })
        };
    })

    .factory('ViewTokenService', function($resource) {
        return {
            tokens: $resource('/Marti/api/token/:token', {token: '@_token'}, {
                'query': {method: "GET", isArray: false},
                'revoke': {method: "DELETE", isArray: false}
            })
        };
    })
