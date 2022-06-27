/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
var app = angular.module('app', ['ui.bootstrap', 'ui.bootstrap.datetimepicker']);

app.controller('MyController', ['$scope', function($scope) {

    var that = this;

    var in10Days = new Date();
    in10Days.setDate(in10Days.getDate() + 10);

    this.dates = {
        date1: new Date('2015-03-01T00:00:00Z'),
        date2: new Date('2015-03-01T12:30:00Z'),
        date3: new Date(),
        date4: new Date(),
        date5: in10Days,
        date6: new Date(),
        date7: new Date(),
        date8: new Date(),
        date9: null,
        date10: new Date('2015-03-01T09:00:00Z'),
        date11: new Date('2015-03-01T10:00:00Z')
    };

    this.open = {
        date1: false,
        date2: false,
        date3: false,
        date4: false,
        date5: false,
        date6: false,
        date7: false,
        date8: false,
        date9: false,
        date10: false,
        date11: false
    };

    // Disable today selection
    this.disabled = function(date, mode) {
        return (mode === 'day' && (new Date().toDateString() == date.toDateString()));
    };

    this.dateOptions = {
        showWeeks: false,
        startingDay: 1
    };

    this.timeOptions = {
        readonlyInput: false,
        showMeridian: false
    };

    this.dateModeOptions = {
        minMode: 'year',
        maxMode: 'year'
    };

    this.openCalendar = function(e, date) {
        that.open[date] = true;
    };

    // watch date4 and date5 to calculate difference
    var unwatch = $scope.$watch(function() {
        return that.dates;
    }, function() {
        if (that.dates.date4 && that.dates.date5) {
            var diff = that.dates.date4.getTime() - that.dates.date5.getTime();
            that.dayRange = Math.round(Math.abs(diff/(1000*60*60*24)))
        } else {
            that.dayRange = 'n/a';
        }
    }, true);

    $scope.$on('$destroy', function() {
        unwatch();
    });
}]);