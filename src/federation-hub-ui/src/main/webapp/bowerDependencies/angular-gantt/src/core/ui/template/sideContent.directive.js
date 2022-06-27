/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt').directive('ganttSideContent', ['GanttDirectiveBuilder', function(Builder) {
        var builder = new Builder('ganttSideContent');
        return builder.build();
    }]);
}());

