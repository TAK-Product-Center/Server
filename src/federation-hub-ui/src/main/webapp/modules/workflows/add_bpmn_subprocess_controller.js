
"use strict";

angular.module('roger_federation.Workflows')
  .controller('AddBPMNSubProcessController', ['$scope', '$modalInstance', '$stateParams', '$log', 'growl', 'WorkflowService', 'JointPaper', addBPMNSubProcessController]);

function addBPMNSubProcessController($scope, $modalInstance, $stateParams, $log, growl, WorkflowService, JointPaper) {

  $scope.editorTitle = "Add";
  $scope.editExisting = false;

  $scope.rowCollection = [];
  $scope.displayedCollection = [];
  $scope.itemsByPage = 15;
  $scope.selectedRowId = -1;
  var cellView;

  $scope.initialize = function() {
    cellView = JointPaper.inpector.options.cellView;
    $scope.roger_federation = cellView.model.attributes.roger_federation;
    if ($scope.roger_federation.name !== "") {
      $scope.editorTitle = "Modify";
      $scope.editExisting = true;
    }

    WorkflowService.getWorkflowDescriptors().then(function(workflowList) {
      $scope.rowCollection = workflowList.filter(function(item) {
        return item.id !== $stateParams.workflowId; //Exclude self from the list
      });
      $scope.displayedCollection = [].concat($scope.rowCollection);
    }, function(result) {
      growl.error("Failed getting workflow names. Error: " + result.data.error);
      $scope.rowCollection.length = 0;
      $scope.displayedCollection = [].concat($scope.rowCollection);
    });
  };


  //fires when table rows are selected
  $scope.$watch('displayedCollection', function(row) {
    $scope.selectedRowId = -1;
    //get selected row
    row.filter(function(r) {
      if (r.isSelected) {
        $scope.selectedRowId = r.id;
      }
    });
  }, true);

  $scope.diagramTypeFilter = function(diagram) {
    return diagram.diagramType === "Workflow";
  };

  $scope.submit = function() {
    var selectedWorkflow = $scope.displayedCollection.filter(function(row) {
      return row.id === $scope.selectedRowId;
    })[0];

    if (JointPaper.inpector !== undefined) {
      var shapeLabel = joint.util.breakText(selectedWorkflow.name, {
        width: JointPaper.options.maxLabelWidth
      });

      var priority = cellView.model.attributes.roger_federation.priority;
      if (priority !== undefined && priority > 0) {
        shapeLabel += "\n\nPriority: " + priority + "";
      }

      if ($scope.roger_federation.type === "Group") {
        injectDiagramIntoGroupCell($scope.selectedRowId);
      } else { //Sub-Process
        cellView.model.set('content', shapeLabel);
      }

      cellView.model.attributes.roger_federation.diagramId = selectedWorkflow.id;
      cellView.model.attributes.roger_federation.name = selectedWorkflow.name;
      cellView.resize(); //Sometimes the label wraps when it shouldn't.  This seems to fix it.
    }
    $modalInstance.close('ok');
  };

  $scope.cancel = function() {
    if (!$scope.editExisting && JointPaper.inpector !== undefined) {
      JointPaper.inpector.options.cellView.model.remove();
    }
    $modalInstance.dismiss('cancel');
  };


    function injectDiagramIntoGroupCell(diagramId) {
      WorkflowService.getBpmnGraph(diagramId).then(function(diagram) {
        cellView.model.attributes.attrs['.label'].text = diagram.name;
        //Pull cells from selected diagram and embed them in the current one
        var tempGraph = new joint.dia.Graph().fromJSON({
          cells: diagram.cells
        }, {
          sort: !1
        });

        //Resize the Group to fit all the cells
        var bbox = tempGraph.getBBox(tempGraph.getElements());
        var groupCell = JointPaper.graph.getCell(cellView.model.id);
        var originalgroupCellPostion = groupCell.attributes.position;
        var pad1 = 50;
        var pad2 = pad1 * 2;
        groupCell.position(bbox.x - pad1, bbox.y - pad1);
        groupCell.resize(bbox.width + pad2, bbox.height + pad2);

        //Gives all the cells new UUIDs
        var clonedSubgraph = tempGraph.cloneSubgraph(tempGraph.getCells(), {
          deep: true
        });

        // JointPaper.commandManager.initBatchCommand(); //Store everything as a single undo command. TODO: does not work properly, possible bug in jointjs

        //Reposition the Group Cell back to where the user dropped it.
        var deltaX = groupCell.attributes.position.x - originalgroupCellPostion.x;
        var deltaY = groupCell.attributes.position.y - originalgroupCellPostion.y;
        groupCell.position(originalgroupCellPostion.x, originalgroupCellPostion.y);

        tempGraph.getCells().forEach(function(cell) {
          var clonedCell = clonedSubgraph[cell.id];
          JointPaper.graph.addCell(clonedCell);

          //Reposition the Cell relative to the groupCell postion
          if (clonedCell.attributes.position !== undefined) { //Links do not have a postion
            clonedCell.position(clonedCell.attributes.position.x - deltaX, clonedCell.attributes.position.y - deltaY);
          }
          if (cell.attributes.parent === undefined) { //Check if cell is already embedded in another cell
            groupCell.embed(clonedCell);
          }
        });

      }, function() {
        growl.error("Failed to obtain diagram with id: " + diagramId);
      });

    }
}
