/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function(){
    'use strict';
    angular.module('gantt').factory('GanttHeaderColumns', [function() {
        var HeaderColumns = function($element) {
            this.$element = $element;
        };
        return HeaderColumns;
    }]);
}());

