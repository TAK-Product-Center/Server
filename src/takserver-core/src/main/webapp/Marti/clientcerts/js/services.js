var services = angular.module('clientCertificateManagerServices', [])

    .factory('ClientCertificatesService', function($resource) {
        return {
            certs: $resource('/Marti/api/certadmin/cert/:hash', {hash: '@_hash'}, {
                'query': {method: "GET", isArray: false},
                'revoke': {method: "DELETE", isArray: false}
            }),

            download: $resource('/Marti/api/certadmin/cert/download/:ids', {ids: '@_ids'}, {
                'query': {method: "GET", isArray: false},
            }),

            delete: $resource('/Marti/api/certadmin/cert/delete/:ids', {ids: '@_ids'}, {
                'delete': {method: "DELETE", isArray: false},
            }),

            revoke: $resource('/Marti/api/certadmin/cert/revoke/:ids', {ids: '@_ids'}, {
                'revoke': {method: "DELETE", isArray: false},
            }),

            active: $resource('/Marti/api/certadmin/cert/active/', {},  {
                'query': {method: "GET", isArray: false},
            }),

            replaced: $resource('/Marti/api/certadmin/cert/replaced/', {},  {
                'query': {method: "GET", isArray: false},
            }),

            expired: $resource('/Marti/api/certadmin/cert/expired/', {},  {
                'query': {method: "GET", isArray: false},
            }),

            revoked: $resource('/Marti/api/certadmin/cert/revoked/', {},  {
                'query': {method: "GET", isArray: false},
            })
        };
    })

    .factory('ViewCertificateService', function($resource) {
        return $resource('/Marti/api/certadmin/cert/:hash', { hash: '@_hash' }, {
                'query': {method: "GET", isArray: false}
            }
        );
    });

