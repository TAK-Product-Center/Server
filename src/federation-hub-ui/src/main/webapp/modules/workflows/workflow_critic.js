
"use strict";
//We may want to run this script severside with out complications.
//Therefore use only native Javascript with no reliance on 3rd party libraries.
var criticResults = [];
var addCritic = function(level, message) {
  criticResults.push(level + " - " + message);
  // criticResults.push({
  //   level: level,
  //   message: message
  // });
};
var addWarning = function(message) {
  addCritic("warning", message);
};
var addFatal = function(message) {
  addCritic("fatal", message);
};


function mustBelongToPool(cell) {
  if (cell.roger_federation.pool === undefined) {
    if (cell.roger_federation.name !== undefined) { //Not all shapes have a name
      addWarning("BPMN shape (" + cell.roger_federation.name + ") must belong to a pool.");
    } else {
      addWarning("BPMN shape (" + cell.roger_federation.type + ") must belong to a pool.");
    }
  }
}


function workflowCritic(wf) {
  console.log("Run Critic");
  criticResults = [];
  var hasStartEvent = false;
  var hasEndEvent = false;
  if (wf.diagramType === "Workflow") {
    for (var i = 0; i < wf.cells.length; i++) {
      var cell = wf.cells[i];

      if (cell.roger_federation.type === "Signal Start Event")  {
        mustBelongToPool(cell); //Start events must belong to a pool
        hasStartEvent = true;
      }
      hasEndEvent = (cell.roger_federation.type === "Signal End Event" || cell.roger_federation.type === "End Event" ? true : hasEndEvent);
    }

    if (!hasStartEvent) {
      addWarning("BPMN Diagram needs at least one start event.");
    }
    if (!hasEndEvent) {
      addWarning("BPMN Diagram needs an end event.");
    }
  } else if (wf.diagramType === "Federation") {
    //TODO Add Federation critics
  }
  return criticResults;
}
