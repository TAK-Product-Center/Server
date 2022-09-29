/* globals joint */
/* globals $ */
'use strict';

angular.module('roger_federation.Workflows')
  .controller('WorkflowsController', ['$scope', '$rootScope', '$window', '$state', '$stateParams', '$timeout', '$uibModal',
    '$log', '$http', 'uuid4', 'growl', 'WorkflowService', 'OntologyService', 'JointPaper', 'SemanticsService', workflowsController
  ]);

function workflowsController($scope, $rootScope, $window, $state, $stateParams, $timeout, $uibModal, $log, $http, uuid4, growl, WorkflowService, OntologyService, JointPaper, SemanticsService) {
  $scope.JointPaper = JointPaper;
  $scope.criticResults = [];

  $scope.data = "{users: 'Joe'}";

  pollActiveConnections()

  function pollActiveConnections() {
    WorkflowService.getActiveConnections().then(function(activeConnections) {
      setOutgoingNodesStatus(activeConnections)
    }).catch(e => setOutgoingNodesStatus([]))
    $timeout(pollActiveConnections, 2000);
  }

  function setOutgoingNodesStatus(activeConnections) {
    if (JointPaper.paper &&  JointPaper.paper._views) {
      var outgoingCellKeys = Object.keys(JointPaper.paper._views);
      for (var i = 0; i < outgoingCellKeys.length; i++) {
        let cellView = JointPaper.paper._views[outgoingCellKeys[i]]

        let text = cellView.model.attributes.roger_federation.stringId +'\n\n' 
                    + cellView.model.attributes.roger_federation.host + ':' + cellView.model.attributes.roger_federation.port

        if (cellView.model.attributes.graphType === "FederationOutgoingCell") {
          let foundConnection = undefined
          activeConnections.forEach(activeConnection => {
            if (activeConnection.connectionId === cellView.model.attributes.roger_federation.name)
              foundConnection = activeConnection
          })
          if (foundConnection) {
            cellView.model.attributes.attrs['.body']['fill'] = 'green'
            text += '\n' + foundConnection.connectionType.toLowerCase()
          } else {
            if (cellView.model.attributes.roger_federation.outgoingEnabled)
              cellView.model.attributes.attrs['.body']['fill'] = 'red'
            else
              cellView.model.attributes.attrs['.body']['fill'] = 'gray'
          }

          var shapeLabel = joint.util.breakText(text, {
              width: 200
          });

          cellView.model.set('content', shapeLabel);

          cellView.update()
          cellView.resize()
        }
      }
    }
  }

  $scope.addSemanticSubscription = function() {
    $state.go('workflows.editor.addSparqlQuery', {
      mode: 'new_request',
      roleProductSetId: $scope.workflow.roleProductSet
    });
  };

  $scope.editSemanticSubscription = function(request) {
    var curScope = this;
    while (curScope.$parent !== null) {
      curScope = curScope.$parent;
    }
    curScope.request = request;
    $state.go("workflows.editor.addSparqlQuery", {
      mode: 'edit_request',
      roleProductSetId: $scope.workflow.roleProductSet
    });
  };

  $scope.deleteSemanticSubscription = function(request) {
    bootbox.confirm("Are you sure you want to delete the semantic subscription (" + request.name + ")?", function(confirm) {
      if (confirm) {
        var index = $scope.workflow.semanticRequests.indexOf(request);
        if (index > -1) {
          $scope.workflow.semanticRequests.splice(index, 1);
          $scope.$apply();
        }
      }
    });
  };

  $scope.displayTemplateContainer = function() {
    var modalInstance = $uibModal.open({
      templateUrl: "views/workflows/display_template_container.html",
      controller: 'TemplateContainerController',
      size: 'lg',
      resolve: {
        data: function() {
          return $scope.data;
        }
      }
    });
  };

  $scope.chooseFederation = function() {
    var modalInstance = $uibModal.open({
      templateUrl: "views/workflows/load_workflow.html",
      controller: 'LoadWorkflowController',
      backdrop: 'static',
      keyboard: false,
      size: 'lg',
      resolve: {
        configParams: function() {
          return {
            diagramType: "Federation",
            mode: "choose"
          };
        }
      }
    });
    modalInstance.result.then(function(federation) {
      $scope.workflow.federationId = federation.id;
      $scope.workflow.federationName = federation.name;
    });
  };
  $scope.editFederation = function() {
    //BUG This will overwrite the diagram navigating away from with the one navigating to.
    //https://github.com/angular-ui/ui-router/issues/2485
    // $state.go('workflows.editor', {
    //   workflowId: $scope.workflow.federationId
    // });
  };
  $scope.removeFederation = function() {
    $scope.workflow.federationId = "";
    $scope.workflow.federationName = "";
  };

  $scope.downloadTemplateContainer = function() {
    $scope.saveGraph().then(function() {
      WorkflowService.downloadTemplateContainer($scope.workflow.id).then(function(result) {
        $scope.data = result;
        $scope.displayTemplateContainer();
      }, function(result) {
        growl.error("Failed to download template container. Error: " + result.statusText);
      });
    });
  };

  $scope.instantiateAndExecute = function() {
    $scope.saveGraph().then(function() {
      WorkflowService.instantiateAndExecute($scope.workflow.id).then(function() {
        //TODO Overlay results on diagram.
      }, function(result) {
        growl.error("Failed to Instantiate and Execute BPMN graph. Error: " + result.statusText);
      });
    });
  };

  $scope.graphToJson = function() {
    var windowFeatures = 'menubar=no,location=no,resizable=yes,scrollbars=yes,status=no';
    var windowName = _.uniqueId('json_output');
    var jsonWindow = window.open('', windowName, windowFeatures);

    jsonWindow.document.write("<html><body><pre>" + angular.toJson(JointPaper.graph.toJSON(), true) + "</pre></body></html>");
  };



  $scope.graphToPNG = function() {
    var pngOptions = {
      // width: 600,
      // height: 600,
      // quality: 0.7
    };
    JointPaper.paper.toPNG(function(imageData) {
      console.log("PNG: " + imageData.length);
      var newWindow = window.open('', 'png_output', 'menubar=no,location=no,resizable=yes,scrollbars=yes,status=no');
      newWindow.document.write("<html><body><img src='" + imageData + "'/></body></html>'");
    }, pngOptions);
  };

  $scope.graphToSVG = function() {
    JointPaper.paper.toSVG(function(imageData) {
      console.log("SVG: " + imageData.length);
      var newWindow = window.open('', 'svg_output', 'menubar=no,location=no,resizable=yes,scrollbars=yes,status=no');
      newWindow.document.write(imageData);
    });
  };

  $scope.policyToJson = function() {
      $scope.saveGraphPromise().then(
          function() {
              WorkflowService.getGraphAsJson($scope.workflow.name).then(function(result) {
                  console.log("Json load complete");
                  $scope.generateJsonWindow(result);
              }, function(result) {
                  growl.error("Failed load JSON from the server. Error: " + result.statusText);
              });
          }, function(error) {
              growl.error("Failed to save federation graph.  The policy may be out of date. Error: " + error.statusText);
          });
  };

 $scope.policyToYaml = function() {
     $scope.saveGraphPromise().then(
         function() {
             WorkflowService.getGraphAsYaml($scope.workflow.name).then(function(result) {
                 console.log("Json load complete");
                 $scope.generateYamlWindow(result);
             }, function(result) {
                 growl.error("Failed load YAML from the server. Error: " + result.statusText);
             });
         }, function(error) {
             growl.error("Failed to save federation graph.  The policy may be out of date. Error: " + error.statusText);
         });
  };

 $scope.policyJsonAsFile = function() {
     $scope.saveGraphPromise().then(
         function() {
             WorkflowService.getGraphAsJson($scope.workflow.name).then(function(result) {
                 console.log("Json load complete");
                 var fileName = $scope.workflow.name + ".json";
                 saveTextAsFile(result, fileName);
             }, function(result) {
                 growl.error("Failed load JSON from the server. Error: " + result.statusText);
             });
         }, function(error) {
             growl.error("Failed to save federation graph.  The policy may be out of date. Error: " + error.statusText);
         });
 }

  function saveTextAsFile (data, filename){

      if(!data) {
          console.error('Console.save: No data')
          return;
      }

      if(!filename) filename = 'console.json'

      var blob = new Blob([data], {type: 'text/plain'}),
          e    = document.createEvent('MouseEvents'),
          a    = document.createElement('a')
// FOR IE:

      if (window.navigator && window.navigator.msSaveOrOpenBlob) {
          window.navigator.msSaveOrOpenBlob(blob, filename);
      }
      else{
          var e = document.createEvent('MouseEvents'),
              a = document.createElement('a');

          a.download = filename;
          a.href = window.URL.createObjectURL(blob);
          a.dataset.downloadurl = ['text/plain', a.download, a.href].join(':');
          e.initEvent('click', true, false, window,
              0, 0, 0, 0, 0, false, false, false, false, 0, null);
          a.dispatchEvent(e);
      }
  };


  $scope.sendToFederationManagerWithoutDownload = function () {
    $scope.sendToFederationManager();
    growl.warning("Making this the active policy does not back up the policy to a file.  Please either save the policy file, or use the send to federation manage + download button.")
  };

   $scope.sendToFederationManager = function() {
    $scope.saveGraphPromise().then(
        function() {
            WorkflowService.sendToFederationManager($scope.workflow.name).then(function(result) {
                growl.success("Successfully sent current policy to federation manager");
                console.log("Successfully sent current policy to federation manager");
            }, function(result) {
                growl.error("Failed to transfer new policy to federation manager " + result.statusText);
            });
        }, function(error) {
            growl.error("Failed to save federation graph.  The policy may be out of date. Error: " + error.statusText);
        });
   };

    $scope.sendToFederationManagerAndFile = function() {
        $scope.saveGraphPromise().then(
            function() {
                WorkflowService.sendToFederationManagerAndFile($scope.workflow.name).then(function(result) {
                    growl.success("Successfully sent current policy to federation manager");
                    console.log("Successfully sent current policy to federation manager");
                }, function(result) {
                    growl.error("Failed to transfer new policy to federation manager " + result.statusText);
                });
            }, function(error) {
                growl.error("Failed to save federation graph.  The policy may be out of date. Error: " + error.statusText);
            });
    };

  $scope.generateYamlWindow = function(result_object) {
    var windowFeatures = 'menubar=no,location=no,resizable=yes,scrollbars=yes,status=no';
    var windowName = _.uniqueId('yaml_output');
    var yamlWindow = window.open('', windowName, windowFeatures);

    yamlWindow.document.write("<html><body><pre>" + result_object + "</pre></body></html>");
  };

  $scope.generateJsonWindow = function(result_object) {
    var newWindow = window.open('', '', 'menubar=no,location=no,resizable=yes,scrollbars=yes,status=no,width=1200,height=1200');
        newWindow.document.write("<html><body><pre>" + angular.toJson(result_object, true) + "</pre></body></html>");
  };

  $scope.zoomToFit = function() {
    JointPaper.paperScroller.zoomToFit({
      padding: 100,
      minScale: 0.2,
      maxScale: 1
    });
  };

  $scope.saveThumbnailAndGraph = function() {
    try {
      var t0 = performance.now();
      if ($scope.workflow !== undefined) {
        var pngOptions = {
          width: 400,
          height: 200
        };
        JointPaper.paper.toPNG(function(imageData) {
          $scope.workflow.thumbnail = imageData;
          console.log("Time to generate thumbnail (" + imageData.length + ") bytes : " + (performance.now() - t0) / 1000 + " seconds.");
          $scope.saveGraph();
        }, pngOptions);
      }
    } catch (ex) {
      growl.error("Failed to save BPMN thumbnail.");
    }
  };

  $scope.saveGraph = function() {
    if ($scope.workflow !== undefined) {
      var graphJSON = JointPaper.graph.toJSON();
      $scope.workflow.cells = graphJSON.cells;
      return WorkflowService.saveBpmnGraph(angular.toJson($scope.workflow)).then(function() {
          console.log("Saved graph");
      }, function(result) {
        growl.error("Failed to save BPMN graph. Error: " + result.statusText);
      });
    } else {
      console.log("WARNING: $scope.workflow is undefined in saveGraph");
    }
  };

  // This function is used instead of saveGraph when we want to execute some other code after the graph is saved
  $scope.saveGraphPromise = function() {
      if ($scope.workflow !== undefined) {
        var graphJSON = JointPaper.graph.toJSON();
        $scope.workflow.cells = graphJSON.cells;
        return WorkflowService.saveBpmnGraph(angular.toJson($scope.workflow))
      } else {
          console.log("WARNING: $scope.workflow is undefined in saveGraph");
      }
  }

  $scope.deleteGraph = function() {
    WorkflowService.deleteBpmnGraph($scope.workflow.id).then(function() {
      JointPaper.graph.clear();
    }, function(result) {
      growl.error("Failed to delete BPMN graph. Error: " + result.statusText);
    });
  };

  function openEditor(cellView) {
    var cellType = cellView.model.attributes.type;
    var propertiesType = cellView.model.attributes.roger_federation.type;

    if (cellType === "bpmn.Flow") {
      if (propertiesType === "Federate Policy") {
        $state.go('workflows.editor.addBPMNFederatePolicy');
      } else {
        $state.go('workflows.editor.addBPMNTransition');
      }
    } else if (cellType === "bpmn.Annotation") {
      JointPaper.autosizeCellView(cellView);
      JointPaper.openInlineEditor(cellView);
    } else {
      if (['Product', 'Role', 'Pool', 'Message', 'Data Store', 'Data Object'].indexOf(propertiesType) !== -1) {
        $state.go('workflows.editor.addRoleProduct', {
          roleProductSetId: $scope.workflow.roleProductSet
        });
      } else if (['Federate'].indexOf(propertiesType) !== -1) {
        $state.go('workflows.editor.addBPMNFederate');
      } else if (['Group'].indexOf(propertiesType) !== -1) {
        $state.go('workflows.editor.addFederateGroup');
      } else if (['FederationOutgoing'].indexOf(propertiesType) !== -1) {
        $state.go('workflows.editor.addFederationOutgoing');
      }
    }
  }

  function openTools(cellView) {
    var type = cellView.model.get('type');
    // No need to re-render inspector if the cellView didn't change.
    if (!JointPaper.inpector || JointPaper.inpector.options.cellView !== cellView) {
      if (JointPaper.inpector) {
        // Clean up the old inspector if there was one.
        JointPaper.inpector.remove();
        JointPaper.cleanUpInlineEditor();
      }
      JointPaper.inpector = new joint.ui.Inspector({
        cellView: cellView,
        inputs: inputs[type],
        groups: {
          general: {
            label: type,
            index: 1
          },
          appearance: {
            index: 2
          }
        }
      });
      var inpectorElement = JointPaper.inpector.render().el;
      $('#inspector-container').prepend(inpectorElement);
    }

    if (cellView.model instanceof joint.dia.Element) {
      new joint.ui.FreeTransform({
        cellView: cellView,
        // minHeight: 100,
        // minWidth: 100,
        preserveAspectRatio: JointPaper.options.defaultShapeSize[type].preserveAspectRatio
      }).render();
      var halo = new joint.ui.Halo({
        cellView: cellView,
        // type: 'toolbar',
        boxContent: false //Hide box for now
          // boxContent: function(cellView) {
          //   var ret = cellView.model.attributes.roger_federation.type;
          //   if (ret !== undefined) {
          //     return ret;
          //   }
          //   return cellView.model.get('type');
          // }
      });
      halo.removeHandle('unlink');
      halo.removeHandle('resize');
      halo.removeHandle('fork');
      halo.removeHandle('clone');
      halo.removeHandle('rotate');
      halo.render();
    }
  }

  var setupJointJsGraph = function() {
    setContainerSize();
    JointPaper.init($scope.workflow.diagramType, '#joint-diagram', '#paper-container', '#stencil-container', 3000, 3000, 1);

    //Graph Events
    JointPaper.graph.on('add', function(cell, collection, opt) {
      if (!opt.stencil) {
        return;
      }
      var cellView = JointPaper.paper.findViewByModel(cell);
      if (cellView) {
        openTools(cellView); // open inspector after a new element dropped from stencil
        openEditor(cellView);
      }
    });

    JointPaper.paper.on('cell:pointerdblclick', function(cellView, evt, x, y) {
      if (evt.shiftKey) {
        // $state.go('workflows.editor.addOntologyElement');
        $state.go('workflows.editor.rpset', {
          roleProductSetId: $scope.workflow.roleProductSet,
          initialDataSet: cellView.model.attributes.roger_federation.datasetName,
          initialClass: encodeURIComponent(cellView.model.attributes.roger_federation.uri)
        });
      } else {
        openEditor(cellView);
      }
    });

    JointPaper.paper.on('blank:pointerdown', function(evt, x, y) {
      JointPaper.cleanUpInlineEditor();
    });

    function showPopover(targetElement, name, datasetName, uri, expressionText) {
      if (uri === undefined || uri === null || uri === "") {
        if (targetElement.data('bs.popover')) {
          targetElement.data('bs.popover').options.content = "";
        }
        return;
      }
      OntologyService.queryClassComment(datasetName, uri).then(function(comment) {
          OntologyService.queryClassInstances(datasetName, uri).then(function(instances) {
            // var top10Instances = instances.data.results.bindings.map(function (item) {
            //     return item.uri.value.split("#")[1] + ' '
            //   });
            $('.popover').popover('hide'); //Hide any lingering popovers
            var content = name + '<br>Dataset: (' + datasetName + ')' + '<br># of Instance: ' + instances.data.results.bindings.length + (expressionText !== "" ? '<br>Instantiation Expression:' + expressionText : "") + '<br>Comment: ' + comment;
            if (!targetElement.data('bs.popover')) { //Creat new Tooltip
              targetElement.popover({
                placement: 'top',
                trigger: 'hover',
                html: true,
                delay: {
                  "show": 250,
                  "hide": 0
                },
                container: 'body',
                content: content
              }).popover('show');
            } else { //Update Tooltip Contents
              targetElement.data('bs.popover').options.content = content;
              targetElement.data('bs.popover').show();
            }
          }, function() {});
        },
        function() {});
    }

    JointPaper.paper.on('cell:mouseover', function(cellView, evt) {
      // var cellType = cellView.model.attributes.type;
      var roger_federation = cellView.model.attributes.roger_federation;
      var expressionText = (roger_federation.instantiationExpression !== undefined ? roger_federation.instantiationExpression.text : "");
      showPopover(cellView.$el, roger_federation.name, roger_federation.datasetName, roger_federation.uri, expressionText);
    });

    JointPaper.paper.on('cell:pointerup', function(cellView, evt, x, y) {
      openTools(cellView);

      var id = cellView.model.id;
      var cell = JointPaper.graph.getCell(id);

      if (cell !== undefined) {
        var cellType = cell.attributes.type;
        $log.debug('cell is ' + cellType);
        if (cellType === 'bpmn.Flow') { //Invoke Policy editor when creating new links
          if ($scope.workflow.diagramType === "Federation" && cell.attributes.roger_federation.policy === undefined) {
            $state.go('workflows.editor.addBPMNFederatePolicy');
          }
        } else {
          //Determine if shape was dropped on a pool
          var parentShape = cell.collection.get(cell.get('parent')); //Could be a pool or a group
          if (parentShape !== undefined && parentShape.attributes.type === "bpmn.Pool") {
            //Determine which swim lane the shape was dropped on
            var pool = parentShape;
            var poolBBox = pool.getBBox();
            var numOfLanes = pool.attributes.lanes.sublanes.length;
            var laneHeight = poolBBox.height / numOfLanes;
            var shapeBBox = cell.getBBox();
            var droppedSwimLaneIndex = Math.trunc((shapeBBox.y - poolBBox.y) / laneHeight);
            cell.attributes.roger_federation.pool = { //Attach pool Id and swimLane info to shape
              poolId: pool.id,
              swimLane: droppedSwimLaneIndex
            };
          } else {
            delete cell.attributes.roger_federation.pool;
          }

        }
        if (cellType.slice(0, 5) === 'bpmn.') {
          //  $scope.saveGraph();
        }
      }
    });

    JointPaper.graph.on('change', function() {
      $scope.saveGraph();
    });

  };

  var loadJointJsBpmnDiagram = function(workflow) {
    JointPaper.graph.clear();
    if (workflow !== undefined) {
      if (workflow.cells !== null) {
        JointPaper.graph.fromJSON({ //Feed jointjs only the cells or else the commandManager will break
          cells: workflow.cells
        });
        if (workflow.cells.length > 0) {
          $scope.zoomToFit();
        }
      }
      runCritic();
    }
  };

  var loadBpmnModel = function(workflowId) {
    WorkflowService.getBpmnGraph(workflowId).then(function(workflow) {
      $scope.workflow = workflow;
      setupJointJsGraph();
      loadJointJsBpmnDiagram(workflow);

      $scope.$on('QUERY_ADDED', function(event, query) {
        for (var i = 0; i < $scope.workflow.semanticRequests.length; i++) {
          if ($scope.workflow.semanticRequests[i].id === query.id) {
            $scope.workflow.semanticRequests[i] = query;
            return;
          }
        }
        $scope.workflow.semanticRequests.push(query);
      });

    }, function(result) {
      growl.error("Failed to obtain workflow with id: " + workflowId + " Error: " +
        result.data.error);
    });
  };

  $scope.initialize = function() {
    var workflowId = $stateParams.workflowId;
    if (workflowId === undefined || workflowId === -1) {
      growl.error("Need to handle workflow that is: " + workflowId);
    } else {
      loadBpmnModel(workflowId);
    }
  };

  //Closing / Leaving Editor
  window.onbeforeunload = function(event) {
    console.log("leaving editor via Refresh or Hard Close... save graph");
    $scope.saveGraph(); //Save graph upon leaving editor
    event.returnValue = "";
  };
  $scope.$on('$destroy', function() {
    console.log("leaving editor... save graph");
    clearTimeout(criticTimer); //destroy critic timer
    $scope.saveThumbnailAndGraph(); //Save graph upon leaving editor
  });

  //Sub Editors
  $scope.openAttributesEditor = function() {
    $scope.modalInstance = $uibModal.open({
      templateUrl: "views/workflows/edit_attributes.html",
      controller: 'EditAttributesController',
      backdrop: 'static',
      resolve: {
        attributes: function() {
          return $scope.workflow;
        }
      }
    });
  };

  var criticTimer;
  var runCritic = function() {
    $scope.workflow.cells = JointPaper.graph.toJSON().cells;
    $scope.criticResults = workflowCritic($scope.workflow);
    var popoverContent = $scope.criticResults.join("<br>");
    var criticPopover = $("#criticIcon").data('bs.popover');
    if (criticPopover === undefined) { //Init critic popover
      $("#criticIcon").popover({
        placement: 'bottom',
        trigger: 'hover',
        html: true,
        width: 'auto',
        delay: {
          "show": 0,
          "hide": 0
        },
        container: 'body',
        content: popoverContent,
        title: "The following issues need to be corrected in this BPMN diagram.",
        template: '<div class="popover" style="max-width:800px"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'
      });
    } else { //Update Popover Content
      $("#criticIcon").data('bs.popover').options.content = popoverContent;
    }
    criticTimer = setTimeout(runCritic, 1000);
  };
}

$(window).resize(function() {
  try {
    $("#stencil-container").offset({
      top: $("#paper-container").offset().top + 1
    });
  } catch (e) {
    console.log(e);
  } finally {}
});
