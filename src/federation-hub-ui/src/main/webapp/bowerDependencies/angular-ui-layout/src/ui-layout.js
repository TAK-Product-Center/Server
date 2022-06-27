/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
'use strict';

/**
 * UI.Layout
 */
angular.module('ui.layout', [])
  .controller('uiLayoutCtrl', ['$scope', '$attrs', '$element', '$timeout', '$window', 'LayoutContainer', 'Layout',
  function uiLayoutCtrl($scope, $attrs, $element, $timeout, $window, LayoutContainer, Layout) {

    var ctrl = this;
    var opts = angular.extend({}, $scope.$eval($attrs.uiLayout), $scope.$eval($attrs.options));
    var numOfSplitbars = 0;
    //var cache = {};
    var animationFrameRequested;
    var lastPos;

    // regex to verify size is properly set to pixels or percent
    var sizePattern = /\d+\s*(px|%)\s*$/i;

    Layout.addLayout(ctrl);

    ctrl.containers = [];
    ctrl.movingSplitbar = null;
    ctrl.bounds = $element[0].getBoundingClientRect();
    ctrl.isUsingColumnFlow = opts.flow === 'column';
    ctrl.sizeProperties = !ctrl.isUsingColumnFlow ?
    { sizeProperty: 'height',
      offsetSize: 'offsetHeight',
      offsetPos: 'top',
      flowProperty: 'top',
      oppositeFlowProperty: 'bottom',
      mouseProperty: 'clientY',
      flowPropertyPosition: 'y' } :
    { sizeProperty: 'width',
      offsetSize: 'offsetWidth',
      offsetPos: 'left',
      flowProperty: 'left',
      oppositeFlowProperty: 'right',
      mouseProperty: 'clientX',
      flowPropertyPosition: 'x' };

    $element
      // Force the layout to fill the parent space
      // fix no height layout...
      .addClass('stretch')
      // set the layout css class
      .addClass('ui-layout-' + (opts.flow || 'row'));

    if (opts.disableToggle) {
      $element.addClass('no-toggle');
    }
    if (opts.disableMobileToggle) {
      $element.addClass('no-mobile-toggle');
    }

    // Initial global size definition
    opts.sizes = opts.sizes || [];
    opts.maxSizes = opts.maxSizes || [];
    opts.minSizes = opts.minSizes || [];
    opts.dividerSize = opts.dividerSize === undefined ? 10 : opts.dividerSize;
    opts.collapsed = opts.collapsed || [];
    ctrl.opts = opts;

    $scope.updateDisplay = function() {
      ctrl.calculate();
    };

    var debounceEvent;
    function draw() {
      var position = ctrl.sizeProperties.flowProperty;
      var dividerSize = parseInt(opts.dividerSize);
      var elementSize = $element[0][ctrl.sizeProperties.offsetSize];

      if(ctrl.movingSplitbar !== null) {
        var splitbarIndex = ctrl.containers.indexOf(ctrl.movingSplitbar);
        var nextSplitbarIndex = (splitbarIndex + 2) < ctrl.containers.length ? splitbarIndex + 2 : null;

        if(splitbarIndex > -1) {
          var processedContainers = ctrl.processSplitbar(ctrl.containers[splitbarIndex]);
          var beforeContainer = processedContainers.beforeContainer;
          var afterContainer = processedContainers.afterContainer;

          if(!beforeContainer.collapsed && !afterContainer.collapsed) {
            // calculate container positons
            var difference = ctrl.movingSplitbar[position] - lastPos;
            var newPosition = ctrl.movingSplitbar[position] - difference;

            // Keep the bar in the window (no left/top 100%)
            newPosition = Math.min(elementSize-dividerSize, newPosition);

            // Keep the bar from going past the previous element min/max values
            if(angular.isNumber(beforeContainer.beforeMinValue) && newPosition < beforeContainer.beforeMinValue)
              newPosition = beforeContainer.beforeMinValue;
            if(angular.isNumber(beforeContainer.beforeMaxValue) && newPosition > beforeContainer.beforeMaxValue)
              newPosition = beforeContainer.beforeMaxValue;

            // Keep the bar from going past the next element min/max values
            if(afterContainer !== null &&
              angular.isNumber(afterContainer.afterMinValue) &&
              newPosition > (afterContainer.afterMinValue - dividerSize))
              newPosition = afterContainer.afterMinValue - dividerSize;
            if(afterContainer !== null && angular.isNumber(afterContainer.afterMaxValue) && newPosition < afterContainer.afterMaxValue)
              newPosition = afterContainer.afterMaxValue;

            // resize the before container
            beforeContainer.size = newPosition - beforeContainer[position];
            // store the current value to preserve this size during onResize
            beforeContainer.uncollapsedSize = beforeContainer.size;

            // update after container position
            var oldAfterContainerPosition = afterContainer[position];
            afterContainer[position] = newPosition + dividerSize;

            //update after container size if the position has changed
            if(afterContainer[position] != oldAfterContainerPosition) {
              afterContainer.size = (nextSplitbarIndex !== null) ?
              (oldAfterContainerPosition + afterContainer.size) - (newPosition + dividerSize) :
              elementSize - (newPosition + dividerSize);
              // store the current value to preserve this size during onResize
              afterContainer.uncollapsedSize = afterContainer.size;
            }

            // move the splitbar
            ctrl.movingSplitbar[position] = newPosition;

            // broadcast an event that resize happened (debounced to 50ms)
            if(debounceEvent) $timeout.cancel(debounceEvent);
            debounceEvent = $timeout(function() {
              $scope.$broadcast('ui.layout.resize', beforeContainer, afterContainer);
              debounceEvent = null;
            }, 50);
          }
        }
      }

      //Enable a new animation frame
      animationFrameRequested = null;
    }

    function offset(element) {
      var rawDomNode = element[0];
      var body = document.documentElement || document.body;
      var scrollX = window.pageXOffset || body.scrollLeft;
      var scrollY = window.pageYOffset || body.scrollTop;
      var clientRect = rawDomNode.getBoundingClientRect();
      var x = clientRect.left + scrollX;
      var y = clientRect.top + scrollY;
      return { left: x, top: y };
    }

    /**
     * Returns the current value for an option
     * @param  option   The option to get the value for
     * @return The value of the option. Returns null if there was no option set.
     */
    function optionValue(option) {
      if(typeof option == 'number' || typeof option == 'string' && option.match(sizePattern)) {
        return option;
      } else {
        return null;
      }
    }

    //================================================================================
    // Public Controller Functions
    //================================================================================
    ctrl.mouseUpHandler = function(event) {
      if(ctrl.movingSplitbar !== null) {
        ctrl.movingSplitbar = null;
      }
      return event;
    };

    ctrl.mouseMoveHandler = function(mouseEvent) {
      var mousePos = mouseEvent[ctrl.sizeProperties.mouseProperty] ||
        (mouseEvent.originalEvent && mouseEvent.originalEvent[ctrl.sizeProperties.mouseProperty]) ||
        // jQuery does touches weird, see #82
        ($window.jQuery ?
          (mouseEvent.originalEvent ? mouseEvent.originalEvent.targetTouches[0][ctrl.sizeProperties.mouseProperty] : 0) :
          (mouseEvent.targetTouches ? mouseEvent.targetTouches[0][ctrl.sizeProperties.mouseProperty] : 0));

      lastPos = mousePos - offset($element)[ctrl.sizeProperties.offsetPos];

      //Cancel previous rAF call
      if(animationFrameRequested) {
        window.cancelAnimationFrame(animationFrameRequested);
      }

      //TODO: cache layout values

      //Animate the page outside the event
      animationFrameRequested = window.requestAnimationFrame(draw);
    };

    /**
     * Returns the min and max values of the ctrl.containers on each side of the container submitted
     * @param container
     * @returns {*}
     */
    ctrl.processSplitbar = function(container) {
      var index = ctrl.containers.indexOf(container);

      var setValues = function(container) {
        var start = container[ctrl.sizeProperties.flowProperty];
        var end = container[ctrl.sizeProperties.flowProperty] + container.size;

        container.beforeMinValue = angular.isNumber(container.minSize) ? start + container.minSize : start;
        container.beforeMaxValue = angular.isNumber(container.maxSize) ? start + container.maxSize : null;

        container.afterMinValue = angular.isNumber(container.minSize) ? end - container.minSize : end;
        container.afterMaxValue = angular.isNumber(container.maxSize) ? end - container.maxSize : null;
      };

      //verify the container was found in the list
      if(index > -1) {
        var beforeContainer = (index > 0) ? ctrl.containers[index-1] : null;
        var afterContainer = ((index+1) <= ctrl.containers.length) ? ctrl.containers[index+1] : null;

        if(beforeContainer !== null) setValues(beforeContainer);
        if(afterContainer !== null) setValues(afterContainer);

        return {
          beforeContainer: beforeContainer,
          afterContainer: afterContainer
        };
      }

      return null;
    };

    /**
     * Checks if a string has a percent symbol in it.
     * @param num
     * @returns {boolean}
     */
    ctrl.isPercent = function(num) {
      return (num && angular.isString(num) && num.indexOf('%') > -1) ? true : false;
    };

    /**
     * Converts a number to pixels from percent.
     * @param size
     * @param parentSize
     * @returns {number}
     */
    ctrl.convertToPixels = function(size, parentSize) {
      return Math.floor(parentSize * (parseInt(size) / 100));
    };

    /**
     * Sets the default size for each container.
     */
    ctrl.calculate = function() {
      var c, i;
      var dividerSize = parseInt(opts.dividerSize);
      var elementSize = $element[0].getBoundingClientRect()[ctrl.sizeProperties.sizeProperty];
      var availableSize = elementSize - (dividerSize * numOfSplitbars);
      var originalSize = availableSize;
      var usedSpace = 0;
      var numOfAutoContainers = 0;
      if(ctrl.containers.length > 0 && $element.children().length > 0) {

        // calculate sizing for ctrl.containers
        for(i=0; i < ctrl.containers.length; i++) {
          if(!LayoutContainer.isSplitbar(ctrl.containers[i])) {

            c = ctrl.containers[i];
            opts.sizes[i] = c.isCentral ? 'auto' : c.collapsed ? (optionValue(c.minSize) || '0px') : optionValue(c.uncollapsedSize) || 'auto';
            opts.minSizes[i] = optionValue(c.minSize);
            opts.maxSizes[i] = optionValue(c.maxSize);

            if(opts.sizes[i] !== 'auto') {
              if(ctrl.isPercent(opts.sizes[i])) {
                opts.sizes[i] = ctrl.convertToPixels(opts.sizes[i], originalSize);
              } else {
                opts.sizes[i] = parseInt(opts.sizes[i]);
              }
            }

            if(opts.minSizes[i] !== null) {
              if(ctrl.isPercent(opts.minSizes[i])) {
                opts.minSizes[i] = ctrl.convertToPixels(opts.minSizes[i], originalSize);
              } else {
                opts.minSizes[i] = parseInt(opts.minSizes[i]);
              }

              // don't allow the container size to initialize smaller than the minSize
              if(!c.collapsed && opts.sizes[i] < opts.minSizes[i]) opts.sizes[i] = opts.minSizes[i];
            }

            if(opts.maxSizes[i] !== null) {
              if(ctrl.isPercent(opts.maxSizes[i])) {
                opts.maxSizes[i] = ctrl.convertToPixels(opts.maxSizes[i], originalSize);
              } else {
                opts.maxSizes[i] = parseInt(opts.maxSizes[i]);
              }

              // don't allow the container size to intialize larger than the maxSize
              if(opts.sizes[i] > opts.maxSizes[i]) opts.sizes[i] = opts.maxSizes[i];
            }

            if(opts.sizes[i] === 'auto') {
              numOfAutoContainers++;
            } else {
              availableSize -= opts.sizes[i];
            }
          }
        }

        // FIXME: autoSize if frequently Infinity, since numOfAutoContainers is frequently 0, no need to calculate that
        // set the sizing for the ctrl.containers
        /*
         * When the parent size is odd, rounding down the `autoSize` leaves a remainder.
         * This remainder is added to the last auto-sized container in a layout.
         */
        var autoSize = Math.floor(availableSize / numOfAutoContainers),
          remainder = availableSize - autoSize * numOfAutoContainers;
        for(i=0; i < ctrl.containers.length; i++) {
          c = ctrl.containers[i];
          c[ctrl.sizeProperties.flowProperty] = usedSpace;
          c.maxSize = opts.maxSizes[i];
          c.minSize = opts.minSizes[i];

          //TODO: adjust size if autosize is greater than the maxSize

          if(!LayoutContainer.isSplitbar(c)) {
            var newSize;
            if(opts.sizes[i] === 'auto') {
              newSize = autoSize;
              // add the rounding down remainder to the last auto-sized container in the layout
              if (remainder > 0 && i === ctrl.containers.length - 1) {
                newSize += remainder;
              }
            } else {
              newSize = opts.sizes[i];
            }

            c.size = (newSize !== null) ? newSize : autoSize;
          } else {
            c.size = dividerSize;
          }

          usedSpace += c.size;
        }
      }
    };

    /**
     * Adds a container to the list of layout ctrl.containers.
     * @param container The container to add
     */
    ctrl.addContainer = function(container) {
      var index = ctrl.indexOfElement(container.element);
      if(!angular.isDefined(index) || index < 0 || ctrl.containers.length < index) {
        console.error("Invalid index to add container; i=" + index + ", len=", ctrl.containers.length);
        return;
      }

      if(LayoutContainer.isSplitbar(container)) {
        numOfSplitbars++;
      }

      container.index = index;
      ctrl.containers.splice(index, 0, container);

      ctrl.calculate();
    };

    /**
     * Remove a container from the list of layout ctrl.containers.
     * @param  container
     */
    ctrl.removeContainer = function(container) {
      var index = ctrl.containers.indexOf(container);
      if(index >= 0) {
        if(!LayoutContainer.isSplitbar(container)) {
          if(ctrl.containers.length > 2) {
            // Assume there's a sidebar between each container
            // We need to remove this container and the sidebar next to it
            if(index == ctrl.containers.length - 1) {
              // We're removing the last element, the side bar is on the left
              ctrl.containers[index-1].element.remove();
            } else {
              // The side bar is on the right
              ctrl.containers[index+1].element.remove();
            }
          }
        } else {
          // fix for potentially collapsed containers
          ctrl.containers[index - 1].collapsed = false;
          numOfSplitbars--;
        }

        // Need to re-check the index, as a side bar may have been removed
        var newIndex = ctrl.containers.indexOf(container);
        if(newIndex >= 0) {
          ctrl.containers.splice(newIndex, 1);
          ctrl.opts.maxSizes.splice(newIndex, 1);
          ctrl.opts.minSizes.splice(newIndex, 1);
          ctrl.opts.sizes.splice(newIndex, 1);
        }
        ctrl.calculate();
      } else {
        console.error("removeContainer for container that did not exist!");
      }
    };

    /**
     * Returns an array of layout ctrl.containers.
     * @returns {Array}
     */
    ctrl.getContainers = function() {
      return ctrl.containers;
    };

    ctrl.toggleContainer = function(index) {

      var splitter = ctrl.containers[index + 1],
        el;

      if (splitter) {
        el = splitter.element[0].children[0];
      } else {
        splitter = ctrl.containers[index - 1];
        el = splitter.element[0].children[1];
      }

      $timeout(function(){
        angular.element(el).triggerHandler('click');
      });
    };

    /**
     * Toggles the container before the provided splitbar
     * @param splitbar
     * @returns {boolean|*|Array}
     */
    ctrl.toggleBefore = function(splitbar) {
      var index = ctrl.containers.indexOf(splitbar) - 1;

      var c = ctrl.containers[index];
      c.collapsed = !ctrl.containers[index].collapsed;

      var nextSplitbar = ctrl.containers[index+1];
      var nextContainer = ctrl.containers[index+2];

      // uncollapsedSize is undefined in case of 'auto' sized containers.
      // Perhaps there's a place where we could set... could find it though. @see also toggleBefore
      if (c.uncollapsedSize === undefined) {
        c.uncollapsedSize = c.size;
      } else {
        c.uncollapsedSize = parseInt(c.uncollapsedSize);
      }
      // FIXME: collapse:resize:uncollapse: works well "visually" without the nextSplitbar and nextContainer calculations
      // but removing those breaks few test
      $scope.$apply(function() {
        if(c.collapsed) {

          c.size = 0;

          if(nextSplitbar) nextSplitbar[ctrl.sizeProperties.flowProperty] -= c.uncollapsedSize;
          if(nextContainer) {
            nextContainer[ctrl.sizeProperties.flowProperty] -= c.uncollapsedSize;
            nextContainer.uncollapsedSize += c.uncollapsedSize;
          }

        } else {
          c.size = c.uncollapsedSize;

          if(nextSplitbar) nextSplitbar[ctrl.sizeProperties.flowProperty] += c.uncollapsedSize;
          if(nextContainer) {
            nextContainer[ctrl.sizeProperties.flowProperty] += c.uncollapsedSize;
            nextContainer.uncollapsedSize -= c.uncollapsedSize;
          }
        }
      });
      $scope.$broadcast('ui.layout.toggle', c);
      Layout.toggled();

      return c.collapsed;
    };


    /**
     * Toggles the container after the provided splitbar
     * @param splitbar
     * @returns {boolean|*|Array}
     */
    ctrl.toggleAfter = function(splitbar) {
      var index = ctrl.containers.indexOf(splitbar) + 1;
      var c = ctrl.containers[index];
      var prevSplitbar = ctrl.containers[index-1];
      var prevContainer = ctrl.containers[index-2];
      var isLastContainer = index === (ctrl.containers.length - 1);
      var endDiff;
      var flowProperty = ctrl.sizeProperties.flowProperty;
      var sizeProperty = ctrl.sizeProperties.sizeProperty;

      ctrl.bounds = $element[0].getBoundingClientRect();

      c.collapsed = !ctrl.containers[index].collapsed;

      // uncollapsedSize is undefined in case of 'auto' sized containers.
      // Perhaps there's a place where we could set... could find it though. @see also toggleBefore
      if (c.uncollapsedSize === undefined) {
        c.uncollapsedSize = c.size;
      } else {
        c.uncollapsedSize = parseInt(c.uncollapsedSize);
      }

      // FIXME: collapse:resize:uncollapse: works well "visually" without the prevSplitbar and prevContainer calculations
      // but removing those breaks few test
      $scope.$apply(function() {
        if(c.collapsed) {

          c.size = 0;

          // adds additional space so the splitbar moves to the very end of the container
          // to offset the lost space when converting from percents to pixels
          endDiff = (isLastContainer) ? ctrl.bounds[sizeProperty] - c[flowProperty] - c.uncollapsedSize : 0;

          if(prevSplitbar) {
            prevSplitbar[flowProperty] += (c.uncollapsedSize + endDiff);
          }
          if(prevContainer) {
            prevContainer.size += (c.uncollapsedSize + endDiff);
          }

        } else {
          c.size = c.uncollapsedSize;

          // adds additional space so the splitbar moves back to the proper position
          // to offset the additional space added when collapsing
          endDiff = (isLastContainer) ? ctrl.bounds[sizeProperty] - c[flowProperty] - c.uncollapsedSize : 0;

          if(prevSplitbar) {
            prevSplitbar[flowProperty] -= (c.uncollapsedSize + endDiff);
          }
          if(prevContainer) {
            prevContainer.size -= (c.uncollapsedSize + endDiff);
          }
        }
      });
      $scope.$broadcast('ui.layout.toggle', c);
      Layout.toggled();
      return c.collapsed;
    };

    /**
     * Returns the container object of the splitbar that is before the one passed in.
     * @param currentSplitbar
     */
    ctrl.getPreviousSplitbarContainer = function(currentSplitbar) {
      if(LayoutContainer.isSplitbar(currentSplitbar)) {
        var currentSplitbarIndex = ctrl.containers.indexOf(currentSplitbar);
        var previousSplitbarIndex = currentSplitbarIndex - 2;
        if(previousSplitbarIndex >= 0) {
          return ctrl.containers[previousSplitbarIndex];
        }
        return null;
      }
      return null;
    };

    /**
     * Returns the container object of the splitbar that is after the one passed in.
     * @param currentSplitbar
     */
    ctrl.getNextSplitbarContainer = function(currentSplitbar) {
      if(LayoutContainer.isSplitbar(currentSplitbar)) {
        var currentSplitbarIndex = ctrl.containers.indexOf(currentSplitbar);
        var nextSplitbarIndex = currentSplitbarIndex + 2;
        if(currentSplitbarIndex > 0 && nextSplitbarIndex < ctrl.containers.length) {
          return ctrl.containers[nextSplitbarIndex];
        }
        return null;
      }
      return null;
    };

    /**
     * Checks whether the container before this one is a split bar
     * @param  {container}  container The container to check
     * @return {Boolean}    true if the element before is a splitbar, false otherwise
     */
    ctrl.hasSplitbarBefore = function(container) {
      var index = ctrl.containers.indexOf(container);
      if(1 <= index) {
        return LayoutContainer.isSplitbar(ctrl.containers[index-1]);
      }

      return false;
    };

    /**
     * Checks whether the container after this one is a split bar
     * @param  {container}  container The container to check
     * @return {Boolean}    true if the element after is a splitbar, false otherwise
     */
    ctrl.hasSplitbarAfter = function(container) {
      var index = ctrl.containers.indexOf(container);
      if(index < ctrl.containers.length - 1) {
        return LayoutContainer.isSplitbar(ctrl.containers[index+1]);
      }

      return false;
    };

    /**
     * Checks whether the passed in element is a ui-layout type element.
     * @param  {element}  element The element to check
     * @return {Boolean}          true if the element is a layout element, false otherwise.
     */
    ctrl.isLayoutElement = function(element) {
      return element.hasAttribute('ui-layout-container') ||
        element.hasAttribute('ui-splitbar') ||
        element.nodeName === 'UI-LAYOUT-CONTAINER';
    };

    /**
     * Retrieve the index of an element within it's parents context.
     * @param  {element} element The element to get the index of
     * @return {int}             The index of the element within it's parent
     */
    ctrl.indexOfElement = function(element) {
      var parent = element.parent();
      var children = parent.children();
      var containerIndex = 0;
      for(var i = 0; i < children.length; i++) {
        var child = children[i];
        if(ctrl.isLayoutElement(child)) {
          if(element[0] == children[i]) {
            return containerIndex;
          }
          containerIndex++;
        }
      }
      return -1;
    };

    return ctrl;
  }])

  .directive('uiLayout', ['$window', function($window) {
    return {
      restrict: 'AE',
      controller: 'uiLayoutCtrl',
      link: function(scope, element, attrs, ctrl) {
        scope.$watch(function () {
          return element[0][ctrl.sizeProperties.offsetSize];
        }, function() {
          ctrl.calculate();
        });

        function onResize() {
          scope.$evalAsync(function() {
            ctrl.calculate();
          });
        }

        angular.element($window).bind('resize', onResize);

        scope.$on('$destroy', function() {
          angular.element($window).unbind('resize', onResize);
        });
      }
    };
  }])

  .directive('uiSplitbar', ['LayoutContainer', function(LayoutContainer) {
    // Get all the page.
    var htmlElement = angular.element(document.body.parentElement);

    return {
      restrict: 'EAC',
      require: '^uiLayout',
      scope: {},

      link: function(scope, element, attrs, ctrl) {
        if(!element.hasClass('stretch')) element.addClass('stretch');
        if(!element.hasClass('ui-splitbar')) element.addClass('ui-splitbar');

        var animationClass = ctrl.isUsingColumnFlow ? 'animate-column' : 'animate-row';
        element.addClass(animationClass);

        scope.splitbar = LayoutContainer.Splitbar();
        scope.splitbar.element = element;

        //icon <a> elements
        var prevButton = angular.element(element.children()[0]);
        var afterButton = angular.element(element.children()[1]);

        //icon <span> elements
        var prevIcon = angular.element(prevButton.children()[0]);
        var afterIcon = angular.element(afterButton.children()[0]);

        //icon classes
        var iconLeft = 'ui-splitbar-icon-left';
        var iconRight = 'ui-splitbar-icon-right';
        var iconUp = 'ui-splitbar-icon-up';
        var iconDown = 'ui-splitbar-icon-down';

        var prevIconClass = ctrl.isUsingColumnFlow ? iconLeft : iconUp;
        var afterIconClass = ctrl.isUsingColumnFlow ? iconRight : iconDown;

        prevIcon.addClass(prevIconClass);
        afterIcon.addClass(afterIconClass);


        prevButton.on('click', function() {
          var prevSplitbarBeforeButton, prevSplitbarAfterButton;
          var result = ctrl.toggleBefore(scope.splitbar);
          var previousSplitbar = ctrl.getPreviousSplitbarContainer(scope.splitbar);

          if(previousSplitbar !== null) {
            prevSplitbarBeforeButton = angular.element(previousSplitbar.element.children()[0]);
            prevSplitbarAfterButton = angular.element(previousSplitbar.element.children()[1]);
          }

          if(ctrl.isUsingColumnFlow) {
            if(result) {
              afterButton.css('display', 'none');
              prevIcon.removeClass(iconLeft);
              prevIcon.addClass(iconRight);

              // hide previous splitbar buttons
              if(previousSplitbar !== null) {
                prevSplitbarBeforeButton.css('display', 'none');
                prevSplitbarAfterButton.css('display', 'none');
              }
            } else {
              afterButton.css('display', 'inline');
              prevIcon.removeClass(iconRight);
              prevIcon.addClass(iconLeft);

              // show previous splitbar icons
              if(previousSplitbar !== null) {
                prevSplitbarBeforeButton.css('display', 'inline');
                prevSplitbarAfterButton.css('display', 'inline');
              }
            }
          } else {
            if(result) {
              afterButton.css('display', 'none');
              prevIcon.removeClass(iconUp);
              prevIcon.addClass(iconDown);

              // hide previous splitbar buttons
              if(previousSplitbar !== null) {
                prevSplitbarBeforeButton.css('display', 'none');
                prevSplitbarAfterButton.css('display', 'none');
              }
            } else {
              afterButton.css('display', 'inline');
              prevIcon.removeClass(iconDown);
              prevIcon.addClass(iconUp);

              // show previous splitbar icons
              if(previousSplitbar !== null) {
                prevSplitbarBeforeButton.css('display', 'inline');
                prevSplitbarAfterButton.css('display', 'inline');
              }
            }
          }
          scope.$evalAsync(function() {
            ctrl.calculate();
          });
        });

        afterButton.on('click', function() {
          var nextSplitbarBeforeButton, nextSplitbarAfterButton;
          var result = ctrl.toggleAfter(scope.splitbar);
          var nextSplitbar = ctrl.getNextSplitbarContainer(scope.splitbar);

          if(nextSplitbar !== null) {
            nextSplitbarBeforeButton = angular.element(nextSplitbar.element.children()[0]);
            nextSplitbarAfterButton = angular.element(nextSplitbar.element.children()[1]);
          }

          if(ctrl.isUsingColumnFlow) {
            if(result) {
              prevButton.css('display', 'none');
              afterIcon.removeClass(iconRight);
              afterIcon.addClass(iconLeft);

              // hide next splitbar buttons
              if(nextSplitbar !== null) {
                nextSplitbarBeforeButton.css('display', 'none');
                nextSplitbarAfterButton.css('display', 'none');
              }
            } else {
              prevButton.css('display', 'inline');
              afterIcon.removeClass(iconLeft);
              afterIcon.addClass(iconRight);

              // show next splitbar buttons
              if(nextSplitbar !== null) {
                nextSplitbarBeforeButton.css('display', 'inline');
                nextSplitbarAfterButton.css('display', 'inline');
              }
            }
          } else {
            if(result) {
              prevButton.css('display', 'none');
              afterIcon.removeClass(iconDown);
              afterIcon.addClass(iconUp);

              // hide next splitbar buttons
              if(nextSplitbar !== null) {
                nextSplitbarBeforeButton.css('display', 'none');
                nextSplitbarAfterButton.css('display', 'none');
              }
            } else {
              prevButton.css('display', 'inline');
              afterIcon.removeClass(iconUp);
              afterIcon.addClass(iconDown);

              // show next splitbar buttons
              if(nextSplitbar !== null) {
                nextSplitbarBeforeButton.css('display', 'inline');
                nextSplitbarAfterButton.css('display', 'inline');
              }
            }
          }
          scope.$evalAsync(function() {
            ctrl.calculate();
          });
        });

        element.on('mousedown touchstart', function(e) {
          if (e.button === 0 || e.type === 'touchstart') {
            // only trigger when left mouse button is pressed:
            ctrl.movingSplitbar = scope.splitbar;
            ctrl.processSplitbar(scope.splitbar);

            e.preventDefault();
            e.stopPropagation();

            htmlElement.on('mousemove touchmove', handleMouseMove);
            return false;
          }
        });

        function handleMouseMove(event) {
          scope.$apply(angular.bind(ctrl, ctrl.mouseMoveHandler, event));
        }

        function handleMouseUp(event) {
          scope.$apply(angular.bind(ctrl, ctrl.mouseUpHandler, event));
          htmlElement.off('mousemove touchmove', handleMouseMove);
        }

        htmlElement.on('mouseup touchend', handleMouseUp);

        scope.$watch('splitbar.size', function(newValue) {
          element.css(ctrl.sizeProperties.sizeProperty, newValue + 'px');
        });

        scope.$watch('splitbar.' + ctrl.sizeProperties.flowProperty, function(newValue) {
          element.css(ctrl.sizeProperties.flowProperty, newValue + 'px');
        });

        //Add splitbar to layout container list
        ctrl.addContainer(scope.splitbar);

        element.on('$destroy', function() {
          ctrl.removeContainer(scope.splitbar);
          htmlElement.off('mouseup touchend', handleMouseUp);
          htmlElement.off('mousemove touchmove', handleMouseMove);
          scope.$evalAsync();
        });
      }
    };

  }])

  .directive('uiLayoutContainer',
    ['LayoutContainer', '$compile', '$timeout', 'Layout',
      function(LayoutContainer, $compile, $timeout, Layout) {
        return {
          restrict: 'AE',
          require: '^uiLayout',
          scope: {
            collapsed: '=',
            resizable: '=',
            size: '@',
            minSize: '@',
            maxSize: '@'
          },

          compile: function() {
            return {
              pre: function(scope, element, attrs, ctrl) {

                scope.container = LayoutContainer.Container();
                scope.container.element = element;
                scope.container.id = element.attr('id') || null;
                scope.container.layoutId = ctrl.id;
                scope.container.isCentral = attrs.uiLayoutContainer === 'central';

                if (scope.collapsed === true) {
                  scope.collapsed = false;
                  Layout.addCollapsed(scope.container);
                }
                // FIXME: collapsed: @see uiLayoutLoaded for explanation
                //if (angular.isDefined(scope.collapsed)) {
                //  scope.container.collapsed = scope.collapsed;
                //}

                if (angular.isDefined(scope.resizable)) {
                  scope.container.resizable = scope.resizable;
                }
                scope.container.size = scope.size;
                scope.container.uncollapsedSize = scope.size;
                scope.container.minSize = scope.minSize;
                scope.container.maxSize = scope.maxSize;
                ctrl.addContainer(scope.container);

                element.on('$destroy', function() {
                  ctrl.removeContainer(scope.container);
                  scope.$evalAsync();
                });
              },
              post: function(scope, element, attrs, ctrl) {
                if(!element.hasClass('stretch')) element.addClass('stretch');
                if(!element.hasClass('ui-layout-container')) element.addClass('ui-layout-container');

                var animationClass = ctrl.isUsingColumnFlow ? 'animate-column' : 'animate-row';
                element.addClass(animationClass);

                scope.$watch('collapsed', function (val, old) {
                  if (angular.isDefined(old) && val !== old) {
                    ctrl.toggleContainer(scope.container.index);
                  }
                });

                scope.$watch('container.size', function(newValue) {
                  element.css(ctrl.sizeProperties.sizeProperty, newValue + 'px');
                  if(newValue === 0) {
                    element.addClass('ui-layout-hidden');
                  } else {
                    element.removeClass('ui-layout-hidden');
                  }
                });

                scope.$watch('container.' + ctrl.sizeProperties.flowProperty, function(newValue) {
                  element.css(ctrl.sizeProperties.flowProperty, newValue + 'px');
                });

                //TODO: add ability to disable auto-adding a splitbar after the container
                var parent = element.parent();
                var children = parent.children();
                var index = ctrl.indexOfElement(element);
                var splitbar = angular.element('<div ui-splitbar>' +
                  '<a><span class="ui-splitbar-icon"></span></a>' +
                  '<a><span class="ui-splitbar-icon"></span></a>' +
                  '</div>');
                if(0 < index && !ctrl.hasSplitbarBefore(scope.container)) {
                  angular.element(children[index-1]).after(splitbar);
                  $compile(splitbar)(scope);
                } else if(index < children.length - 1) {
                  element.after(splitbar);
                  $compile(splitbar)(scope);
                }
              }
            };
          }
        };
      }])

  .directive('uiLayoutLoaded', ['$timeout', 'Layout', function($timeout, Layout) {
    // Currently necessary for programmatic toggling to work with "initially" collapsed containers,
    // because prog. toggling depends on the logic of prevButton and nextButton (which should be probably refactored out)
    //
    // This is how it currently works:
    // 1. uiLayoutContainer in prelink phase resets @collapsed to false, because layout has to be calculated
    //    with all containers uncollapsed to get the correct dimensions
    // 2. layout with ui-layout-loaded attributes broadcasts "ui.layout.loaded"
    // 3. user changes values of @collapsed which triggers 'click' on either of the buttons
    // 3. the other button is hidden and container size set to 0
    return {
      require: '^uiLayout',
      restrict: 'A',
      priority: -100,
      link: function($scope, el, attrs){

        // negation is safe here, because we are expecting non-empty string
        if (!attrs['uiLayoutLoaded']) {
          Layout.toggle().then(
            function(){
              $scope.$broadcast('ui.layout.loaded', null);
            }
          );
        } else {
          $scope.$broadcast('ui.layout.loaded',  attrs['uiLayoutLoaded']);
        }
      }
    };
  }])

  .factory('Layout', ['$q', '$timeout', function($q, $timeout) {
    var layouts = [],
      collapsing = [],
      toBeCollapsed = 0,
      toggledDeffered =  null;

    function toggleContainer(container) {
      try {
        layouts[container.layoutId].toggleContainer(container.index);
      } catch (e) {
        e.message = 'Could not toggle container [' + container.layoutId + '/' + container.index + ']: ' + e.message;
        throw e;
      }
    }

    return {
      addLayout: function (ctrl) {
        ctrl.id = layouts.length;
        layouts.push(ctrl);
      },
      addCollapsed: function(container) {
        collapsing.push(container);
      },
      hasCollapsed: function() {
        return collapsing.length > 0;
      },
      toggled: function() {
        // event already dispatched, do nothing
        if (toBeCollapsed === 0) {
          if (toggledDeffered) {
            toggledDeffered.reject();
          } else {
            return false;
          }
        }
        toBeCollapsed--;
        if (toBeCollapsed === 0) {
          toggledDeffered.resolve();
        }
      },
      toggle: function() {
        toggledDeffered = $q.defer();
        toBeCollapsed = collapsing.length;
        if (toBeCollapsed === 0) {
          $timeout(function(){
            toggledDeffered.resolve();
          });
        }
        collapsing.reverse();
        var c;
        while(c = collapsing.pop()) {
          toggleContainer(c);
        }
        return toggledDeffered.promise;
      }
    };
  }])

  .factory('LayoutContainer', function() {
    function BaseContainer() {

      /**
       * Stores element's id if provided
       * @type {string}
       */
      this.id = null;

      /**
       * Id of the parent layout
       * @type {number}
       */
      this.layoutId = null;

      /**
       * Central container that is always resized
       * @type {boolean}
       */
      this.isCentral = false;

      /**
       * actual size of the container, which is changed on every updateDisplay
       * @type {any}
       */
      this.size = 'auto';

      /**
       * cache for the last uncollapsed size
       * @type {any}
       */
      this.uncollapsedSize = null;

      this.maxSize = null;
      this.minSize = null;
      this.resizable = true;
      this.element = null;
      this.collapsed = false;
    }

    // Splitbar container
    function SplitbarContainer() {
      this.size = 10;
      this.left = 0;
      this.top = 0;
      this.element = null;
    }

    return {
      Container: function(initialSize) {
        return new BaseContainer(initialSize);
      },
      Splitbar: function() {
        return new SplitbarContainer();
      },
      isSplitbar: function(container) {
        return container instanceof SplitbarContainer;
      }
    };
  })
;
