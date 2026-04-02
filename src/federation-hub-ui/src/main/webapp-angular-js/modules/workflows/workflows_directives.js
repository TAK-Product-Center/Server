
angular.module('roger_federation.Workflows')

.directive('jointDiagram', [function () {

    var directive = {
	    link: link,
	    restrict: 'E',
	    scope: {
		height: '=',
		width: '=',
		gridSize: '=',
		graph: '='
	    }
    };

    return directive;

    function link(scope, element, attrs) {

	var diagram = newDiagram(scope.height, scope.width, scope.gridSize, scope.graph, element[0]);

	//add event handlers to interact with the diagram
	diagram.on('cell:pointerclick', function (cellView, evt, x, y) {
	    //your logic here e.g. select the element
	    $log.debug('link cell view ' + cellView.model.id + ' was clicked');
	});

	//add event handlers to interact with the diagram
	diagram.on('cell:pointerup', function (cellView, evt, x, y) {
	    //your logic here e.g. select the element
	    $log.debug('link cell view pointer up: ' + cellView.model.id);
	});

	//add event handlers to interact with the diagram
	diagram.on('cell:pointerdown', function (cellView, evt, x, y) {
	    //your logic here e.g. select the element
	    $log.debug('link cell view pointer down: ' + cellView.model.id );
	});

	diagram.on('blank:pointerclick', function (evt, x, y) {
	    // your logic here e.g. unselect the element by clicking on a blank part of the diagram
	});

	diagram.on('link:options', function (evt, cellView, x, y) {
	    // your logic here: e.g. select a link by its options tool
	});
    }

    function newDiagram(height, width, gridSize, graph, targetElement) {

	var paper = new joint.dia.Paper({
	    el: targetElement,
	    width: width,
	    height: height,
	    gridSize: gridSize,
	    model: graph
	});

	return paper;
    }

}]);


