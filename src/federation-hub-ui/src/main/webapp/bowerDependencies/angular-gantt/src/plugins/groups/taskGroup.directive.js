/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt.groups').directive('ganttTaskGroup', ['GanttDirectiveBuilder', function(Builder) {
        var builder = new Builder('ganttTaskGroup', 'plugins/groups/taskGroup.tmpl.html');
        return builder.build();
    }]);
}());

