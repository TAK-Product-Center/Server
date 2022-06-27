'use strict';

var setupControllers = angular.module('setupControllers', []);

setupControllers.controller('SecurityCtrl', [
    '$rootScope',
    '$scope',
    '$timeout',
    '$state',
    'securityConfigService',
    'authConfigService',
    'isSecureService',
    'InputManagerService',
    function($rootScope,
            $scope,
            $timeout,
            $state,
            securityConfigService,
            authConfigService,
            isSecureService,
            InputManagerService) {

        $scope.setUpSecure = function(showSafe) {
            isSecureService.query(function(response) {
                var d = new Date();
                var expires = "expires="+d.toUTCString();
                document.cookie =  "portsSecure=false;" + expires + ";path=/";
                if (response.data != "true") {
                    console.log(response);
                    console.log("showing alerts?");
                    $('#securityAlert').show();
                    $('#securityAlert span').html("<strong>ALERT!</strong> The following ports support unsecure connections: " + response.data);
                }
            });
        }

        $scope.showSecureInput = false;
        $scope.unsafeHttpConnectors = false;
        $scope.inputNames = [];
        $scope.showSecureConfig = function() {
            var d = new Date();
            var expires = "expires="+d.toUTCString();
            document.cookie = "portsSecure=false;" + expires + ";path=/";
            $scope.showSecureInput = true;
            var tempList = [];
            isSecureService.query(function(response) {
                if (response.data == "true") {
                    return;
                } else {
                    console.log("showing alerts?");
                    $('#securityAlert').show();
                    $('#securityAlert span').html("<strong>ALERT!</strong> The following ports support unsecure connections: " + response.data);
                    tempList = JSON.parse(response.data);
                    console.log(JSON.parse(response.data));
                }
            });
            console.log(tempList);
            InputManagerService.query(function(response) {
                var inputPorts = [];
                var x;
                for (x of response.data) {
                    console.log(x);
                    $scope.inputNames.push(x.input.name);
                    inputPorts.push(x.input.port);
                }
                if (tempList.length > 0) {
                    $scope.unsafeHttpConnectors = [];
                }
                for (x of tempList) {
                    if (!inputPorts.includes(x)) {
                        $scope.unsafeHttpConnectors.push(x);
                    }
                }
                if ($scope.unsafeHttpConnectors === false) {
                    $rootScope.update_progress("security", "unsafeHttpConnectors");
                }
            });
        }

        $scope.setSecureInput = function() {
            console.log("doing setSEcureInput");
            $scope.setUpSecure(false);

            var inputName;
            for (inputName of $scope.inputNames) {
                console.log("deleting: "+inputName);
                var ims = new InputManagerService();
                ims.$delete({id : inputName},
                        function(data) {
                            console.log("success" + data);
                        },
                        function(data) {
                            alert('An error occurred setting up your secure input configuration (delete unsafe inputs failed) ' + data);
                        }
                );
            }
            var safeInput = new InputManagerService();
            safeInput.name = 'stdssl';
            safeInput.protocol = 'tls';
            safeInput.port = 8089;
            InputManagerService.save(safeInput, function(response) {
                console.log("success");
                $rootScope.update_progress("security", "setSecureInput");
            }, function(response) {
                alert("an error occured saving the secure input");
            });
        }

        $scope.noLDAP = false;

        $scope.hideSkip = false;
        $scope.hideSkipFunc = function() {
            $scope.hideSkip = true;
        }

        function getSecConfig() {
            securityConfigService.query(function(response) {
                $scope.secConfig = response.data
            }, function(response) {
                $scope.secConfig = null
            });
        }

        $scope.saveSecConfig = function(config){
            securityConfigService.update(config,
                    function(response){},
                    function(response){alert(response.data.data)}
                    );
        }

        function getAuthConfig() {
            authConfigService.query(function(response) {
                if (response.data != null) {
                    $scope.authConfig = response.data;
                } else {
                    $scope.authConfig = null;
                    $scope.noLDAP = true;
                }
            }, function(response) {
                $scope.authConfig = null
            });
        }

        $scope.saveAuthConfig = function(config){
            authConfigService.update(config,
                    function(response){
                        console.log("saving auth? " + response.data.data);
                        $rootScope.update_progress("security", "ldap");
                        $state.transitionTo("security.ldap.view");
                    },
                    function(response){alert(response.data.data)}
                    );

        }

        $scope.showTest = false;
        $scope.testPassed = false;
        $scope.testAuthConfig = function(){
            $scope.showTest = false;
            $scope.testPassed = false;

            authConfigService.test(
                function(response){
                    $scope.testPassed = true;
                    $scope.showTest = true;
                },
                function(response){
                    $scope.showTest = true;
                }
            );

        }

        $scope.$on('$viewContentLoaded', function(event) {
            getSecConfig();
            getAuthConfig();
        });
    }
]);

setupControllers.controller('CheckSecConfigCtrl', [
    '$scope',
    '$rootScope',
    'securityVerifyConfigService',
    'securityConfigService',
    function($scope, $rootScope, securityVerifyConfigService, securityConfigService) {
        securityConfigService.query(function(response) {
            $scope.secConfig = response.data
        }, function(response) {
            $scope.secConfig = null
        });
        
        $scope.clickedShowLDAP = function() {
            $rootScope.showLDAP = true;
        }

        securityVerifyConfigService.query(function(response) {
            $scope.configVerified = true;
            $rootScope.update_progress("security", "configCorrect");
        }, function(response) {
            $scope.configVerified = false;
            console.log(response);
        });
    }
]);

setupControllers.controller('FederationCtrl', [
    '$scope',
    '$rootScope',
    'FederationConfigService',
    'FederationVerifyConfigService',
    function ($scope, $rootScope, FederationConfigService, FederationVerifyConfigService) {
        $scope.showAdvanced = false;
        $scope.showTrust = false;

        $scope.fedConfig = new FederationConfigService();
        function getFedConfig() {
            FederationConfigService.query(
                    function (response) {
                        $scope.fedConfig = response.data;
                        $scope.fedConfig.enabled = false;
                        $scope.fedConfig.federatedGroupMapping = false;
                        $scope.fedConfig.automaticGroupMapping = false;
                        $scope.fedConfig.serverPortEnabled = false;
                        $scope.fedConfig.serverPortEnabledv2 = false;
                        console.log("webaseurl: " + $scope.fedConfig.webBaseURL);
                        $scope.webUrlAddress = $scope.fedConfig.webBaseURL.split("://")[1].split(":")[0];
                    },
                    function (response) {
                        $scope.fedConfig = null;
                    });
        }

        $scope.verifyFedConfig = function() {
            FederationVerifyConfigService.query(
                    function (response) {
                        $scope.configVerified = true;
                        $rootScope.update_progress("federation", "fedTruststore");
                    },
                    function (reponse) {
                        $scope.showTrust = true;
                        $scope.configVerified = false;
                    }
            )
        }
        $scope.$on('$viewContentLoaded', function (event) {
            getFedConfig();
            $scope.verifyFedConfig();
        });

        $scope.enableFederation = function(enableFed) {
            if (enableFed) {
                $(function() {
                    $("#enableFed").addClass("active");
                    $("#disableFed").removeClass("active");
                });
                $scope.configFinished = false;
                $rootScope.update_progress("federation", false);
                $scope.fedConfig.enabled = true;
                $scope.fedConfig.federatedGroupMapping = false;
                $scope.fedConfig.automaticGroupMapping = false;
                $scope.fedConfig.serverPortEnabledv2 = true;
                $scope.fedConfig.serverPortv2 = 9001;
                $scope.fedConfig.allowMissionFederation = true;
                $scope.fedConfig.allowFederatedDelete = false;

                $rootScope.update_progress("federation", "v2")
            } else {
                $(function() {
                    $("#disableFed").addClass("active");
                    $("#enableFed").removeClass("active");
                });
                $scope.fedConfig.enabled = false;
                $scope.fedConfig.federatedGroupMapping = false;
                $scope.fedConfig.automaticGroupMapping = false;
                $scope.fedConfig.serverPortEnabled = false;
                $scope.fedConfig.serverPortEnabledv2 = false;
                $scope.configFinished = true;
                $scope.showAdvanced = false;
                $scope.showSave = false;
                $rootScope.update_progress("federation", true);
                if ($scope.configVerified) {
                    $scope.showTrust = false;
                    $scope.saveFederationConfig();
                } else {
                    $scope.showTrust = true;
                }
                return;
            }
        }


        $scope.webUrlAddress = "";
        $scope.webUrlPort = 8443;

        $scope.enableV1Federation = function() {
            $(function() {
                $("#enableV1").addClass("active");
                $("#skipV1").removeClass("active");
            });
            $scope.fedConfig.serverPortEnabled = true;
            var v1port = new FederationConfigService()
            v1port.port = 9000;
            $scope.fedConfig.v1Ports = [v1port]
            $scope.showTrust = true;

            $rootScope.update_progress("federation", "v1")
        }

        $scope.skipV1Federation = function() {
            $(function() {
                $("#skipV1").addClass("active");
                $("#enableV1").removeClass("active");
            });
            $scope.fedConfig.serverPortEnabled = false;
            $scope.showTrust = true;
            $scope.configFinished = false;
            $rootScope.update_progress("federation", "v1")
        }

        $scope.skipTrust = function() {
            $scope.showAdvanced = true;
            $scope.showSave = true;
            $scope.configFinished = false;
        }

        $scope.saveFederationConfig = function () {
            $scope.submitInProgress = true;

            if ($scope.webUrlPort < 1 || $scope.webUrlPort > 65535) {
                alert("The port must be between 1 and 65535.");
                return;
            }

            $scope.fedConfig.webBaseURL = "https://" + $scope.webUrlAddress + ":" + $scope.webUrlPort + "/Marti";
            $rootScope.update_progress("federation", "webbase");


            FederationConfigService.update($scope.fedConfig,
                function (apiResponse) {
                    console.log(apiResponse.data.data);
                    $scope.configFinished = true;
                    $scope.verifyFedConfig();
                },
                function (apiResponse) {
                    alert(apiResponse.data.data);
                    $scope.configFinished = false;
                }
            );

        }

    }]);


setupControllers.controller('ProgressBarCtrl', [
    '$scope',
    '$rootScope',
    'isSecureService',
    function($scope, $rootScope, isSecureService) {

        isSecureService.query(function(response) {
            var d = new Date();
            var expires = "expires="+d.toUTCString();
            document.cookie =  "portsSecure=false;" + expires + ";path=/";
            if (response.data != "true") {
                console.log(response);
                console.log("showing alert?");
                $('#securityAlert').show();
                $('#securityAlert span').html("<strong>ALERT!</strong> The following ports support unsecure connections: " + response.data);
            }
        });

        $scope.progress = 0;
        $scope.progress_txt = $scope.progress.toString() + "%";
        $scope.progress_type = '';
        function setProgressType(prog) {
            if (prog < 33) {
                $scope.progress_type = 'danger';
            } else if (prog < 50) {
                $scope.progress_type = 'warning';
            } else if (prog < 67) {
                $scope.progress_type = 'info';
            } else if (prog < 80) {
                $scope.progress_type = 'primary';
            } else {
                $scope.progress_type = 'success';
            }
        }
        $scope.progress_categories = {};


        if (true === false) {//localStorage.getItem('progress_categories')) { /* we dont actually want to do this yet */
            console.log("getting from lcoal storage");
            $scope.progress_categories = JSON.parse(localStorage.getItem('progress_categories'));
            console.log($scope.progress_categories);
            console.log(localStorage.getItem('progress_categories'));
        } else {
            $scope.progress_categories = {
                    'security': {
                        'total': 4,
                        'complete': false,
                        'elements': [],
                        'expected_elements': [
                            'setSecureInput', 'unsafeHttpConnectors', 'configCorrect', 'ldap'
                            ]
                    },
                    'federation': {
                        'total': 4,
                        'complete': false,
                        'elements': [],
                        'expected_elements': [
                            'v2', 'v1', 'fedTruststore', 'webbase'
                            ]
                    }
            };
            //localStorage.setItem('progress_categories', JSON.stringify($scope.progress_categories));
        }

        $scope.update_progress_local = function(category, element) {
            console.log("category:" + category + ", element: "+element);
            console.log($scope.progress_categories)
            if (category in $scope.progress_categories &&
                    $scope.progress_categories[category]['expected_elements'].includes(element)) {

                if (!$scope.progress_categories[category]['elements'].includes(element)) {
                    $scope.progress_categories[category]['elements'].push(element);
                }
            } else if (element === true) {
                $scope.progress_categories[category]['complete'] = true;
            } else if (element === false) {
                $scope.progress_categories[category]['complete'] = false;
            }

            var temp_prog = 0;
            var denom = 0;
            for (var cat in $scope.progress_categories) {
                denom += $scope.progress_categories[cat]['total'];
                if ($scope.progress_categories[cat]['complete']) {
                    temp_prog += $scope.progress_categories[cat]['total'];
                } else {
                    temp_prog += $scope.progress_categories[cat]['elements'].length
                }
            }
            $scope.progress = 100 * (temp_prog / denom);
            $scope.progress_txt = $scope.progress.toString() + "%";
            setProgressType($scope.progress);
            //localStorage.setItem('progress_categories', JSON.stringify($scope.progress_categories));
        };
        $scope.update_progress_local("", ""); //this will update based on localStorage

        $rootScope.update_progress = function(category, element) {
            console.log("rootScope.updateProgress(" + category + ", " + element + ")");
            $scope.update_progress_local(category, element);
        };
    }
])
