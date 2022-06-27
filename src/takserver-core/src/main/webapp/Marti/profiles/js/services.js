var services = angular.module('deviceProfileManagerServices', [])

    .factory('DeviceProfilesService', function($resource) {
        return {
            profiles: $resource('/Marti/api/device/profile', {},{
                'get': {method: "GET", isArray: false},
            }),

            addProfile: $resource('/Marti/api/device/profile/:name?group=__ANON__', {name: '@name'}, {
                'add': {method: "POST", isArray: false},
            }),

            deleteProfile: $resource('/Marti/api/device/profile/:id', {id: '@id'}, {
                'delete': {method: "DELETE", isArray: false},
            }),

            updateProfile: $resource('/Marti/api/device/profile/:name?', {name: '@name'}, {
                'update': {method: "PUT", isArray: false},
            })
        };
    })

    .factory('EditProfileService', function($resource) {
        return {
            updateProfile: $resource('/Marti/api/device/profile/:name?', {name: '@name'}, {
                'update': {method: "PUT", isArray: false},
            }),

            profile: $resource('/Marti/api/device/profile/:name', {name: '@name'}, {
                'get': {method: "GET", isArray: false},
            }),

            files: $resource('/Marti/api/device/profile/:name/files', {name: '@name'}, {
                'get': {method: "GET", isArray: false},
            }),

            uploadFile: $resource('/Marti/api/device/profile/:name/file?filename=:filename',
                {name: '@name', filename : '@filename' }, {
                    'add': {
                        method: "PUT",
                        isArray: false,
                        headers: {'Content-Type': 'application/octet-stream'},
                        transformRequest: []
                    },
                }),

            deleteFile: $resource('/Marti/api/device/profile/:name/file/:id',
                {name: '@name', id : '@id' }, {
                    'delete': {
                        method: "DELETE",
                        isArray: false
                    },
                }),

            groups: $resource('/Marti/api/groups/all', {}, {
                'get': {method: "GET", isArray: false},
            }),

            validDirectories: $resource('/Marti/api/device/profile/directories', {}, {
                'get': {method: "GET", isArray: false},
            }),

            updateSelectedDirectories: $resource('/Marti/api/device/profile/:name/directories/:directories',
                {name: '@name', directories : '@directories'}, {
                    'update': {method: "PUT", isArray: false},
            }),

            directories: $resource('/Marti/api/device/profile/:name/directories', {name: '@name'}, {
                'get': {method: "GET", isArray: false},
                'delete': {method: "DELETE", isArray: false},
            })
        };
    })

    .factory('SendProfileService', function($resource) {
        return {
            profile: $resource('/Marti/api/device/profile/:name', {name: '@name'}, {
                'get': {method: "GET", isArray: false},
            }),

            clientEndPoints: $resource('/Marti/api/clientEndPoints?showCurrentlyConnectedClients=true', { }, {
                'get': {method: "GET", isArray: false},
            }),

            sendProfile: $resource('/Marti/api/device/profile/:name/send', {name: '@name'}, {
                'send': {method: "POST", isArray: false},
            })
        };
    });

