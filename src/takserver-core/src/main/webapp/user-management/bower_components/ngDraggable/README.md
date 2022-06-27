ngDraggable
===========

> *NOTE:*
> I'm not actively maintaining this project any more. If any anyone would like to take on that responsible please get in touch


Drag and drop module for Angular JS with support for touch devices. [`demo`](http://htmlpreview.github.io/?https://github.com/fatlinesofcode/ngDraggable/blob/master/example.html).

### Usage:

- Install: `bower install ngDraggable`
- Add `angular` and `ngDraggable` to your code:

```html
<script src="//ajax.googleapis.com/ajax/libs/angularjs/1.3.8/angular.min.js"></script>
<script src="ngDraggable.js"></script>
```

- Add a dependency to the `ngDraggable` module in your application.

```js
angular.module('app', ['ngDraggable']);
```

- Add attribute directives to your html:


Draggable usage:
```html
<div ng-drag="true" ng-drag-data="{obj}" ng-drag-success="onDragComplete($data,$event)" ng-center-anchor="true">
  Draggable div
</div>
```

* `ng-center-anchor` is optional. If not specified, it defaults to false.
* If the draggable is also clickable (ng-click, ng-dblclick) the script wont react.
* You can define a drag-button as child with the attribute `ng-drag-handle`.

```ng-drag-start``` and ```ng-drag-move``` is also available. Add to the ng-drop element.
``ng-drag-stop`` can be used when you want to react to the user dragging an item and it wasn't dropped into the target container.

```draggable:start```, ```draggable:move``` and  ```draggable:end``` events are broadcast on drag actions.


Drop area usage:
```html
<div ng-drop="true" ng-drop-success="onDropComplete($data,$event)" >
  Drop area
</div>
```

### Angular Controller:

```js
app.controller('MainCtrl', function ($scope) {
    $scope.onDragComplete=function(data,evt){
       console.log("drag success, data:", data);
    }
    $scope.onDropComplete=function(data,evt){
        console.log("drop success, data:", data);
    }
 };
```

## Examples
[`Drag and drop`](http://htmlpreview.github.io/?https://github.com/fatlinesofcode/ngDraggable/blob/master/example.html).

[`Re-ordering`](http://htmlpreview.github.io/?https://github.com/fatlinesofcode/ngDraggable/blob/master/example-reorder.html).

[`Cloning`](http://htmlpreview.github.io/?https://github.com/fatlinesofcode/ngDraggable/blob/master/example-clone.html).

[`Canceling`](http://htmlpreview.github.io/?https://github.com/fatlinesofcode/ngDraggable/blob/master/example-cancel.html).

## Pull requests
We welcome pull requests but please check that all the examples still work if you modified the source base. There have been serveral PRs recently that broke core functionality. If you are feeling really keen you could include some protractor test cases in your PR.
