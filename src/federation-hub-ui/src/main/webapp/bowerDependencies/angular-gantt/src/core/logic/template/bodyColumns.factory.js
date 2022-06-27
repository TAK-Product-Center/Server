/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt').factory('GanttBodyColumns', [function() {
        var BodyColumns = function(body) {
            this.body = body;
        };
        return BodyColumns;
    }]);
}());

