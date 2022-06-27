/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt.labels').directive('ganttLabelsHeader', ['GanttDirectiveBuilder', function(Builder) {
        var builder = new Builder('ganttLabelsHeader', 'plugins/labels/labelsHeader.tmpl.html');
        return builder.build();
    }]);
}());

