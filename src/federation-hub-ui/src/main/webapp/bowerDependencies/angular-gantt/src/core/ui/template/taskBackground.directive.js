/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt').directive('ganttTaskBackground', ['GanttDirectiveBuilder', function(Builder) {
        var builder = new Builder('ganttTaskBackground');
        return builder.build();
    }]);
}());

