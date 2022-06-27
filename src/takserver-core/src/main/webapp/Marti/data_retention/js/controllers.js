'use strict';

var dataRetentionControllers = angular.module('DataRetentionControllers', []);

const WEEK = 7;
const MONTH = 30.42; // average days in a month (365 / 12)
const YEAR = 365;


dataRetentionControllers.controller('ViewPoliciesCtrl', [
    '$scope',
    '$uibModal',
    '$window',
    'RetentionPolicyService',
    'RetentionScheduleService',
    function(
        $scope,
        $uibModal,
        $window,
        RetentionPolicyService,
        RetentionScheduleService
    ) {

        $scope.data_types = {
            "cot": "Cot (non-chat)",
            "geochat": "GeoChat (chat cot messages)",
            "missionpackages": "Mission Packages",
            "missions": "Missions (including mission structure, tracks, and files)",
            "files": "Files (all enterprise sync data that is not mission data)"
        }

        $scope.policy_map = {};
        $scope.edit_policy = {};

        for (let data_type of Object.keys($scope.data_types)) {
            $scope.policy_map[data_type] = {};
            $scope.edit_policy[data_type] = false;
        }

        (function() {
            RetentionPolicyService.query(
                function(apiResponse) {
                    var policy_map = apiResponse.data;
                    console.log(apiResponse);
                    for (let data_type of Object.keys(policy_map)) {
                        let ttl = policy_map[data_type];
                        if (ttl === null || ttl === undefined || ttl < 0) {
                            $scope.policy_map[data_type] = {
                                'ttl': null,
                                'ttl_type': "",
                                'ttl_number': null,
                                'ttl_display': "None"
                            }
                            continue;
                        }
                        let ttl_number = ttl / (24 * 3600);
                        let ttl_type = "";
                        if (ttl_number < 1) {
                            ttl_number = ttl / 3600;
                            ttl_type = "hours";
                        } else if (ttl_number < 7) {
                            ttl_type = "days";
                        } else if (ttl_number < MONTH) {
                            ttl_number = ttl / (WEEK * 24 * 3600);
                            ttl_type = "weeks";
                        } else if (ttl_number < YEAR) {
                            ttl_number = ttl / (MONTH * 24 * 3600);
                            ttl_type = "months";
                        } else {
                            ttl_number = ttl / (YEAR * 24 * 3600);
                            ttl_type = "years";
                        }
                        $scope.policy_map[data_type] = {
                            'ttl': ttl,
                            'ttl_type': ttl_type,
                            'ttl_number': ttl_number,
                            'ttl_display': ttl_number + " " + ttl_type
                        }
                    }
                }
            )
        })();

        $scope.viewMissionArchive = function() {
            $window.location.href = '/Marti/data_retention/index.html#!/mission-archive';
        }

        $scope.setPolicyNull = function(data_type) {
            if (!(data_type in $scope.policy_map)) {
                console.log("You cannot set a policy for that data type {" + data_type + "}");
                return;
            }
            let policy = $scope.policy_map[data_type];
            console.log(data_type);
            console.log(policy);
            $scope.policy_map[data_type] = {
                'ttl': null,
                'ttl_type': "",
                'ttl_number': null,
                'ttl_display': "None"
            }
        }

        $scope.savePolicy = function(data_type) {
            if (!(data_type in $scope.policy_map)) {
                console.log(data_type);
                return;
            }
            let policy = $scope.policy_map[data_type];
            console.log(data_type);
            console.log(policy);
            if (policy.ttl_number < 0 || policy.ttl_number === undefined) {
                console.log("Cant have a negative time to live!");
                alert("You can't set the policy to have a negative time to live!");
                return;
            }
            if (policy.ttl_type === "hours") {
                policy.ttl = policy.ttl_number * 3600;
            } else if (policy.ttl_type === "days") {
                policy.ttl = policy.ttl_number * 24 * 3600;
            } else if (policy.ttl_type === "weeks") {
                policy.ttl = policy.ttl_number * WEEK * 24 * 3600;
            } else if (policy.ttl_type === "months") {
                policy.ttl = policy.ttl_number * MONTH * 24 * 3600;
            } else if (policy.ttl_type === "years") {
                policy.ttl = policy.ttl_number * YEAR * 24 * 3600;
            } else {
                policy.ttl = null;
            }
            let save_policy = {
                [data_type]: policy.ttl
            }
            console.log(save_policy);
            RetentionPolicyService.update(save_policy,
                function(apiResponse) {
                    console.log(apiResponse);
                }
            )
            if (policy.ttl == null) {
                policy.ttl_display = "None";
            } else {
                policy.ttl_display = policy.ttl_number + " " + policy.ttl_type;
            }
            $scope.edit_policy[data_type] = false;
            //$scope.policy_map[data_type] = policy;
        }

        var cronstrue = $window.cronstrue;
        $scope.cron_english = "";
        $scope.cron_expression = "* * * * * *";
        $scope.split_cron = {
            'second': null,
            'minute': null,
            'hour': null,
            'day': null,
            'month': null,
            'day_of_week': null
        }

        $scope.freq = null;
        $scope.month = null;
        $scope.day_of_week = null;
        $scope.days_of_week = [
            'Sunday',
            'Monday',
            'Tuesday',
            'Wednesday',
            'Thursday',
            'Friday',
            'Saturday'
        ];

        (function() {
            RetentionScheduleService.query(
                function(apiResponse) {
                    $scope.cron_expression = apiResponse.data;
                    $scope.split_cron = parse_cron($scope.cron_expression);
                    console.log($scope.split_cron);
                    if ($scope.split_cron.disabled) {
                        $scope.freq = "disabled";
                    } else if ($scope.split_cron.day != null) {
                        $scope.freq = "month";
                    } else if ($scope.split_cron.day_of_week != null) {
                        $scope.freq = "week";
                        $scope.day_of_week = $scope.days_of_week[$scope.split_cron.day_of_week];
                    } else if ($scope.split_cron.hour != null) {
                        $scope.freq = "day";
                    } else {
                        $scope.freq = "hour";
                    }
                    if ($scope.freq === "disabled") {
                        $scope.cron_english = "Not Current Scheduled to Run";
                    } else {
                        $scope.cron_english = cronstrue.toString($scope.cron_expression);
                    }
                    console.log("Every " + $scope.freq + "; day: " + $scope.split_cron.day + ", day of week: " + $scope.day_of_week + " at hour: " + $scope.split_cron.hour);
                }
            )
        })();


        var parse_cron = function(cron_string) {
            let split_cron = cron_string.split(' ');
            var return_dict = {
                'second': null,
                'minute': null,
                'hour': null,
                'day': null,
                'month': null,
                'day_of_week': null,
                'disabled': false
            }
            console.log(split_cron);
            if (split_cron[0] === "-") {
                return_dict.disabled = true;
                return return_dict;
            }
            if (split_cron[0] !== "*") {
                return_dict.second = parseInt(split_cron[0]);
            }
            if (split_cron[1] !== "*") {
                return_dict.minute = parseInt(split_cron[1]);
            }
            if (split_cron[2] !== "*") {
                return_dict.hour = parseInt(split_cron[2]);
            }
            if (split_cron[3] !== "*") {
                return_dict.day = parseInt(split_cron[3]);
            }
            if (split_cron[4] !== "*") {
                return_dict.month = parseInt(split_cron[4]);
            }
            if (split_cron[5] !== "*") {
                return_dict.day_of_week = parseInt(split_cron[5]);
            }
            return return_dict;
        }

        $scope.saveCron = function() {
            console.log($scope.split_cron);
            $scope.cron_expression = "0";
            if ($scope.freq === "disabled") {
                $scope.cron_expression = "-";
            }
            else if ($scope.freq === "hour") {
                $scope.cron_expression += " " + $scope.split_cron.minute + " * * * *";
            } else if ($scope.freq === "day") {
                if ($scope.split_cron.minute === null) {
                    $scope.split_cron.minute = 0;
                }
                if ($scope.split_cron.hour === null) {
                    $scope.split_cron.hour = 0;
                }
                $scope.cron_expression += " " + $scope.split_cron.minute + " " + $scope.split_cron.hour + " * * *";
            } else if ($scope.freq === "week") {
                if ($scope.split_cron.minute === null) {
                    $scope.split_cron.minute = 0;
                }
                if ($scope.split_cron.hour === null) {
                    $scope.split_cron.hour = 0;
                }
                if ($scope.split_cron.day_of_week === null) {
                    $scope.split_cron.day_of_week = 0;
                }
                $scope.cron_expression += " " + $scope.split_cron.minute + " " + $scope.split_cron.hour + " * * " + $scope.split_cron.day_of_week;
            } else if ($scope.freq === "month") {
                if ($scope.split_cron.minute === null) {
                    $scope.split_cron.minute = 0;
                }
                if ($scope.split_cron.hour === null) {
                    $scope.split_cron.hour = 0;
                }
                if ($scope.split_cron.day === null) {
                    $scope.split_cron.day = 1;
                }
                $scope.cron_expression += " " + $scope.split_cron.minute + " " + $scope.split_cron.hour + " " + $scope.split_cron.day + " * *";
            }
            console.log($scope.cron_expression);
            if ($scope.freq === "disabled") {
                $scope.cron_english = "Not Currently Scheduled to Run";
            } else {
                $scope.cron_english = cronstrue.toString($scope.cron_expression);
            }
            RetentionScheduleService.update($scope.cron_expression,
                function(apiResponse) {
                    console.log(apiResponse);
                }
            )
        }



    }])

dataRetentionControllers.controller('MissionArchiveCtrl', [
    '$scope',
    '$uibModal',
    '$window',
    'MissionArchiveService',
    'MissionRestoreService',
    'MissionArchiveConfigService',
    function(
        $scope,
        $uibModal,
        $window,
        MissionArchiveService,
        MissionRestoreService,
        MissionArchiveConfigService
    ) {

        $scope.missions = {}
        $scope.missions.missonArchiveStoreEntries = []

        $scope.missions.queryMissionName = ''
        $scope.missions.queryCreateTime = ''
        $scope.missions.queryArchiveTime = ''

        $scope.viewRetentionPolicies = function() {
            $window.location.href = '/Marti/data_retention/index.html#!/';
        }

        $scope.restoreMission = function(missonArchiveStoreEntry) {
            $scope.missions.missonArchiveStoreEntries = $scope.missions.missonArchiveStoreEntries.filter(function(entry, index, arr) { 
                return entry.id !== missonArchiveStoreEntry.id;
            });
            MissionRestoreService.save(missonArchiveStoreEntry.id,
                function(apiResponse) {
                    alert(apiResponse.data)
                }
            )
        }

        MissionArchiveService.query(res =>{
            let data = JSON.parse(res.data)
            if (data && data.missonArchiveStoreEntries) {
                $scope.missions.missonArchiveStoreEntries = data.missonArchiveStoreEntries;
                console.log($scope.missonArchiveStoreEntries)
            } else {
                $scope.missions.missonArchiveStoreEntries = []   
            }
         })

         $scope.missionFilter = function(mission) {
            return mission.missionName.toLowerCase().includes($scope.missions.queryMissionName.toLowerCase()) 
                && mission.archiveTime.toLowerCase().includes($scope.missions.queryArchiveTime.toLowerCase()) 
                && mission.createTime.toLowerCase().includes($scope.missions.queryCreateTime.toLowerCase()) 
        };

        var cronstrue = $window.cronstrue;
        $scope.cron_english = "";
        $scope.cron_expression = "* * * * * *";
        $scope.split_cron = {
            'second': null,
            'minute': null,
            'hour': null,
            'day': null,
            'month': null,
            'day_of_week': null
        }

        $scope.freq = null;
        $scope.month = null;
        $scope.day_of_week = null;
        $scope.days_of_week = [
            'Sunday',
            'Monday',
            'Tuesday',
            'Wednesday',
            'Thursday',
            'Friday',
            'Saturday'
        ];

        $scope.archiveMissionByNoContentActivity = false;
        $scope.archiveMissionByNoSubscriptionActivity = false;
        $scope.archiveAfterNoActivityDays = 365;
        $scope.removeFromArchiveAfterDays = 1065;

        MissionArchiveConfigService.query(
            function(apiResponse) {
                console.log(apiResponse.data)
                $scope.archiveMissionByNoContentActivity = apiResponse.data.archiveMissionByNoContentActivity;
                $scope.archiveMissionByNoSubscriptionActivity = apiResponse.data.archiveMissionByNoSubscriptionActivity;
                $scope.timeToArchiveAfterNoActivityDays = apiResponse.data.timeToArchiveAfterNoActivityDays;
                $scope.removeFromArchiveAfterDays = apiResponse.data.removeFromArchiveAfterDays;

                $scope.cron_expression = apiResponse.data.cronExpression;
                $scope.split_cron = parse_cron($scope.cron_expression);
                if ($scope.split_cron.disabled) {
                    $scope.freq = "disabled";
                } else if ($scope.split_cron.day != null) {
                    $scope.freq = "month";
                } else if ($scope.split_cron.day_of_week != null) {
                    $scope.freq = "week";
                    $scope.day_of_week = $scope.days_of_week[$scope.split_cron.day_of_week];
                } else if ($scope.split_cron.hour != null) {
                    $scope.freq = "day";
                } else {
                    $scope.freq = "hour";
                }
                if ($scope.freq === "disabled") {
                    $scope.cron_english = "Not Current Scheduled to Run";
                } else {
                    $scope.cron_english = cronstrue.toString($scope.cron_expression);
                }
            }
        )


        var parse_cron = function(cron_string) {
            let split_cron = cron_string.split(' ');
            var return_dict = {
                'second': null,
                'minute': null,
                'hour': null,
                'day': null,
                'month': null,
                'day_of_week': null,
                'disabled': false
            }

            if (split_cron[0] === "-") {
                return_dict.disabled = true;
                return return_dict;
            }
            if (split_cron[0] !== "*") {
                return_dict.second = parseInt(split_cron[0]);
            }
            if (split_cron[1] !== "*") {
                return_dict.minute = parseInt(split_cron[1]);
            }
            if (split_cron[2] !== "*") {
                return_dict.hour = parseInt(split_cron[2]);
            }
            if (split_cron[3] !== "*") {
                return_dict.day = parseInt(split_cron[3]);
            }
            if (split_cron[4] !== "*") {
                return_dict.month = parseInt(split_cron[4]);
            }
            if (split_cron[5] !== "*") {
                return_dict.day_of_week = parseInt(split_cron[5]);
            }
            return return_dict;
        }

        $scope.saveArchiveSettings = function() {
            let update = {
                cronExpression:$scope.cron_expression,
                archiveMissionByNoContentActivity: $scope.archiveMissionByNoContentActivity,
                archiveMissionByNoSubscriptionActivity: $scope.archiveMissionByNoSubscriptionActivity,
                timeToArchiveAfterNoActivityDays: $scope.timeToArchiveAfterNoActivityDays,
                removeFromArchiveAfterDays: $scope.removeFromArchiveAfterDays
            }

            MissionArchiveConfigService.update(update,
                function (apiResponse) {
                    alert('Saved Settings');
                },
                function (apiResponse) {
                    alert('Error Saving Settings');
                }
            )
        }

        $scope.saveCron = function() {
            $scope.cron_expression = "0";
            if ($scope.freq === "disabled") {
                $scope.cron_expression = "-";
            }
            else if ($scope.freq === "hour") {
                $scope.cron_expression += " " + $scope.split_cron.minute + " * * * *";
            } else if ($scope.freq === "day") {
                if ($scope.split_cron.minute === null) {
                    $scope.split_cron.minute = 0;
                }
                if ($scope.split_cron.hour === null) {
                    $scope.split_cron.hour = 0;
                }
                $scope.cron_expression += " " + $scope.split_cron.minute + " " + $scope.split_cron.hour + " * * *";
            } else if ($scope.freq === "week") {
                if ($scope.split_cron.minute === null) {
                    $scope.split_cron.minute = 0;
                }
                if ($scope.split_cron.hour === null) {
                    $scope.split_cron.hour = 0;
                }
                if ($scope.split_cron.day_of_week === null) {
                    $scope.split_cron.day_of_week = 0;
                }
                $scope.cron_expression += " " + $scope.split_cron.minute + " " + $scope.split_cron.hour + " * * " + $scope.split_cron.day_of_week;
            } else if ($scope.freq === "month") {
                if ($scope.split_cron.minute === null) {
                    $scope.split_cron.minute = 0;
                }
                if ($scope.split_cron.hour === null) {
                    $scope.split_cron.hour = 0;
                }
                if ($scope.split_cron.day === null) {
                    $scope.split_cron.day = 1;
                }
                $scope.cron_expression += " " + $scope.split_cron.minute + " " + $scope.split_cron.hour + " " + $scope.split_cron.day + " * *";
            }

            if ($scope.freq === "disabled") {
                $scope.cron_english = "Not Currently Scheduled to Run";
            } else {
                $scope.cron_english = cronstrue.toString($scope.cron_expression);
            }

            let update = {
                cronExpression:$scope.cron_expression,
                archiveMissionByNoContentActivity: $scope.archiveMissionByNoContentActivity,
                archiveMissionByNoSubscriptionActivity: $scope.archiveMissionByNoSubscriptionActivity,
                timeToArchiveAfterNoActivityDays: $scope.timeToArchiveAfterNoActivityDays,
                removeFromArchiveAfterDays: $scope.removeFromArchiveAfterDays
            }

            MissionArchiveConfigService.update(update,
                function (apiResponse) {
                    alert('Saved Settings');
                },
                function (apiResponse) {
                    alert('Error Saving Settings');
                }
            )
        }
    }])
















