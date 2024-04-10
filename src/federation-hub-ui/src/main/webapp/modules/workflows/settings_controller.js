/*******************************************************************************
 * DISTRIBUTION C. Distribution authorized to U.S. Government agencies and their contractors. Other requests for this document shall be referred to the United States Air Force Research Laboratory.
 *
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
"use strict";

angular.module('roger_federation.Workflows')
    .controller('SettingsController', ['$rootScope', '$scope', '$http', '$stateParams', '$timeout', '$log', '$cookieStore', 'growl', 'WorkflowService', settingsController]);

function settingsController($rootScope, $scope, $http, $stateParams, $timeout, $log, $cookieStore, growl, WorkflowService) {

    $scope.downloadSelfCa = function() {
      WorkflowService.getSelfCa().then(function(result) {
          if (!result) {
              growl.error("Failed to Load Self CA" + error);
              return
          }
          try{
              const a = document.createElement("a");
              a.href = URL.createObjectURL(new Blob([result], {
                type: "application/x-pem-file"
              }));
              a.setAttribute("download", "ca.pem");
              document.body.appendChild(a);
              a.click();
              document.body.removeChild(a);
              growl.success("Downloaded ca.pem file!");
          } catch(e) {
              growl.error("Failed to Load Self CA" + error);
          }
      }, function(error) {
          growl.error("Failed to Load Self CA" + error);
      });
    }

}