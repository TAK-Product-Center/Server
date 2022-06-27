
'use strict';

angular.module('roger_federation.Workflows')
.factory("WorkflowTemplate", ["$localStorage",
                              function ($localStorage) {

    var service = {};

    var observers = [];
    var tupleDataDefaults = {subjects : [{group : "Variable", name : "New Variable", id: 0,  validRelations : 'all', validObjects : 'all' }],
	    		     relations : [],
	                     objects: [{group : "Variable", name : "New Variable", id : 0,  validRelations : 'all', validSubjects : 'all'}] };


    var _isDirty = false;

    service.clearAll = function () {
	$localStorage.workflow = {};
	$localStorage.workflow.id = -1;
	$localStorage.workflow.name = "No Name";
	$localStorage.workflow.creatorName = "No Creator";
	$localStorage.workflow.creationDate = "No Creation Date";
	$localStorage.workflow.modifiedDate = "No Modified Date";
	$localStorage.workflow.description = "No Description";
	$localStorage.workflow.roleProductSet = -1;
	$localStorage.workflow.graphItems = [];
	$localStorage.workflow.graphLinks = [];
	$localStorage.workflow.roleSetData = [];
	$localStorage.workflow.productSetData = [];
	$localStorage.workflow.tupleDataChoices = {};
	$localStorage.workflow.isGroup = false;
	$localStorage.workflow.lifecycleEvents = [];

//	$localStorage.workflow.roleProductSetData = { products: [], roles: [] };
//	$localStorage.workflow.network = { cells : [] };
	_isDirty = false;
    };

    service.registerObserver = function(callback) {
	observers.push(callback);
    };

    var notifyObservers = function() {
	angular.forEach(observers, function(callback) {
	    callback();
	});
    };

    service.setName = function(name) {
	var diff = false;
	if ($localStorage.workflow.name !== name) {
	    diff = true;
	}
	$localStorage.workflow.name = name;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getName = function() {
	return $localStorage.workflow.name;
    };

    service.setDescription = function(description) {
	var diff = false;
	if ($localStorage.workflow.description !== description) {
	    diff = true;
	}
	$localStorage.workflow.description = description;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getDescription = function() {
	return $localStorage.workflow.description;
    };

    service.setCreatorName = function(creatorName) {
	var diff = false;
	if ($localStorage.workflow.creatorName !== creatorName) {
	    diff = true;
	}
	$localStorage.workflow.creatorName = creatorName;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getCreatorName = function() {
	return $localStorage.workflow.creatorName;
    };


    service.setCreationDate = function(creationDate) {
	var diff = false;
	if ($localStorage.workflow.creationDate !== creationDate) {
	    diff = true;
	}
	$localStorage.workflow.creationDate = creationDate;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getCreationDate = function() {
	return $localStorage.workflow.creationDate;
    };


    service.setModifiedDate = function(modifiedDate) {
	var diff = false;
	if ($localStorage.workflow.modifiedDate !== modifiedDate) {
	    diff = true;
	}
	$localStorage.workflow.modifiedDate = modifiedDate;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getModifiedDate = function() {
	return $localStorage.workflow.modifiedDate;
    };


    service.setId = function(id) {
	var diff = false;
	if ($localStorage.workflow.id !== id) {
	    diff = true;
	}
	$localStorage.workflow.id = id;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getId = function() {
	return $localStorage.workflow.id;
    };

    service.setRoleProductSet = function(roleProductSet) {
	var diff = false;
	if ($localStorage.workflow.roleProductSet !== roleProductSet) {
	    diff = true;
	}
	$localStorage.workflow.roleProductSet = roleProductSet;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getRoleProductSet = function() {
	return $localStorage.workflow.roleProductSet;
    };

    service.setProductSetData = function(productSetData) {
	var diff = false;
	if ($localStorage.workflow.productSetData !== productSetData) {
	    diff = true;
	}
	$localStorage.workflow.productSetData = productSetData;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getProductSetData = function() {
	return $localStorage.workflow.productSetData;
    };

    service.setRoleSetData = function(roleSetData) {
	var diff = false;
	if ($localStorage.workflow.roleSetData !== roleSetData) {
	    diff = true;
	}
	$localStorage.workflow.roleSetData = roleSetData;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getRoleSetData = function() {
	return $localStorage.workflow.roleSetData;
    };

    service.setGraphItems = function(graphItems) {
	var diff = false;
	if ($localStorage.workflow.graphItems !== graphItems) {
	    diff = true;
	}
	$localStorage.workflow.graphItems = graphItems;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getGraphItems = function() {
	return $localStorage.workflow.graphItems;
    };

    service.setLifecycleEvents = function(lifecycleEvents) {
  var diff = false;
  if ($localStorage.workflow.lifecycleEvents !== lifecycleEvents) {
      diff = true;
  }
  $localStorage.workflow.lifecycleEvents = lifecycleEvents;
  if (diff) {
      notifyObservers();
  }
    };

    service.getLifecycleEvents = function() {
  return $localStorage.workflow.lifecycleEvents;
    };

    service.setGraphLinks = function(graphLinks) {
	var diff = false;
	if ($localStorage.workflow.graphLinks !== graphLinks) {
	    diff = true;
	}
	$localStorage.workflow.graphLinks = graphLinks;
	if (diff) {
	    notifyObservers();
	}
    };

    service.getGraphLinks = function() {
	return $localStorage.workflow.graphLinks;
    };

    service.setTupleDataChoices = function(data) {
	var diff = false;
	if ($localStorage.workflow.tupleDataChoices === undefined) {
	    $localStorage.workflow.tupleDataChoices = {};
	}
	if ($localStorage.workflow.tupleDataChoices !== data) {
	    diff = true;
	}

	$localStorage.workflow.tupleDataChoices.subjects = tupleDataDefaults.subjects.concat(data.subjects);
	$localStorage.workflow.tupleDataChoices.relations = tupleDataDefaults.relations.concat(data.relations);
	$localStorage.workflow.tupleDataChoices.objects = tupleDataDefaults.objects.concat(data.objects);

	if (diff) {
	    notifyObservers();
	}
    };

    service.getTupleDataChoices = function() {
	return $localStorage.workflow.tupleDataChoices;
    };

    service.isGroup = function() {
    	return $localStorage.workflow.isGroup;
    };

    service.setIsGroup = function(isGroup) {
    	$localStorage.workflow.isGroup = isGroup;
    };

    service.setIsDirty = function (isDirty) {
	_isDirty = isDirty;
    };

    service.isDirty = function () {
	return _isDirty;
    };


    service.getAll = function() {
	var result = {
		id : $localStorage.workflow.id,
		name : $localStorage.workflow.name,
		creatorName : $localStorage.workflow.creatorName,
		creationDate : $localStorage.workflow.creationDate,
		modifiedDate : $localStorage.workflow.modifiedDate,
		description : $localStorage.workflow.description,
		roleProductSet : $localStorage.workflow.roleProductSet,
		graphItems : $localStorage.workflow.graphItems,
		graphLinks : $localStorage.workflow.graphLinks,
		isGroup : $localStorage.workflow.isGroup,
		lifecycleEvents : $localStorage.workflow.lifecycleEvents,
	};
	return result;
    };

    service.getAttributes = function() {
      var result = {
        id: $localStorage.workflow.id,
        name: $localStorage.workflow.name,
        creatorName: $localStorage.workflow.creatorName,
        description: $localStorage.workflow.description
      };
      return result;
    };

    if (typeof $localStorage.workflow === "undefined") {
	service.clearAll();
    }

    return service;
}]);
