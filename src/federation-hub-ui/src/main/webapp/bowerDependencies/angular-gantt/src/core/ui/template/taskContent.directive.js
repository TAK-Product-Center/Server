/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt').directive('ganttTaskContent', ['GanttDirectiveBuilder', function(Builder) {
        var builder = new Builder('ganttTaskContent');
        return builder.build();
    }]);
}());

