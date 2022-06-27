'use strict';

var certificateManagerControllers = angular.module('clientCertificateManagerControllers', []);

certificateManagerControllers.controller('ClientCertificatesListCtrl', ['$scope', '$location', 'ClientCertificatesService',
    function ($scope, $location, ClientCertificatesService) {

        $scope.showRmiError = false;

        $scope.getAll = function() {
            ClientCertificatesService.certs.query(
                function(apiResponse) {$scope.clientCertificates = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.getActive = function() {
            ClientCertificatesService.active.query(
                function(apiResponse) {$scope.clientCertificates = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.getReplaced = function() {
            ClientCertificatesService.replaced.query(
                function(apiResponse) {$scope.clientCertificates = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.getExpired = function() {
            ClientCertificatesService.expired.query(
                function(apiResponse) {$scope.clientCertificates = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.getRevoked = function() {
            ClientCertificatesService.revoked.query(
                function(apiResponse) {$scope.clientCertificates = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.revokeCertificate = function(clientCertificate) {
            if (confirm('Are you sure you want to revoke the certificate (Subject  = ' + clientCertificate.subjectDn + ')? TAK Server must be restarted for the revocation to take effect.')) {
                ClientCertificatesService.certs.revoke({hash:clientCertificate.hash},
                    function(apiResponse) {$scope.getAll();},
                    function() {alert('An unexpected error occurred revoking the certificate.');});
            }
        }

        $scope.toggle = function () {
            angular.forEach($scope.clientCertificates, function(cc) { cc.selected = !cc.selected; });
        }

        $scope.getSelected = function () {
            var ids = '';
            angular.forEach($scope.clientCertificates, function(cc) {
                if (cc.selected) {
                    ids += cc.id + ',';
                }
            });
            return ids;
        }

        $scope.download = function () {
            var ids = $scope.getSelected();
            if (ids.length == 0) {
                return;
            }

            window.open('/Marti/api/certadmin/cert/download/' + ids);
        }

        $scope.delete = function () {
            var ids = $scope.getSelected();
            if (ids.length == 0) {
                return;
            }

            if (!confirm("Press Ok to confirm deletion")) {
                return;
            }

            ClientCertificatesService.delete.delete({ids:ids},
                function(apiResponse) {$scope.getAll();},
                function() {alert('An unexpected error occurred deleting the certificates.');});
        }

        $scope.revokeSelected = function () {
            var ids = $scope.getSelected();
            if (ids.length == 0) {
                return;
            }

            if (!confirm("Press Ok to confirm revocation. TAK Server must be restarted for the revocation to take effect.")) {
                return;
            }

            ClientCertificatesService.revoke.revoke({ids:ids},
                function(apiResponse) {$scope.getAll();},
                function() {alert('An unexpected error occurred revoking the certificates.');});
        }

        $scope.sortPropertyName = 'issuanceDate';
        $scope.reverse = false;

        $scope.sortBy = function(sortPropertyName) {
            $scope.reverse = ($scope.sortPropertyName === sortPropertyName) ? !$scope.reverse : false;
            $scope.sortPropertyName = sortPropertyName;
        };

        $scope.getAll();
    }]);

certificateManagerControllers.controller('ViewCertificateCtrl', ['$scope', '$location', 'ViewCertificateService', '$routeParams',
    function ($scope, $location, ViewCertificateService, $routeParams) {

        $scope.showRmiError = false;

        $scope.getClientCertificate = function() {
            ViewCertificateService.query(
                {hash: $routeParams.hash },
                function(apiResponse) {$scope.clientCertificate = apiResponse.data;},
                function() {$scope.showRmiError = true;});
        }

        $scope.backToClientCertificates = function() {
            $location.path("/");
        };

        $scope.getClientCertificate();
    }]);

