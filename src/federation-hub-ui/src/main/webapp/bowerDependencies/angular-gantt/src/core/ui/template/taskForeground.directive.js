/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt').directive('ganttTaskForeground', ['GanttDirectiveBuilder', function(Builder) {
        var builder = new Builder('ganttTaskForeground');
        return builder.build();
    }]);
}());

