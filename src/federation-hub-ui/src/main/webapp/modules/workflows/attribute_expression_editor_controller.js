
"use strict";

angular.module('roger_federation.Workflows')
.controller('AttributeExpressionEditorController',
	[ '$scope', '$rootScope', '$state', '$stateParams', '$http', '$uibModalInstance', '$uibModal', '$log', '$timeout', 'growl', 'ConfigService', 'WorkflowTemplate', 'WorkflowGraphFactory', 'RoleProductSetService', 'RolService', 'WorkflowService', attributeExpressionEditorController]);

function attributeExpressionEditorController($scope, $rootScope, $state, $stateParams, $http, $uibModalInstance, $uibModal, $log, $timeout, growl, ConfigService, WorkflowTemplate, WorkflowGraphFactory, RoleProductSetService, RolService, WorkflowService) {
    $rootScope.$state = $state;
    $rootScope.$stateParams = $stateParams;
    
    $scope.mode = $stateParams.mode;
    $scope.workflowId = $stateParams.workflowId;

    $scope.productData = [];
    $scope.roleData = [];
    
    
    var initOntologyNames = function() {
        return $http.get(ConfigService.getServerBaseUrlStr() + 'ontology').then(
		      function(res) {
		        $scope.ontologies = res.data;
		      },
		      function(reason) {
		        throw reason;
		      }
		);
	};

	// initialize role and ROL state
    var initRolAndRole = function () {
    	var roleSetData = WorkflowTemplate.getRoleSetData();
        if (roleSetData === undefined) {
          var rpSetId = WorkflowTemplate.getRoleProductSet();
          if (rpSetId !== undefined) {
            RoleProductSetService.getRoleProductSet(rpSetId).then(function(result) {
              WorkflowTemplate.setRoleSetData(result.roles);
              $scope.roleData = flattenRoleProductData(result.roles);
            }, function() {
              growl.error("Failed to acquire RoleProduct Set Data.");
            });

          } else {
            growl.error("Role/Product Set is Unknown!");
          }
        } else {
          $scope.roleData = flattenRoleProductData(roleSetData);

          $scope.productData = WorkflowTemplate.getProductSetData();
          
          $scope.graphItemId = $stateParams.graphItemId;
          
          $log.debug('graphItemId: ' + $scope.graphItemId);
  
          WorkflowService.getWorkflowGraphItem($scope.graphItemId).then(function(graphItem) {
        		$log.debug('graph item');
        		$log.debug(JSON.stringify(graphItem));
        		
        		$scope.graphItem = graphItem;
        		
        		$scope.itemId = graphItem.item.id;
        		
        		$scope.roleName = graphItem.item.name;

        		$scope.roleUri = graphItem.item.uri;
        		
        		$scope.ontologyName = graphItem.item.ontology.name;
        		
        		if (graphItem.rolExpressions.length > 0) {
        			$scope.rolStatement = graphItem.rolExpressions[0].statements;
        		} else {
        			$scope.rolStatement = '';
        		}
        		
        		$log.debug('loaded rol statement: ' + $scope.rolStatement);
        		
        		$log.debug('about to parse rol statement: ' + $scope.rolStatement);
            	
            	// get this from the role, later
              	var program = RolService.parse($scope.rolStatement);

              	$log.debug('rol parse complete. program:');
              	$log.debug(program);
              	
              	var treeData = rolTreeToObject(program);
              	
              	$scope.data = treeData;
              	
              	$log.debug('treeData');
              	$log.debug(treeData);
        		
        	});
        }
    };
    
    var flattenRoleProductData = function(rpData) {
    	var flatDataArray = [];
    	for(var i = 0; i < rpData.length; i++) {
    		flatDataArray.push({item: rpData[i].item});
    		flatDataArray = flatDataArray.concat(flattenRoleProductData(rpData[i].children));
    	}
    	
    	return flatDataArray;
    }

    var initProductData = function () {
    	var productSetData = WorkflowTemplate.getProductSetData();
        if (productSetData === undefined) {
          var apSetId = WorkflowTemplate.getRoleProductSet();
          if (apSetId !== undefined) {
            RoleProductSetService.getRoleProductSet(apSetId).then(function(result) {
              WorkflowTemplate.setProductSetData(result.products);
              $scope.productData = flattenRoleProductData(result.products);
            }, function(result) {
              growl.error("Failed to acquire role-product set. Error: " + result.data.error);
            });

          } else {
            growl.error("Role/Product Set is Unknown!");
          }
        } else {
          $scope.productData = flattenRoleProductData(productSetData);
        }
    };

    $scope.initialize = function () {
    	
    	$log.debug('init attribute_expression_editor_controller');
    	
    	$log.debug('scope');
    	$log.debug($scope);
    	
    	$log.debug('state');
    	$log.debug($scope.$state);
    	$log.debug('rootScope');
    	$log.debug($rootScope);

    	$log.debug('stateParams');
    	$log.debug($stateParams);

    	initOntologyNames();
    	initRolAndRole();
    	initProductData();
    };
   
    $scope.submit = function() {
		$uibModalInstance.close('ok');
    };
    
    $scope.cancel = function() {
    	$uibModalInstance.dismiss('cancel');
    };

    // angular-ui-tree related
    
    // remove node
    $scope.remove = function (scope) {
    	scope.remove();
    	updateRol();
    };

    $scope.toggle = function (scope) {
    	scope.toggle();
    };

    $scope.moveLastToTheBeginning = function () {
    	var a = $scope.data.pop();
    	$scope.data.splice(0, 0, a);
    };

    // add node
    $scope.newSubItem = function (scope) {
    	var nodeData = scope.$modelValue;
    	nodeData.nodes.push({
    		type: 'attribute',
    		key: null,
    		value: null,
    		nodes: []
    	});
    	
    	updateRol();
    };

    $scope.collapseAll = function () {
    	$scope.$broadcast('angular-ui-tree:collapse-all');
    };

    $scope.expandAll = function () {
    	$scope.$broadcast('angular-ui-tree:expand-all');
    };

    // using a ROL parse tree visitor implementation, convert a parse tree to a JavaScript object tree for the UI
    function rolTreeToObject(program) {

    	var result = [{
    		type : 'attribute',
    		key : null,
    		value : null,
    		nodes: []
    	}];

    	var rol = RolService.getRol();

    	// invariant: currentNode always points to the node that will be either the logical op node, or the attribute node  
    	var currentNode = result[0];

    	// expression visitor implementation
    	function ExpressionVisitor() {
    		rol.RolVisitor.call(this);
    	}

    	ExpressionVisitor.prototype = Object.create(rol.RolVisitor.prototype);

    	ExpressionVisitor.prototype.visitProgram = function(ctx) {
    		console.log('visiting program');
    		console.log(ctx);

    		// visit each statement in the program
    		ctx.children.forEach(function(child) {
    			// call parent object vist, to generic visit children
    			ExpressionVisitor.prototype.visit(child);
    		});
    	};

    	ExpressionVisitor.prototype.visitAssertion = function(ctx) {
    		console.log('visiting assertion');
    		console.log(ctx);

    		// recursive case (parenthetical expression)

    		// 'match attribute' assertion case (leaf)
    		if (ctx.children !== null && ctx.children.length === 2) {
    			var assertionType = ctx.children[0].symbol.text;
    			$log.debug('assertionType: ' + assertionType);

    			// only process "match attribute" assertions without siblings (no logops, just one attribute assertion)
    			if (assertionType === 'match attribute') {
    				// visit the parameter
    				ExpressionVisitor.prototype.visit(ctx.children[1]);
    			} 
    			
    			// TODO: parentheses logic can go here
//  			else if (assertionType === '(') {
//  			currentNode.type = 'parentheses';
//  			$log.debug('parentheses case');
//  			// save parent ref
//  			var logopNode = currentNode;
//  			// create a node for each node in the parenthetical expression
//  			// create left child
//  			currentNode = makeAttributeNode();
//  			logopNode.nodes.push(currentNode);
//  			// left sibling of logop node
//  			ExpressionVisitor.prototype.visit(ctx.parentCtx.children[0]);
//  			// create right child
//  			currentNode = makeAttributeNode();
//  			logopNode.nodes.push(currentNode);
//  			// right sibling of logop node
//  			ExpressionVisitor.prototype.visit(ctx.parentCtx.children[2]);
//  			$log.debug('processing binary op' + op);
//  			break;
//  			}
    		}

    		// do something with the assertion so that it can be rendered
    	};

    	ExpressionVisitor.prototype.visitLogop = function(ctx) {
    		console.log('visiting logop');
    		console.log(ctx);
    		
    		console.log('tree:');
    		$log.debug(JSON.stringify(result));

    		var op = ctx.children[0].symbol.text;
    		$log.debug('op: ' + op);

    		switch(op) {

    		// binary ops
    		case 'or':
    		case 'and':
    			
    			var left = shallowCopyNode(currentNode);
    			
    			// save parent ref
    			var logopNode = currentNode;
    			
    			logopNode.type = op;
    			logopNode.key = null;
    			logopNode.value = null;
    			logopNode.nodes = [];
    			
    			logopNode.nodes.push(left);

    			// create right child
    			var right = makeAttributeNode();
    			logopNode.nodes.push(right);
    			
    			// TODO: check this - should the current be the sibling of the logop node now instead?
    			currentNode = right;
    			
    			// right sibling of logop node
    			ExpressionVisitor.prototype.visit(ctx.parentCtx.children[2]);

    			$log.debug('processing binary op' + op);
    			break;

    			// unary op
    		case 'not':
    			
    			// TODO: revisit this logic
    			// create child
    			currentNode = makeAttributeNode();
    			logopNode.nodes.push(currentNode);
    			// right sibling of logop node
    			ExpressionVisitor.prototype.visit(ctx.parentCtx.children[1]);

    			$log.debug('processing unary op not');
    			break;
    		default:
    			// unsupported op type
    			$log.debug('operator ' + op + ' not supported in ROL visitor');
    		}
    	};

    	ExpressionVisitor.prototype.visitAssertions = function(ctx) {
    		console.log('visiting assertions');
    		console.log(ctx);
    		
    		// visit all children
    		ctx.children.forEach(function(child) {
    				ExpressionVisitor.prototype.visit(child);
    		});
    	};

    	ExpressionVisitor.prototype.visitStatement = function(ctx) {
    		console.log('visiting statement');

    		// visit all children
    		ctx.children.forEach(function(child) {
    			ExpressionVisitor.prototype.visit(child);
    		});
    	};

    	ExpressionVisitor.prototype.visitParameter = function(ctx) {
    		$log.debug("visit parameter")
    		$log.debug(ctx);

    		if (currentNode.type === 'attribute' && ctx.children.length === 3) {
    			$log.debug('param type');

    			var key = ctx.children[0].symbol.text;

    			// try to parse as JSON, to unescape and remove quotes. This works because the IDENT and STRING productions in ROL are compatible with JSON.
    			try {
    				key = JSON.parse(key);
    			} catch (e) { }

    			$log.debug('key: ' + key);

    			var value = ctx.children[2].children[0].symbol.text;

    			try {
    				value = JSON.parse(value);
    			} catch (e) { }

    			$log.debug('value: ' + value);

    			currentNode.key = key;
    			currentNode.value = value;
    		}
    	};

    	var ev = new ExpressionVisitor();

    	// execute the visitor, updating the UI tree progressively
    	ev.visitProgram(program);

    	console.log('ev in controller');
    	console.log(ev);

    	return result;
    };

    function makeAttributeNode(key, value) {
    	return {
    		type: 'attribute',
    		key: key,
    		value: value,
    		nodes: []
    	}
    };
    
    function shallowCopyNode(node) {
    	return {
    		type: node.type,
    		key: node.key,
    		value: node.value,
    		nodes: []
    	}
    }

    function treeToRol(uiTree) {
    	var rol = 'assign role ' + JSON.stringify($scope.roleUri) + ' ';

    	// iterate and recurse through top-level array
    	uiTree.forEach(function(node) {
    		rol += treeNodeToRol(node, '');
    		rol += ' ';  
    	});

    	return rol + ' {};';
    }

    function treeNodeToRol(node, rol) {
    	switch(node.type) {
    	// base case
    	case 'attribute':
    		rol += 'match attribute ' + node.key + ' : ' + JSON.stringify(node.value);
    		break;
    		// recursive cases
    	case 'and':
    	case 'or':
    		// must have minimum operand count. If invalid, stop.
    		if (node.nodes.length > 1) {

    			// recurse left
    			rol += treeNodeToRol(node.nodes[0], '') + ' ';
    			
    			// operation
    			rol += node.type + ' ';

    			// recurse right
    			rol += treeNodeToRol(node.nodes[1], '') + ' ';
    		}
    		break;
    	case 'not':
    		if (node.nodes.length > 0) {
    			// operation
    			rol += ' node.type ';

    			// recurse right
    			rol += treeNodeToRol(node.nodes[0], '') + ' ';
    			// recurse right
    		}
    		break;
    	default:
    		// unsupported operation
    		$log.debug('unsupported node type ' + node.type);
    	}

    	return rol;
    } 
    
    // regenerate ROL from tree and save to server
    // this could be optimized by only regenerating changed subtrees
    var updateRol = function() {
    	var rol = treeToRol($scope.data)
    	$log.debug('rol after tree change');
    	$log.debug(rol);
    	
    	// Set new ROL on graph item
    	$scope.graphItem.rolExpressions[0] = rol;
    	
    	var gi = $scope.graphItem;
    	
    	// replace item with its id, backend expects this
		gi.item = $scope.itemId;
		var giJson = JSON.stringify(gi);
		$log.debug("graph item json to save: " + giJson);
    	// save to server
    	WorkflowService.updateWorkflowGraphItem($scope.graphItemId, giJson);
    	
    	$log.debug('graph item saved');
    }
    
    $scope.updateRol = updateRol;
};



