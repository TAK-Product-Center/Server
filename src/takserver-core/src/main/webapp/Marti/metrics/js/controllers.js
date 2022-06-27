'use strict';

var controllers = angular.module('metricsManagerControllers', []);

controllers.controller('ConnectionCtrl', function($scope, $window, $timeout, $routeParams, MetricsService, ChartService, $http) {
    $scope.uid = $routeParams.uid;
    $scope.isFocused = true;
    $scope.error = '';

    $scope.closeRow1 = function() {
        $scope.hideRow1 = !$scope.hideRow1;
        $scope.hasAdminRole = !$scope.hasAdminRole;
    }
    $scope.closeRow2 = function() {
        $scope.hideRow2 = !$scope.hideRow2;
    }

    // ////////// SINGLE NETWORK BYTES ////////////

    $scope.initSingleBytesChart = function() {
        $scope.singleBytesChart = new ChartService();
        $scope.prevSingleBytesWritten = 0;
        $scope.prevSingleBytesRead = 0;
        let container = $('#singleBytesContent');
        $scope.singleBytesChart.setSelector('#singleBytesChart')
            .setNumLines(2)
            .setHeight(container.height() - $('#singleBytesLabel').outerHeight(true))
            .setWidth(container.width())
            .setMargins({ right: container.width() * .1, left: container.width() * .1, bottom: 30, top: 10 })
            .setLineColors(['blue', 'red'])
            .setMaxDataPoints(20)
            .init();
        singleBytesInterval()
    }

    function singleBytesInterval() {
        MetricsService.getConnectionCustomNetworkMetrics($scope.uid).then(function(res) {
            if (!res.data['uid']) {
                $scope.error = 'Connection no longer exists.';
                $timeout.cancel($scope.singleBytesInterval)
            }
            $scope.props = res.data;
            $scope.totalSingleBytesWritten = res.data["totalTcpBytesWritten"];
            $scope.totalSingleBytesRead = res.data["totalTcpBytesRead"];
            if ($scope.prevSingleBytesWritten == 0) $scope.prevSingleBytesWritten = $scope.totalSingleBytesWritten;
            if ($scope.prevSingleBytesRead == 0) $scope.prevSingleBytesRead = $scope.totalSingleBytesRead;

            $scope.curSingleBytesWritten = $scope.totalSingleBytesWritten - $scope.prevSingleBytesWritten;
            $scope.curSingleBytesRead = $scope.totalSingleBytesRead - $scope.prevSingleBytesRead;

            $scope.prevSingleBytesWritten = $scope.totalSingleBytesWritten;
            $scope.prevSingleBytesRead = $scope.totalSingleBytesRead;

            $scope.singleBytesChart.addDataPoint([$scope.curSingleBytesWritten, $scope.curSingleBytesRead])
            $scope.singleBytesChart.redraw()
        }, err => {});
        $scope.singleBytesInterval = $timeout(singleBytesInterval, 5000)
    }

    ////////// SINGLE NETWORK READ/WRITES ////////////
    $scope.initSingleNetworkReadWritesChart = function() {
        $scope.singleNetworkReadWritesChart = new ChartService();
        $scope.prevSingleWrites = 0;
        $scope.prevSingleReads = 0;
        let container = $('#singleNetworkReadWritesContent');
        $scope.singleNetworkReadWritesChart.setSelector('#singleNetworkReadWritesChart')
            .setNumLines(2)
            .setHeight(container.height() - $('#singleNetworkReadWritesLabel').outerHeight(true))
            .setWidth(container.width())
            .setMargins({ right: container.width() * .1, left: container.width() * .1, bottom: 30, top: 10 })
            .setLineColors(['blue', 'red'])
            .setMaxDataPoints(20)
            .init();
        singleNetworkReadWritesInterval()
    }

    function singleNetworkReadWritesInterval() {
        MetricsService.getConnectionCustomNetworkMetrics($scope.uid).then(function(res) {
            if (!res.data['uid']) {
                $scope.error = 'Connection no longer exists.';
                $timeout.cancel($scope.singleNetworkReadWritesInterval)
            }

            $scope.totalSingleWrites = res.data["totalTcpNumberOfWrites"] / 5;
            $scope.totalSingleReads = res.data["totalTcpNumberOfReads"] / 5;

            if ($scope.prevSingleWrites == 0) $scope.prevSingleWrites = $scope.totalSingleWrites;
            if ($scope.prevSingleReads == 0) $scope.prevSingleReads = $scope.totalSingleReads;

            $scope.curSingleWrites = $scope.totalSingleWrites - $scope.prevSingleWrites;
            $scope.curSingleReads = $scope.totalSingleReads - $scope.prevSingleReads;

            $scope.prevSingleWrites = $scope.totalSingleWrites;
            $scope.prevSingleReads = $scope.totalSingleReads;

            $scope.singleNetworkReadWritesChart.addDataPoint([$scope.curSingleWrites, $scope.curSingleReads])
            $scope.singleNetworkReadWritesChart.redraw()
        }, err => {});
        $scope.singleNetworkReadWritesInterval = $timeout(singleNetworkReadWritesInterval, 5000)
    }
});

controllers.controller('DashboardCtrl', function($scope, $window, $timeout, $http, MetricsService, ChartService, DonutService) {
    $scope.tcpConnections = {};
    $scope.isFocused = true;
    $scope.startTime = new Date();
    $scope.hasAdminRole = false;
    $scope.dbIsConnected = true;
    $scope.maxConnections = 0;
    $scope.actualNum = 0;


    (function setAdmin() {
      $http.get('/Marti/api/util/isAdmin').then(function(response){
          $scope.hasAdminRole = response.data;
      })
    })();

    (function pollDBStatus() {
        MetricsService.getDatabaseMetrics().get(function (res) {
            $scope.dbIsConnected = res.apiConnected && res.messagingConnected;
            $scope.maxConnections = res.maxConnections;
            $timeout(pollDBStatus, 5000);
        }, err => {});
    })();
    
    (function getDbConfig() {
        $http.get('/Marti/api/inputs/config').then(
            function(response) {
                if (response.data.data.connectionPoolAutoSize) {
                    $scope.actualNum = response.data.data.numAutoDbConnections;
                } else {
                    $scope.actualNum = response.data.data.numDbConnections;
                }
            }
        )
    })();

    $scope.closeRow1 = function() {
        $scope.hideRow1 = !$scope.hideRow1;
    }
    $scope.closeRow2 = function() {
        $scope.hideRow2 = !$scope.hideRow2;
    }
    $scope.closeRow3 = function() {
        $scope.hideRow3 = !$scope.hideRow3;
    }
    $scope.closeRow4 = function() {
        $scope.hideRow4 = !$scope.hideRow4;
    }
    //////////// LIFECYCLE EVENTS ////////////
    var handleFocus = function() {
        $scope.isFocused = true;
        // $('#metrics-container').removeClass('blur-filter')
    }
    var handleBlur = function() {
        $scope.isFocused = false;
        // $('#metrics-container').addClass('blur-filter')
    }

    $window.onfocus = handleFocus;

    $window.onblur = handleBlur;

    $scope.$on('$destroy', function() {
        handleBlur()
    });

    pollQueueMetrics();
    
    pollNetworkMetricsInterval();

    $timeout(function() {
        document.hasFocus() ? handleFocus() : handleBlur();
    }, 5000)

    $scope.selectedConnection = null;
    $scope.setSelected = function(selectedConnection) {
        $scope.selectedConnection = selectedConnection;
        $scope.prevSingleWrites = 0;
        $scope.prevSingleReads = 0;
        $scope.prevSingleBytesWritten = 0;
        $scope.prevSingleBytesRead = 0;
        $scope.singleBytesChart.clear();
        $scope.singleNetworkReadWritesChart.clear();
    };

    //////////// SERVER START TIME ////////////

    $scope.initStartTime = function() {
        MetricsService.getStartTime().get(function(res) {
            let time = new Date(res.measurements[0].value * 1000)

            if (time > 100000000000000)
                time /= 1000

            $scope.startTime = new Date(time);
        }, err => {});
        upTimeInterval();
    }

    function upTimeInterval() {
        if ($scope.isFocused) {
            $scope.upTime = convertSeconds((new Date() - new Date($scope.startTime)) / 1000);
        }
        $timeout(upTimeInterval, 5000);
    }

    //////////// HEAP MEMORY ////////////

    $scope.initHeapMemoryChart = function() {
        $scope.heapMemoryChart = new ChartService();
        let container = $('#heapContent');
        $scope.heapMemoryChart.setSelector('#chart')
            .setNumLines(2)
            .setHeight(container.height() - $('#heapLabel').outerHeight(true))
            .setWidth(container.width())
            .setLineColors(['red', 'green'])
            .setAreaColors(['orange', 'lightgreen'])
            .setMargins({ right: container.width() * .1, left: container.width() * .1, bottom: 30, top: 10 })
            .setAreaUnderLine(true)
            .setAreaOpacity([.6, .4])
            .setMaxDataPoints(20)
            .init();
        heapMemoryInterval()
    }

    function heapMemoryInterval() {
        MetricsService.getCustomMemoryMetrics().get(function(res) {
            let heapCommitted = sciToNumber(res["heapCommitted"]);
            let heapUsed = sciToNumber(res["heapUsed"]);

            heapUsed = Math.round(heapUsed / 1000000)
            heapCommitted = Math.round(heapCommitted / 1000000)

            $scope.heapCommitted = heapCommitted;
            $scope.heapUsed = heapUsed;

            let memoryMetrics = {
                heapCommitted,
                heapUsed
            };

            $scope.heapMemoryChart.addDataPoint([heapCommitted, heapUsed])
            if ($scope.isFocused) $scope.heapMemoryChart.redraw()
        }, err => {});
        $timeout(heapMemoryInterval, 5000);
    }

    //////////// Network I/O ////////////
    
    
    $scope.initNetworkBytesWrittenChart = function(writes, reads) {
        $scope.networkBytesWrittenChart = new ChartService();
        $scope.prevBytesWritten = 0;
        $scope.totalBytesWritten = 0;
        let container = $('#networkBytesWrittenContent');
        $scope.networkBytesWrittenChart.setSelector('#networkBytesWrittenChart')
            .setNumLines(1)
            .setHeight(container.height() - $('#networkBytesWrittenLabel').outerHeight(true))
            .setWidth(container.width())
            .setMargins({ right: container.width() * .1, left: container.width() * .1, bottom: 30, top: 10 })
            .setLineColors(['blue'])
            .setMaxDataPoints(20)
            .init();
    }

    $scope.initNetworkBytesReadChart = function(writes, reads) {
        $scope.networkBytesReadChart = new ChartService();
        $scope.prevBytesRead = 0;
        $scope.totalBytesRead = 0;
        let container = $('#networkBytesReadContent');
        $scope.networkBytesReadChart.setSelector('#networkBytesReadChart')
            .setNumLines(1)
            .setHeight(container.height() - $('#networkBytesReadLabel').outerHeight(true))
            .setWidth(container.width())
            .setMargins({ right: container.width() * .1, left: container.width() * .1, bottom: 30, top: 10 })
            .setLineColors(['red'])
            .setMaxDataPoints(20)
            .init();
    }

    $scope.initNetworkWritesChart = function() {
        $scope.networkWritesChart = new ChartService();
        $scope.prevWrites = 0;

        let container = $('#networkWritesContent');
        $scope.networkWritesChart.setSelector('#networkWritesChart')
            .setNumLines(2)
            .setHeight(container.height() - $('#networkWritesLabel').outerHeight(true))
            .setWidth(container.width())
            .setMargins({ right: container.width() * .1, left: container.width() * .1, bottom: 30, top: 10 })
            .setLineColors(['blue'])
            .setMaxDataPoints(20)
            .init();
    }

    $scope.initNetworkReadChart = function() {
        $scope.networkReadChart = new ChartService();
        $scope.prevReads = 0;

        let container = $('#networkReadContent');
        $scope.networkReadChart.setSelector('#networkReadChart')
            .setNumLines(2)
            .setHeight(container.height() - $('#networkReadLabel').outerHeight(true))
            .setWidth(container.width())
            .setMargins({ right: container.width() * .1, left: container.width() * .1, bottom: 30, top: 10 })
            .setLineColors(['red'])
            .setMaxDataPoints(20)
            .init();
    }

    function pollNetworkMetricsInterval() {
        MetricsService.getCustomNetworkMetrics().get(function(res) {
            $scope.totalWrites = res["numWrites"] / 5;
            $scope.totalReads = res["numReads"] / 5;
            $scope.totalBytesRead = res["bytesRead"] / 5;
            $scope.totalBytesWritten = res["bytesWritten"] / 5;
           
            if ($scope.prevBytesRead == 0) $scope.prevBytesRead = $scope.totalBytesRead;
            if ($scope.prevReads == 0) $scope.prevReads = $scope.totalReads;
            if ($scope.prevWrites == 0) $scope.prevWrites = $scope.totalWrites;
            if ($scope.prevBytesWritten == 0) $scope.prevBytesWritten = $scope.totalBytesWritten;

            $scope.clientsConnected = res["numClients"];

            if ($scope.networkBytesWrittenChart){
                $scope.curBytesWritten = $scope.totalBytesWritten - $scope.prevBytesWritten;
                $scope.prevBytesWritten = $scope.totalBytesWritten;

                $scope.networkBytesWrittenChart.addDataPoint([$scope.curBytesWritten])
                if ($scope.isFocused) $scope.networkBytesWrittenChart.redraw() 
            }

            if ($scope.networkBytesReadChart){
                $scope.curBytesRead = $scope.totalBytesRead - $scope.prevBytesRead;
                $scope.prevBytesRead = $scope.totalBytesRead;

                $scope.networkBytesReadChart.addDataPoint([$scope.curBytesRead])
                if ($scope.isFocused) $scope.networkBytesReadChart.redraw() 
            }

            if ($scope.networkReadChart){
                $scope.curReads = $scope.totalReads - $scope.prevReads;
                $scope.prevReads = $scope.totalReads;

                $scope.networkReadChart.addDataPoint([$scope.curReads])
                if ($scope.isFocused) $scope.networkReadChart.redraw() 
            }

            if ($scope.networkWritesChart){
                $scope.curWrites = $scope.totalWrites - $scope.prevWrites;
                $scope.prevWrites = $scope.totalWrites;

                $scope.networkWritesChart.addDataPoint([$scope.curWrites])
                if ($scope.isFocused) $scope.networkWritesChart.redraw() 
            }
        }, err => {});
        $timeout(pollNetworkMetricsInterval, 5000);
    }

    //////////// CPU ////////////

    $scope.initCpuGauge = function() {
        $scope.cpuGauge = new DonutService();

        $scope.cpuGauge
            .setSelector('#donut')
            .setHeight($('#cpuContent').height() - $('#cpuLabel').outerHeight(true))
            .setWidth($('#cpuContent').height() - $('#cpuLabel').outerHeight(true))
            .init();
        
        cpuInterval()
    }

    function cpuInterval() {
        if ($scope.isFocused) {
            MetricsService.getCustomCpuMetrics().get(function(res) {
                $scope.cpuCores = res['cpuCount'];
                $scope.cpuUsage = Math.round(res['cpuUsage'] * 100 * 100) / 100
                $scope.cpuRatio = ($scope.cpuUsage * $scope.cpuCores) + '/' + ($scope.cpuCores * 100)
                $scope.cpuGauge.redraw($scope.cpuUsage)
            }, err => {});
        }
        $timeout(cpuInterval, 5000)
    }

    //////////// Queue ////////////

    $scope.initSubmissionQueueGauge = function() {
        $scope.submissionQueueGauge = new DonutService();

        $scope.submissionQueueGauge
            .setSelector('#submissionQueueGauge')
            .setHeight($('#submissionQueueGaugeContent').height() - $('#submissionQueueGaugeLabel').outerHeight(true))
            .setWidth($('#submissionQueueGaugeContent').height() - $('#submissionQueueGaugeLabel').outerHeight(true))
            .init();
    }

    $scope.initBrokerQueueGauge = function() {
        $scope.brokerQueueGauge = new DonutService();

        $scope.brokerQueueGauge
            .setSelector('#brokerQueueGauge')
            .setHeight($('#brokerQueueGaugeContent').height() - $('#brokerQueueGaugeLabel').outerHeight(true))
            .setWidth($('#brokerQueueGaugeContent').height() - $('#brokerQueueGaugeLabel').outerHeight(true))
            .init();
    }

    $scope.initRepositoryQueueGauge = function() {
        $scope.repositoryQueueGauge = new DonutService();

        $scope.repositoryQueueGauge
            .setSelector('#repositoryQueueGauge')
            .setHeight($('#repositoryQueueGaugeContent').height() - $('#repositoryQueueGaugeLabel').outerHeight(true))
            .setWidth($('#repositoryQueueGaugeContent').height() - $('#repositoryQueueGaugeLabel').outerHeight(true))
            .init();
    }

    function pollQueueMetrics() {
        MetricsService.getCustomQueueMetrics().get(function(res) {
            $scope.repositoryCapacity = res['brokerCapacity'];
            $scope.repositorySize = res['brokerSize'];
            $scope.submissionCapacity = res['submissionCapacity'];
            $scope.submissionSize = res['submissionSize'];
            $scope.brokerCapacity = res['repositoryCapacity'];
            $scope.brokerSize = res['repositorySize'];
            if($scope.isFocused && $scope.repositoryQueueGauge) 
                $scope.repositoryQueueGauge.redraw(Math.round(($scope.repositorySize/$scope.repositoryCapacity) * 100 * 100) / 100)
            if($scope.isFocused && $scope.submissionQueueGauge) 
                $scope.submissionQueueGauge.redraw(Math.round(($scope.submissionSize/$scope.submissionCapacity) * 100 * 100) / 100)
            if($scope.isFocused && $scope.brokerQueueGauge) 
                $scope.brokerQueueGauge.redraw(Math.round(($scope.brokerSize/$scope.brokerCapacity) * 100 * 100) / 100)
        }, err => {console.log('could not get queue metrics')});
        $timeout(pollQueueMetrics, 5000)
    }

    //////////// HELPER FUNCTIONS ////////////

    function sciToNumber(num) {
        return new Number(new Number(num).toString().replace(".", ""));
    }

    function convertSeconds(seconds) {
        var day, hour, minute;
        seconds = Math.floor(seconds)
        minute = Math.floor(seconds / 60);
        seconds = seconds % 60;
        hour = Math.floor(minute / 60);
        minute = minute % 60;
        day = Math.floor(hour / 24);
        hour = hour % 24;

        var str = ''
        str += (day + 'D:');
        str += (hour + 'H:')
        str += (minute + 'M:')
        str += (seconds + 'S')

        return str;
    }
});