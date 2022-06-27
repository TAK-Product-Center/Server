/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt').factory('GanttBodyBackground', [function() {
        var GanttBodyBackground = function(body) {
            this.body = body;
        };
        return GanttBodyBackground;
    }]);
}());
