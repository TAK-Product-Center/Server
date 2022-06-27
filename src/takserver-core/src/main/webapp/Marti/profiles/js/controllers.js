'use strict';

var profileManagerControllers = angular.module('deviceProfileManagerControllers', []);

profileManagerControllers.controller('deviceProfilesListCtrl', ['$scope', '$location', 'DeviceProfilesService', '$window',
    function ($scope, $location, DeviceProfilesService, $window) {

        $scope.showRmiError = false;
        $scope.setProfileNameLabel = 'Set Profile Name';

        $scope.getAll = function() {
            DeviceProfilesService.profiles.get(
                function(apiResponse) {$scope.deviceProfiles = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.addProfile = function() {

            if ($scope.profileName == $scope.setProfileNameLabel) {
                return;
            }

            var profileName = $scope.profileName;

            DeviceProfilesService.addProfile.add(
                { name : $scope.profileName },
                function(apiResponse) {
                    $window.location.href = '/Marti/profiles/index.html#!/editProfile/' + profileName;
                },
                function() {alert('An unexpected error occurred adding the profile.');});

            $scope.profileName = $scope.setProfileNameLabel;
        }

        $scope.deleteProfile = function(id) {

            if (!confirm("Press Ok to delete profile")) {
                return;
            }

            DeviceProfilesService.deleteProfile.delete(
                { id: id },
                function(apiResponse) { $scope.getAll(); },
                function() {$scope.showRmiError = true;});
        }

        $scope.updateProfile = function(profile) {
            DeviceProfilesService.updateProfile.update(
                { name : profile.name },
                profile,
                function(apiResponse) {},
                function() {alert('An unexpected error occurred updating the profile.');});
        }

        $scope.sortPropertyName = 'type';
        $scope.reverse = true;

        $scope.sortBy = function(sortPropertyName) {
            $scope.reverse = ($scope.sortPropertyName === sortPropertyName) ? !$scope.reverse : false;
            $scope.sortPropertyName = sortPropertyName;
        };

        $scope.profileName = $scope.setProfileNameLabel;
        $scope.getAll();
    }]);


profileManagerControllers.controller('editProfileCtrl', ['$scope', '$location', 'EditProfileService', '$routeParams',
    function ($scope, $location, EditProfileService, $routeParams) {

        $scope.showRmiError = false;
        $scope.selectAll = false;

        $scope.getProfile = function() {
            EditProfileService.profile.get(
                {name: $routeParams.name},
                function(apiResponse) {
                    $scope.profile = apiResponse.data;
                    angular.forEach($scope.profile.groups, function(key,value) {
                        $scope.groupStatus[key] = true;
                    });
                },
                function() {$scope.showRmiError = true;});
        }

        $scope.updateProfile = function(profile) {
            EditProfileService.updateProfile.update(
                { name : profile.name },
                profile,
                function(apiResponse) {},
                function() {alert('An unexpected error occurred updating the profile.');});
        }

        $scope.getFiles = function() {
            EditProfileService.files.get(
                {name: $routeParams.name},
                function(apiResponse) {$scope.files = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.uploadFile = function() {
            var f = document.getElementById('file').files[0];
            var r = new FileReader();
            r.onloadend = function(e) {
                var data = e.target.result;
                EditProfileService.uploadFile.add(
                    {name: $routeParams.name, filename: f.name },
                    data,
                    function(apiResponse) {$scope.files.push(apiResponse.data);},
                    function() {$scope.showRmiError = true;}
                );
            }

            r.readAsArrayBuffer(f);
        }

        $scope.deleteFile = function(id) {

            if (!confirm("Press Ok to delete file")) {
                return;
            }

            EditProfileService.deleteFile.delete(
                {name: $routeParams.name, id: id},
                function(apiResponse) { $scope.getFiles(); },
                function() {$scope.showRmiError = true;});
        }

        $scope.getGroups = function() {
            EditProfileService.groups.get(
                {},
                function(apiResponse) {$scope.groups = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.updateGroups = function() {
            var selectedGroups = [];
            angular.forEach($scope.groupStatus, function(key,value) {
                if (key) {
                    selectedGroups.push(value);
                }
            });

            $scope.profile.groups = selectedGroups;
            $scope.updateProfile($scope.profile);
        }

        $scope.toggle = function() {
            $scope.selectAll = !$scope.selectAll;

            angular.forEach($scope.groups, function(group) {
                $scope.groupStatus[group.name] = $scope.selectAll;
            });

            $scope.groupStatus['APPLY_TO_ALL_GROUPS'] = $scope.selectAll;

            this.updateGroups();
        }

        $scope.getValidDirectories = function() {
            EditProfileService.validDirectories.get(
                {},
                function(apiResponse) {$scope.validDirectories = apiResponse.data;},
                function() {$scope.showRmiError = true;}
            );
        }

        $scope.updateSelectedDirectories = function() {

            // clear out all current profile directories
            EditProfileService.directories.delete(
                {name: $routeParams.name},
                function(apiResponse) {},
                function() {$scope.showRmiError = true;}
            );

            var selectedValidDirectories = [];
            angular.forEach($scope.directoryStatus, function(key,value) {
                if (key) {
                    selectedValidDirectories.push(value);
                }
            });

            if (selectedValidDirectories.length > 0) {
                EditProfileService.updateSelectedDirectories.update(
                    {name: $routeParams.name, directories: selectedValidDirectories},
                    function (apiResponse) {
                        $scope.directories.push(apiResponse.data);
                    },
                    function () {
                        $scope.showRmiError = true;
                    }
                );
            }
        }

        $scope.getDirectories = function() {
            EditProfileService.directories.get(
                {name: $routeParams.name},
                function(apiResponse) {
                    $scope.directories = apiResponse.data;
                        angular.forEach($scope.directories, function(key, value) {
                            $scope.directoryStatus[key.path] = true;
                        });
                    },
                function() {$scope.showRmiError = true;}
            );
        }

        $scope.groupStatus = {};
        $scope.directoryStatus = {};
        $scope.getProfile();
        $scope.getFiles();
        $scope.getGroups();
        $scope.getValidDirectories();
        $scope.getDirectories();
    }]);

profileManagerControllers.controller('sendProfileCtrl', ['$scope', '$location', 'SendProfileService', '$routeParams', '$window',
    function ($scope, $location, SendProfileService, $routeParams, $window) {

        $scope.getClientEndPoints = function() {
            SendProfileService.clientEndPoints.get(
                { },
                function(apiResponse) {
                    $scope.clientEndPoints = apiResponse.data;
                },
                function() {$scope.showRmiError = true;});
        }

        $scope.toggle = function () {
            angular.forEach($scope.clientEndPoints, function(clientEndPoint) { clientEndPoint.checked = !clientEndPoint.checked; });
        }

        $scope.sendProfile = function() {
            var selected = [];

            angular.forEach($scope.clientEndPoints, function(clientEndPoint) {
                if (clientEndPoint.checked) {
                    selected.push(clientEndPoint.uid);
                }
            });

            SendProfileService.sendProfile.send(
                { name : $scope.profileName },
                selected,
                function(apiResponse) {
                    alert("Successfully sent profile");
                    $window.location.href = '/Marti/profiles/index.html#!';
                },
                function() {
                    alert("Error sending profile");
                });
        }

        $scope.showRmiError = false;
        $scope.clientEndPointSelected = {};
        $scope.profileName = $routeParams.name;
        $scope.getClientEndPoints();
    }]);
