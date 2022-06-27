/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt').factory('GanttBodyRows', [function() {
        var BodyRows = function(body) {
            this.body = body;
        };
        return BodyRows;
    }]);
}());

