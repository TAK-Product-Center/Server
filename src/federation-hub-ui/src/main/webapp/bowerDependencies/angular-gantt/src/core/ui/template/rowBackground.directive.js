/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt').directive('ganttRowBackground', ['GanttDirectiveBuilder', function(Builder) {
        var builder = new Builder('ganttRowBackground');
        return builder.build();
    }]);
}());

