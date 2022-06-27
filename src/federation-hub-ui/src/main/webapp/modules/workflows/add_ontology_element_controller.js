
"use strict";

angular.module('roger_federation.Workflows')
  .controller('AddOntologyElementController', ['$scope', '$rootScope', '$state', '$stateParams', '$interval',
    '$log', '$http', '$modalInstance', 'uuid4', 'growl', 'RoleProductSetService', 'RoleProductSetTemplate', 'OntologyService', 'JointPaper', addOntologyElementController
  ]);

function addOntologyElementController($scope, $rootScope, $state, $stateParams, $interval, $log, $http, $modalInstance, uuid4, growl, RoleProductSetService, RoleProductSetTemplate, OntologyService, JointPaper) {
  $rootScope.$state = $state;
  $rootScope.$stateParams = $stateParams;

  $scope.treeOptions = {
    nodeChildren: "children",
    allowDeselect: false,
    multiSelection: false
  };

  $scope.datasetOptions = [];
  $scope.selectedDataset = "";

  $scope.stateObjects = [];
  $scope.roleProductSet = {};
  $scope.datasetData = [];
  $scope.selectedClass = undefined;
  $scope.selectedRole = undefined;
  $scope.selectedProduct = undefined;
  $scope.showRPSetLoadSpinner = false;

  $scope.cancel = function() {
    // if (!$scope.editExisting && JointPaper.inpector !== undefined) {
    //   JointPaper.inpector.options.cellView.model.remove();
    // }
    $modalInstance.dismiss('cancel');
  };


  $scope.queryClasses = function(searchString) {
    return OntologyService.queryClasses($scope.selectedDataset, searchString, 40).then(function(result) {
      //var x = result.data.results.bindings;
      return result.data.results.bindings.map(function(item) {
        return item.label.value;
      });
    }, function(result) {
      // growl.error("Failed to create Role/Product Set file. Error: " + result.data.error);
    });

    // return $http.get('//maps.googleapis.com/maps/api/geocode/json', {
    //   params: {
    //     address: val,
    //     sensor: false
    //   }
    // }).then(function(response){
    //   return response.data.results.map(function(item){
    //     return item.formatted_address;
    //   });
    // });
  };

  $scope.search = {
    input: {
      text: ""
    },
    options: {
      wholeWordMatch: false,
      includeClassInstances: false
    },
    result: {
      alertNoMatches: false,
      hits: [],
      currentIndex: 0,
      newExpandedNodeList: []

    },
    reset: function() {
      this.input.text = "";
      this.alertNoMatches = false;
      this.result.hits.length = 0;
      this.result.currentIndex = 0;
    },
    doSearch: function() {
      $scope.search.result.hits.length = 0;
      $scope.expandedClassNodes.length = 0;
      $scope.expandedClassNodes = [$scope.datasetData[0]];
      this.result.alertNoMatches = false;
      if ($scope.search.input.text.length === 0) {
        return;
      } else if ($scope.search.input.text.length < 3) {
        growl.error("Please enter at least 3 characters to search");
        return;
      }
      this.doSearchRecursive($scope.datasetData[0].children, $scope.search.input.text.toUpperCase());
      this.result.newExpandedNodeList.push($scope.datasetData[0]);
      $scope.expandedClassNodes = this.result.newExpandedNodeList;
      if ($scope.search.result.hits.length > 0) {
        $scope.search.result.currentIndex = 0;
        $scope.selectedClass = $scope.search.result.hits[0]; //Select first search hit
        $scope.showSelectedClass($scope.selectedClass, false);
        scrollTreeToSelectedClass(1000);
      } else {
        if ($scope.search.input.text !== "") {
          this.result.alertNoMatches = true;
        }
      }
    },
    doSearchRecursive: function(classArray, searchText) {
      var found = false;
      for (var i = 0; i < classArray.length; i++) {
        if (this.hasSearchMatch(classArray[i], searchText, $scope.search.options.wholeWordMatch)) {
          $scope.search.result.hits.push(classArray[i]);
          found = true;
        } else {
          var ret = this.doSearchRecursive(classArray[i].children, searchText);
          if (ret === true) {
            found = true;
            this.result.newExpandedNodeList.push(classArray[i]);
          }
        }
      }
      return found;
    },
    findNext: function() {
      if ($scope.search.result.currentIndex >= $scope.search.result.hits.length - 1) {
        $scope.search.result.currentIndex = 0;
      } else {
        $scope.search.result.currentIndex++;
      }
      $scope.selectedClass = $scope.search.result.hits[$scope.search.result.currentIndex];
      $scope.showSelectedClass($scope.selectedClass, false);
      scrollTreeToSelectedClass(200);
    },
    findPrevious: function() {
      if ($scope.search.result.currentIndex <= 0) {
        $scope.search.result.currentIndex = $scope.search.result.hits.length - 1;
      } else {
        $scope.search.result.currentIndex--;
      }
      $scope.selectedClass = $scope.search.result.hits[$scope.search.result.currentIndex];
      $scope.showSelectedClass($scope.selectedClass, false);
      scrollTreeToSelectedClass(200);
    },
    getNodeBGColor: function(node) {
      if ($scope.search.result.hits.indexOf(node) > -1) {
        return "#aaddff";
      } else {
        return "transparent";
      }
    },
    hasSearchMatch: function(theClass, searchText, wholeWordMatch) {
      if (wholeWordMatch) {
        var re = new RegExp("\\b" + searchText + "\\b", 'ig');
        if (re.test(theClass.name.toUpperCase())) {
          return true;
        }
        if (theClass.label["IRI-based"] !== undefined) {
          if (re.test(theClass.label["IRI-based"].toUpperCase())) {
            return true;
          }
        }
      } else {
        if (theClass.name.toUpperCase().indexOf(searchText) > -1) {
          return true;
        }
        if (theClass.label["IRI-based"] !== undefined) {
          if (theClass.label["IRI-based"].toUpperCase().indexOf(searchText) > -1) {
            return true;
          }
        }
      }
      return false;
    }
  };

  function getIndexByClassId(roleOrProductSet, classId) {
    for (var i = 0; i < roleOrProductSet.length; i++) {
      if (roleOrProductSet[i].id === classId) {
        return i;
      }
    }
    return -1;
  }

  function getIndexByClassURI(roleOrProductSet, classUri) {
    for (var i = 0; i < roleOrProductSet.length; i++) {
      if (roleOrProductSet[i].uri === classUri) {
        return i;
      }
    }
    return -1;
  }

  function copyClassHierarchy(theClass, parentPath) {
    var ret = {
      id: theClass.id,
      // iri: theClass.iri,
      // item: {
      name: theClass.name,
      uri: theClass.iri,
      parentPath: parentPath,
      datasetName: getDatasetDetails($scope.selectedDataset),
      // },
      children: []
    };
    for (var i = 0; i < theClass.children.length; i++) {
      ret.children.push(copyClassHierarchy(theClass.children[i], parentPath === "" ? theClass.label['IRI-based'] : parentPath + "." + theClass.label['IRI-based']));
    }
    return ret;
  }

  function makeNamesUnique(theClass) {
    var uniqueNames = {};
    return makeNamesUniqueRecursive(uniqueNames, theClass);
  }

  function makeNamesUniqueRecursive(uniqueNames, theClass) {
    var uniqueName = theClass.name.trim();
    if (uniqueNames[uniqueName] === undefined) {
      uniqueNames[uniqueName] = 0;
    } else {
      uniqueNames[uniqueName] += 1;
      // uniqueName += " (" +  uniqueNames[uniqueName] + ")";
      uniqueName += repeatStr(" ", uniqueNames[uniqueName]); //Hack: The tree doesn't work properly unless node.name is unique. Keep names unique by padding with trailing spaces.
    }
    theClass.name = uniqueName;
    for (var i = 0; i < theClass.children.length; i++) {
      makeNamesUniqueRecursive(uniqueNames, theClass.children[i]);
    }
    return theClass;
  }

  function repeatStr(str, num) {
    var holder = [];
    for (var i = 0; i < num; i++) {
      holder.push(str);
    }
    return holder.join('');
  }
  $scope.showSelectedClass = function(sel, onlySelectIfDifferent) {
    if ((onlySelectIfDifferent && $scope.selectedClass !== sel) || onlySelectIfDifferent === false) {
      $scope.selectedClass = sel;
      document.getElementById("viz").contentWindow.loadGraphDataByClass(sel.id);
    }
  };

  $scope.checkClassSelection = function() {
    if ($scope.selectedClass === undefined || $scope.selectedClass.name === "Thing") {
      return true;
    }
    return false;
  };

  $scope.saveRoleProductSet = function() {
    RoleProductSetService.uploadRoleProductSet(JSON.stringify($scope.roleProductSet)).then(function() {}, function(result) {
      growl.error("Failed to create Role/Product Set file. Error: " + result.data.error);
    });
  };

  //Roles
  $scope.addClassToRoles = function() {
    if (getIndexByClassURI($scope.roleProductSet.roles, $scope.selectedClass.iri) === -1) {
      // var roleURI = $scope.selectedClass.iri;
      // var rpSetId = RoleProductSetTemplate.getId();
      // var datasetName = $scope.selectedDataset;
      // RoleProductSetService.uploadRole(rpSetId, datasetName, roleURI).then(function() {
      $scope.roleProductSet.roles.push(copyClassHierarchy($scope.selectedClass, ""));
      // }, function(result) {
      //   growl.error("Failed to add role: " + roleURI + ". Error: " + result.data.error);
      // });
      $scope.saveRoleProductSet();
    } else {
      growl.error("The class (" + $scope.selectedClass.name + ") already exists in the role list.");
    }
  };


  $scope.showSelectedRole = function(node, selected) {
    if (selected) {
      $scope.selectedRole = node;
    } else {
      $scope.selectedRole = undefined;
    }
  };
  $scope.allowRoleDeletion = function() {
    if ($scope.selectedRole === undefined) {
      return false;
    } else if (getIndexByClassURI($scope.roleProductSet.roles, $scope.selectedRole.uri) === -1) { //Only top level roles/products can be deleted
      return false;
    }
    return true;
  };
  $scope.removeSelectedRole = function() {
    var index = getIndexByClassURI($scope.roleProductSet.roles, $scope.selectedRole.uri);
    if (index !== -1) {
      var roleURI = $scope.selectedRole.uri;
      // var rpSetId = RoleProductSetTemplate.getId();
      // RoleProductSetService.hasAssociatedWorkflows(rpSetId).then(function(result) {
      //   if (result === true) {
      //     growl.error("This role/product set has an associated workflow and therefore can not be deleted.");
      //   } else {
      // RoleProductSetService.deleteRole(rpSetId, roleURI).then(function() {
      $scope.roleProductSet.roles.splice(index, 1);
      $scope.selectedRole = undefined;
      $scope.saveRoleProductSet();
      growl.success("Successfully removed role: " + roleURI);
      // }, function(result) {
      //   growl.error("Failed to remove role: " + roleURI + ". Error: " + result.data.error);
      // });
      //   }
      // }, function(result) {
      //   growl.error("Failed to remove role: " + roleURI + ". Error: " + result.data.error);
      // });
    }
  };


  //Products
  $scope.addClassToProducts = function() {
    if (getIndexByClassURI($scope.roleProductSet.products, $scope.selectedClass.iri) === -1) {
      // var productURI = $scope.selectedClass.iri;
      // var rpSetId = RoleProductSetTemplate.getId();
      // var datasetName = $scope.selectedDataset;
      // RoleProductSetService.uploadProduct(rpSetId, datasetName, productURI).then(function() {
      $scope.roleProductSet.products.push(copyClassHierarchy($scope.selectedClass, ""));
      // }, function(result) {
      //   growl.error("Failed to add product: " + productURI + ". Error: " + result.data.error);
      // });
      $scope.saveRoleProductSet();
    } else {
      growl.error("The class (" + $scope.selectedClass.name + ") already exists in the product list.");
    }
  };

  $scope.showSelectedProduct = function(node, selected) {
    if (selected) {
      $scope.selectedProduct = node;
    } else {
      $scope.selectedProduct = undefined;
    }
  };
  $scope.allowProductDeletion = function() {
    if ($scope.selectedProduct === undefined) {
      return false;
    } else if (getIndexByClassURI($scope.roleProductSet.products, $scope.selectedProduct.uri) === -1) { //Only top level roles/products can be deleted
      return false;
    }
    return true;
  };
  $scope.removeSelectedProduct = function() {
    var index = getIndexByClassURI($scope.roleProductSet.products, $scope.selectedProduct.uri);
    if (index !== -1) {
      var productURI = $scope.selectedProduct.uri;
      // FIXME BEK
      // var rpSetId = RoleProductSetTemplate.getId();
      // RoleProductSetService.hasAssociatedWorkflows(rpSetId).then(function(result) {
      //   if (result === true) {
      //     growl.error("This role/product set has an associated workflow and therefore can not be deleted.");
      //   } else {
      // RoleProductSetService.deleteProduct(rpSetId, productURI).then(function() {
      $scope.roleProductSet.products.splice(index, 1);
      $scope.selectedProduct = undefined;
      $scope.saveRoleProductSet();
      growl.success("Successfully removed product: " + productURI);
      // }, function(result) {
      //   growl.error("Failed to remove product: " + productURI + ". Error: " + result.data.error);
      // });
      //   }
      // }, function(result) {
      //   growl.error("Failed to remove role: " + productURI + ". Error: " + result.data.error);
      // });
    }
  };

  //FIXME BEK
  $scope.showPopover = function(node, evt) {
    if (!$(evt.target).data('bs.popover')) {
      RoleProductSetService.getClassDetails(node.datasetName, node.uri).then(function(result) {
        $('.popover').popover('hide'); //Hide any lingering popovers
        $(evt.target).popover({
          placement: 'right',
          trigger: 'hover',
          html: true,
          delay: {
            "show": 250,
            "hide": 0
          },
          container: 'body',
          content: 'Dataset: (' + node.datasetName + ')<br>Comment: ' + (result.description === "" ? "none" : result.description)
        }).popover('show');
      }, function() {});
      $('.popover').popover('hide');
    }
  };

  $scope.isDirty = function() {
    return RoleProductSetTemplate.isDirty();
  };

  $scope.$watch('roleProductSet.name', function(newVal, oldVal) {
    if (newVal !== oldVal) {
      RoleProductSetTemplate.setName(newVal);
    }
  }, true);
  $scope.$watch('roleProductSet.creatorName', function(newVal, oldVal) {
    if (newVal !== oldVal) {
      RoleProductSetTemplate.setCreatorName(newVal);
    }
  }, true);
  $scope.$watch('roleProductSet.description', function(newVal, oldVal) {
    if (newVal !== oldVal) {
      RoleProductSetTemplate.setDescription(newVal);
    }
  }, true);

  var loadRoleProductSet = function(RoleProductSetId) {
    return RoleProductSetService.getRoleProductSet(RoleProductSetId).then(function(roleProductSet) {
      RoleProductSetTemplate.clearAll();
      RoleProductSetTemplate.setId(roleProductSet.id);
      RoleProductSetTemplate.setName(roleProductSet.name);
      RoleProductSetTemplate.setCreatorName(roleProductSet.creatorName);
      RoleProductSetTemplate.setDescription(roleProductSet.description);

      if (roleProductSet.roles !== undefined) {
        RoleProductSetTemplate.setRoles(roleProductSet.roles);
      }
      if (roleProductSet.products !== undefined) {
        RoleProductSetTemplate.setProducts(roleProductSet.products);
      }


      $scope.roleProductSet = RoleProductSetTemplate.getAll();

      RoleProductSetTemplate.setIsDirty(false);
    }, function(result) {
      growl.error("Failed to obtain role/product sets file with id: " + $stateParams.roleProductSetId + " Error: " +
        result.data.error);
    });
  };


  $scope.initializeRoleProductSet = function() {
    $scope.selectedDataset = JointPaper.inpector.options.cellView.model.attributes.roger_federation.datasetName;
    RoleProductSetService.getDatasetNames().then(function(datasetList) {
      $scope.datasetOptions = datasetList;
    }, function(result) {
      growl.error("Failed to acquire dataset names. Error: " + result.data.error);
    });
    loadRoleProductSet($stateParams.roleProductSetId).then(function() {
      $scope.getDatasetData($scope.selectedDataset);
    });
  };

  $scope.updateRoleProductSetAttributes = function() {
    RoleProductSetService.updateRoleProductSetAttributes(RoleProductSetTemplate.getId(), RoleProductSetTemplate.getAttributes());
  };

  var updatedRoleProductSet = function() {
    $scope.roleProductSet = RoleProductSetTemplate.getAll();
    RoleProductSetTemplate.setIsDirty(true);
  };

  RoleProductSetTemplate.registerObserver(updatedRoleProductSet);

  function createRoleProductSetPackage() {
    var RoleProductSet = {
      name: RoleProductSetTemplate.getName(),
      version: RoleProductSetTemplate.getVersion(),
      description: RoleProductSetTemplate.getDescription(),
      creatorName: RoleProductSetTemplate.getCreatorName(),
      roles: RoleProductSetTemplate.getRoles(),
      products: RoleProductSetTemplate.getProducts()
    };
    return RoleProductSet;
  }

  function buildClassHierarchy(datasetJson) {
    // console.clear();
    var myClassHierarchy = {
      cache: {
        class: {},
        classAttribute: {},
        datatype: {},
        datatypeAttribute: {},
        property: {},
        propertyAttribute: {}
      },
      topLevel: [],
      things: [],
      thingIds: []
    };
    buildCache(datasetJson.class, myClassHierarchy.cache.class);
    // buildCache(datasetJson.classAttribute, myClassHierarchy.cache.classAttribute);
    // buildCache(datasetJson.datatype, myClassHierarchy.cache.datatype);
    // buildCache(datasetJson.datatypeAttribute, myClassHierarchy.cache.datatypeAttribute);
    buildCache(datasetJson.property, myClassHierarchy.cache.property);
    buildCache(datasetJson.propertyAttribute, myClassHierarchy.cache.propertyAttribute);

    var excludeClassTypes = ["rdfs:Datatype", "rdfs:Literal"];
    //Find top level nodes/classAttributes
    for (var i = 0; i < datasetJson.classAttribute.length; i++) {
      //Add name field. Used for the Tree display
      var classType = myClassHierarchy.cache.class[datasetJson.classAttribute[i].id].type;
      if (excludeClassTypes.indexOf(classType) > -1) {
        datasetJson.classAttribute[i].name = "";
      } else if (datasetJson.classAttribute[i].label === undefined) {
        datasetJson.classAttribute[i].name = "";
      } else {
        if (datasetJson.classAttribute[i].label["undefined"] !== undefined) { //Use label if it exist
          datasetJson.classAttribute[i].name = datasetJson.classAttribute[i].label["undefined"];
        } else if (datasetJson.classAttribute[i].label["IRI-based"] !== undefined) {
          datasetJson.classAttribute[i].name = datasetJson.classAttribute[i].label["IRI-based"];
        } else {
          console.log("Should not happen: Both Label and IRI-based Id are empty");
          datasetJson.classAttribute[i].name = "";
        }
      }

      myClassHierarchy.cache.classAttribute[datasetJson.classAttribute[i].id] = datasetJson.classAttribute[i]; //Add classAttribute to cache
      if (datasetJson.classAttribute[i].superClasses === undefined) { //These are top level classes
        if (datasetJson.classAttribute[i].name === "Thing") {
          myClassHierarchy.things.push(datasetJson.classAttribute[i]);
          myClassHierarchy.thingIds.push(datasetJson.classAttribute[i].id);
        } else if (datasetJson.classAttribute[i].name !== "" && datasetJson.classAttribute[i].name !== "anonymous") {
          myClassHierarchy.topLevel.push(datasetJson.classAttribute[i]);
        }
      }
    }
    //Sort top level nodes
    sortClassArray(myClassHierarchy.topLevel);
    //Recursively add children
    for (var j = 0; j < myClassHierarchy.topLevel.length; j++) {
      buildClassHierarchyChildren(myClassHierarchy.topLevel[j], myClassHierarchy.cache.classAttribute, 0);
    }
    return myClassHierarchy;
  }

  function copyArray(source, dest) {
    for (var i = 0; i < source.length; i++) {
      dest.push(source[i]);
    }
  }

  function buildCache(source, destHashMap) {
    for (var i = 0; i < source.length; i++) {
      destHashMap[source[i].id] = source[i];
    }
  }

  function buildClassHierarchyChildren(theClass, classCacheByClassId, depth) {
    // if (depth === 1) {
    // 	return;
    // }
    theClass.children = []; //Create children array
    //Create a copy of subClasses/superClasses/equivalent as the original arrays will be modified for the graph.
    if (theClass.subClasses !== undefined) {
      if (theClass.subClassesIntact === undefined) {
        theClass.subClassesIntact = [];
        copyArray(theClass.subClasses, theClass.subClassesIntact);
      }
    }
    if (theClass.superClasses !== undefined) {
      if (theClass.superClassesIntact === undefined) {
        theClass.superClassesIntact = [];
        copyArray(theClass.superClasses, theClass.superClassesIntact);
      }
    }
    if (theClass.equivalent !== undefined) {
      if (theClass.equivalentIntact === undefined) {
        theClass.equivalentIntact = [];
        copyArray(theClass.equivalent, theClass.equivalentIntact);
      }
    }
    //Structure class attibutes into a object tree
    if (theClass.subClasses !== undefined) {
      for (var sc = 0; sc < theClass.subClasses.length; sc++) {
        var subClass = classCacheByClassId[theClass.subClasses[sc]];
        theClass.children.push(subClass);
        buildClassHierarchyChildren(subClass, classCacheByClassId, depth += 1);
      }
      sortClassArray(theClass.children);
    }
  }

  function sortClassArray(classArray) {
    classArray.sort(function(a, b) {
      var aKey = a.name.toUpperCase();
      var bKey = b.name.toUpperCase();
      if (aKey > bKey) {
        return 1;
      } else if (aKey < bKey) {
        return -1;
      }
      return 0;
    });
  }

  function getDatasetDetails(datasetName) {
    return $scope.datasetOptions.filter(function(a) {
      return a === datasetName;
    })[0];
  };

  $scope.reloadDataset = function(dataset) {
    $scope.showRPSetLoadSpinner = true;
    RoleProductSetService.reloadDataset(dataset).then(function(result) {
      $scope.getDatasetData(dataset);
    }, function(result) {
      $scope.showRPSetLoadSpinner = false;
      growl.error("Failed to reload dataset.  Error: " + result.data.error);
    });
  };

  $scope.getDatasetData = function(dataset) {
    if (dataset === "") {
      growl.info("Select a dataset to get started.");
      return;
    } else if (dataset === undefined) {
      return;
    }
    $scope.search.reset();

    $scope.showRPSetLoadSpinner = true;
    RoleProductSetService.getDatasetByName(dataset).then(function(result) {
      //  RoleProductSetService.getFOAF().then(function(result) {
      if (result === "") {
        growl.error("Failed to acquire dataset.");
      }
      document.getElementById("viz").contentWindow.loadFullDataSet(JSON.parse(JSON.stringify(result)));

      var classHierarchy = buildClassHierarchy(result);
      var treeViewModel = [];
      var thing = {
        id: "Thing",
        name: "Thing",
        children: []
      };
      for (var i = 0; i < classHierarchy.topLevel.length; i++) {
        if (classHierarchy.topLevel[i].name !== "Thing") { //Hide Thing from Treeview. Why is there more than one Thing?
          thing.children.push(classHierarchy.topLevel[i]);
        }
      }
      // treeViewModel.push(copyClassHierarchy(thing)); //Slim down the class structure to ID, IRI, Name, Children
      treeViewModel.push(makeNamesUnique(JSON.parse(JSON.stringify(thing)))); //Clone classHierarchy structure and make names unique for tree control's sake.  Classes found multiple places in tree will be duplicated. .
      $scope.datasetData = treeViewModel;
      $scope.expandedClassNodes = [$scope.datasetData[0]]; //Expand Thing node
      //$scope.selectedClass = $scope.datasetData[0]; //Set Thing as default node;

      setTimeout(function() { //scroll to first selected item in class hierarchy tree
        //selectNodeRecursive($scope.datasetData[0].children, JointPaper.inpector.options.cellView.model.attributes.roger_federation.uri);
        selectNodeInTree(JointPaper.inpector.options.cellView.model.attributes.roger_federation.uri);
      }, 400);
      resizeClassHierarchy();
    }, function(result) {
      growl.error("Failed to acquire dataset.  Error: " + result.data.error);
    }).finally(function() {
      $scope.showRPSetLoadSpinner = false;
    });
  };

  var selectNodeInTree = function(classIdOrURI) {
    $scope.expandedClassNodes.length = 0;
    $scope.expandedClassNodes = [$scope.datasetData[0]]; //Make sure Thing is always expanded
    $scope.$apply(function() { //force controller to apply changes. http://stackoverflow.com/questions/10490570/call-angular-js-from-legacy-code/10508731#10508731
      selectNodeInTreeRecursive($scope.datasetData[0].children, classIdOrURI);
    });
  };
  window.selectNodeInTree = selectNodeInTree; //Attach to window so visualization can access.
  window.vizLoadComplete = function() {
    $scope.initializeRoleProductSet();
    setContainerSize();
    resizeClassHierarchy();
  };

  function selectNodeInTreeRecursive(classArray, classIdOrURI) { //Walk tree to find node to select. Expand nesting.
    for (var i = 0; i < classArray.length; i++) {
      if (classArray[i].id === classIdOrURI || classArray[i].iri === classIdOrURI) {
        $scope.selectedClass = classArray[i];
        return classArray[i];
      } else {
        var ret = selectNodeInTreeRecursive(classArray[i].children, classIdOrURI);
        if (ret !== undefined) {
          $scope.expandedClassNodes.push(classArray[i]);
          return ret;
        }
      }
    }
  }

  function scrollTreeToSelectedClass(delay) {
    try {
      setTimeout(function() { //scroll to first selected item in class hierarchy tree
        $("#pnlClassHierarchy").get(0).scrollTop = $("#treeClassHierarchy .tree-selected").offset().top - $("#treeClassHierarchy").position().top - 300;
      }, delay);
    } catch (e) {
      //an expection is possible if the tree control takes to long to update.  typically when there are a large number of elements in expandedClassNodes
    }
  }

  $scope.$on('ui.layout.resize', function(e, beforeContainer, afterContainer) {
    resizeClassHierarchy();
  });
  $(window).resize(function() {
    clearTimeout(window.resizedFinished);
    window.resizedFinished = setTimeout(function() {
      resizeClassHierarchy();
    }, 100);

  });

  function resizeClassHierarchy() {
    $("#pnlClassHierarchy").height($("#pnlPrimary").innerHeight() - 110);
    $("#viz").height($("#vizContainer").innerHeight() - 55);
  }

  function selectNodeRecursive(classArray, uri) {
    var found = false;
    for (var i = 0; i < classArray.length; i++) {
      if (classArray[i].iri === uri) {
        $scope.selectedNode = classArray[i];
        return true;
      } else {
        var ret = selectNodeRecursive(classArray[i].children, uri);
        if (ret === true) {
          $scope.expandedClassNodes.push(classArray[i]);
          found = true;
        }
      }
    }
    return found;
  }
}
