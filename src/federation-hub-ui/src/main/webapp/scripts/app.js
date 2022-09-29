
'use strict';

/**
 * @ngdoc overview
 * @name TAK Server Federation Hub UI
 * @description # TAK Server Federation Hub UI
 *
 * Main module of the application.
 */
angular
    .module('fed', [
        'ngAnimate',
        'ngCookies',
        'ngResource',
        'ngMessages',
        'ngFileSaver',
        'ui.router',
        'ui.bootstrap',
        'ui.bootstrap.tpls',
        'ui.layout',
        'ui.select',
        'jsonFormatter',
        "agGrid",
        'ngSanitize',
        'ngTouch',
        'ngStorage',
        'ngFileUpload',
        'smart-table',
        'treeControl',
        'ncy-angular-breadcrumb',
        'roger_federation.Core',
        'roger_federation.Config',
        'roger_federation.Authentication',
        'roger_federation.Manage',
        'roger_federation.Workflows',
        'roger_federation.OntologyService',
        'roger_federation.CommanderDashboard',
        'roger_federation.Logout',
        'angular-growl',
        'uuid4',
        'ui.bootstrap.datetimepicker',
        'angularSpinner',
        'angularMoment',
        'gantt',
        // 'gantt.sortable',
        // 'gantt.movable',
        // 'gantt.drawtask',
        'gantt.tooltips',
        'gantt.bounds', //Display configured bounds when moving mouse over a task.
        // 'gantt.progress', //Display a visual indicator showing configured progress of tasks.
        // 'gantt.table',
        'ui.tree', //gantt.tree dependency
        'gantt.tree', //Display a tree hierarchy of labels on the side.
        'gantt.groups', //Groups tasks into a single row based on defined hierarchy. See Tree Plugin to define this tree hierarchy.
        'gantt.dependencies', //Add support for dependency links between tasks using jsPlumb.
        'gantt.overlap', // Add a border with gantt-task-overlaps CSS class on tasks that overlaps in same rows or through the whole gantt.
        'gantt.resizeSensor', //Use CSS-Element-Queries Polyfill to support dynamic resizing of the component.
        'queryBuilder'
    ])


    .directive('nodeList', function($compile) {
        return {
            restrict: 'E',
            terminal: true,
            scope: {
                nodes: '=ngModel'
            },
            link: function($scope, $element) {
                if (angular.isArray($scope.nodes)) {
                    $element.append('<accordion close-others="true"><node ng-repeat="item in nodes" ng-model="item"></node></accordion>');
                }
                $compile($element.contents())($scope.$new());
            }
        };
    })


    .directive('fileModel', ['$parse', function ($parse) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var model = $parse(attrs.fileModel);
                var modelSetter = model.assign;

                element.bind('change', function(){
                    scope.$apply(function(){
                        modelSetter(scope, element[0].files[0]);
                    });
                });
            }
        };
    }])

    .directive('node', function($compile) {
        return {
            restrict: 'E',
            terminal: true,
            scope: {
                node: '=ngModel'
            },
            link: function($scope, $element) {

                if (angular.isArray($scope.node.children) && $scope.node.children.length > 0) {
                    $element.append('<accordion-group heading={{node.name}}> <node-list ng-model="node.children"></node-list></accordion-group>');
                } else {
                    $element.append("<accordion-group heading={{node.name}}><button type='button'class='btn btn-success' value={{node.name}} ng-click='node.func(node.name)'>Add {{node.name}}</button></accordion-group>");
                }
                $compile($element.contents())($scope.$new());
            }
        };
    })

    .config(["growlProvider", function(growlProvider) {
        growlProvider.globalPosition("top-center"); // default
        growlProvider.globalTimeToLive(5000);
    }])

    .config(["$resourceProvider", function($resourceProvider) {
        $resourceProvider.defaults.stripTrailingSlashes = true;
    }])

    //              .constant("SERVER_BASE_URL", "http://localhost:8080/api/v1/")
    //              .constant("SERVER_BASE_URL", location.origin + "/api/v1/")

    .provider('modalState', function($stateProvider) {
        var provider = this;
        this.$get = function() {
            return provider;
        };

        this.state = function(stateName, options) {
            var modalInstance;
            $stateProvider.state(stateName, {
                url: options.url,
                ncyBreadcrumb: {
                    skip: true
                },
                onEnter: function($uibModal, $state) {
                    modalInstance = $uibModal.open(options);
                    modalInstance.result['finally'](function() {
                        modalInstance = null;
                        if ($state.$current.name === stateName) {
                            $state.go('^');
                        }
                    });
                },
                onExit: function() {
                    if (modalInstance) {
                        modalInstance.close();
                    }
                }
            });
        };
    })

    .config(['$stateProvider', 'modalStateProvider', '$urlRouterProvider', '$breadcrumbProvider',
        function($stateProvider, modalStateProvider, $urlRouterProvider, $breadcrumbProvider) {

            $breadcrumbProvider.setOptions({
                prefixStateName: 'home'
            });

            $stateProvider
            // Application States
                .state('home', {
                    url: "/home",
                    ncyBreadcrumb: {
                        label: 'TAK Server Federation Hub'
                    },
                    templateUrl: "views/dashboard/dashboard.html"
//        data: {
////          requireLogin: true
//        }
                })
                .state('about', {
                    url: "/about",
                    ncyBreadcrumb: {
                        label: 'About'
                    },
                    templateUrl: "views/about.html"
//        data: {
//          requireLogin: true
//        }
                })
                .state('help', {
                    url: "/help",
                    ncyBreadcrumb: {
                        label: 'Help'
                    },
                    templateUrl: "views/help.html"
//        data: {
////          requireLogin: true
//        }
                })
                .state('config', {
                    url: "/config",
                    ncyBreadcrumb: {
                        label: 'Config'
                    },
                    templateUrl: "views/config/config.html",
                    controller: 'ConfigController'
//        data: {
////          requireLogin: true
//        }
                })
                // Template States
                .state('workflows', {
                    abstract: true,
                    url: "/workflows",
                    templateUrl: "views/workflows/workflows.html",
                    controller: 'WorkflowsController'
//        data: {
//          requireLogin: true
//        }
                })
                .state('workflows.editor', {
                    url: "/editor/{workflowId}",
                    ncyBreadcrumb: {
                        label: 'Federation Editor'
                    },
                    templateUrl: "views/workflows/workflow_editor.html",
                    controller: "WorkflowsController"
                })
                .state('workflows.roles', {
                    url: "/roles",
                    ncyBreadcrumb: {
                        label: 'Workflow Roles'
                    },
                    templateUrl: "views/workflows/workflow_roles.html"
                })
                .state('workflows.subscriptions', {
                    url: "/subscriptions",
                    ncyBreadcrumb: {
                        label: 'Workflow Subscriptions'
                    },
                    templateUrl: "views/workflows/workflow_subscriptions.html"
                })

                //    // Instances States
                //    .state('instances', {
                //        abstract: true,
                //        url: "/instances",
                //        templateUrl: "views/instance/instances.html",
                //        controller: 'InstancesController',
                //        data: {
                //          requireLogin: true
                //        }
                //      })
                //      .state('instances.editor', {
                //        url: "/editor/{instanceId}",
                //        ncyBreadcrumb: {
                //          label: 'Instance Editor'
                //        },
                //        templateUrl: "views/instance/instance_editor.html",
                //        controller: 'InstancesController'
                //      })
                //
                //    // Workflow Sets States
                //    .state('workflow-sets', {
                //      abstract: true,
                //      url: "/workflow-sets",
                //      templateUrl: "views/workflow-sets/workflow_sets.html",
                //      controller: 'WorkflowSetsController',
                //      data: {
                //        requireLogin: true
                //      }
                //    })
                //
                //    .state('workflow-sets.editor', {
                //      url: "/editor/{workflowSetId}",
                //      ncyBreadcrumb: {
                //        label: 'Workflow Sets Editor'
                //      },
                //      templateUrl: "views/workflow-sets/workflow_sets_editor.html",
                //      controller: 'WorkflowSetsController'
                //    })
                //
                //    // Role Product Set States
                //    .state('role-product-set', {
                //      abstract: true,
                //      url: "/role-product-set",
                //      templateUrl: "views/role-product-sets/role_product_set.html",
                //      controller: 'RoleProductSetController',
                //      data: {
                //        requireLogin: true
                //      }
                //    })
                //
                //    .state('role-product-set.editor', {
                //      url: "/editor/{roleProductSetId}",
                //      ncyBreadcrumb: {
                //        label: 'Role Product Set Editor'
                //      },
                //      templateUrl: "views/role-product-sets/role_product_set_editor.html",
                //      controller: 'RoleProductSetController'
                //    })

                //Commander Dashboard
                .state('commander-dashboard', {
                    abstract: true,
                    url: "/commander-dashboard",
                    templateUrl: "views/commander-dashboard/commander_dashboard.html",
                    controller: 'CommanderDashboardController'
//      data: {
//        requireLogin: true
//      }
                })

                .state('commander-dashboard.editor', {
                    url: "/editor/{CommanderDashboardId}",
                    ncyBreadcrumb: {
                        label: 'Commander Dashboard'
                    },
                    templateUrl: "views/commander-dashboard/commander_dashboard.html",
                    controller: 'CommanderDashboardController'
                })


                // Manage States
                .state('manage', {
                    abstract: true,
                    url: "/manage",
                    templateUrl: "views/manage/manage.html",
                    controller: 'ManageController'
//        data: {
//          requireLogin: true
//        }
                })
                //      .state('manage.role-sets', {
                //        url: "/role-sets",
                //        ncyBreadcrumb: {
                //          label: 'Manage Role/Product Sets'
                //        },
                //        templateUrl: "views/manage/manage_role_sets.html"
                //      })
                //      .state('manage.diagrams', {
                //        url: "/workflows",
                //        ncyBreadcrumb: {
                //          label: 'Manage Diagrams'
                //        },
                //        templateUrl: "views/manage/manage_workflows.html"
                //      })
                .state('manage.federations', {
                    url: "/federations",
                    ncyBreadcrumb: {
                        label: 'Manage Federations'
                    },
                    templateUrl: "views/manage/manage_federations.html"
                })
                //      .state('manage.workflow-sets', {
                //        url: "/workflow-sets",
                //        ncyBreadcrumb: {
                //          label: 'Manage Workflow Linkage Files'
                //        },
                //        templateUrl: "views/manage/manage_workflow_sets.html"
                //      })
                //      .state('manage.instances', {
                //        url: "/instances",
                //        ncyBreadcrumb: {
                //          label: 'Manage Instances'
                //        },
                //        templateUrl: "views/manage/manage_instances.html"
                //      })
                //      .state('manage.assets', {
                //        url: "/assets",
                //        ncyBreadcrumb: {
                //          label: 'Manage Assets'
                //        },
                //        templateUrl: "views/manage/manage_assets.html"
                //      })
                //      .state('manage.assets.upload', {
                //        url: "/assets/upload",
                //        data: {
                //          requireLogin: true
                //        },
                //        onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
                //          var modalInstance = $uibModal.open({
                //            templateUrl: "views/assets/upload_asset_file.html",
                //            controller: 'ManageController'
                //          });
                //
                //          modalInstance.result.then(function() {
                //            $state.go('manage.assets');
                //          }, function() {
                //            $state.go('manage.assets');
                //          });
                //        }]
                //
                //      })
                //      .state('manage.defaults', {
                //        url: "/defaults",
                //        ncyBreadcrumb: {
                //          label: 'Manage Defaults'
                //        },
                //        templateUrl: "views/manage/manage_defaults.html"
                //      })
                // Monitor States
                .state('monitor', {
                    abstract: true,
                    url: "/monitor",
                    template: '<ui-view>'
//        data: {
//          requireLogin: true
//        }
                })

                // New Workflow
                .state('diagrams-new', {
                    url: '/new_workflow',
                    params: {
                        diagramType: {
                            value: "Workflow"
                        }
                    },
//      data: {
//        requireLogin: true
//      },
                    onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
                        var modalInstance = $uibModal.open({
                            backdrop: 'static',
                            keyboard: false,
                            templateUrl: "views/workflows/new_workflow.html",
                            controller: "NewWorkflowController"
                        });

                        modalInstance.result.then(function() {

                        }, function() {
                            $state.go('home');
                        });
                    }]
                })

                // Load Workflow
                .state('diagrams-load', {
                    url: '/load_workflow',
//      data: {
//        requireLogin: true
//      },
                    onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
                        var modalInstance = $uibModal.open({
                            backdrop: 'static',
                            keyboard: false,
                            templateUrl: "views/workflows/load_workflow.html",
                            controller: 'LoadWorkflowController',
                            size: 'lg',
                            resolve: {
                                configParams: function() {
                                    return {
                                        diagramType: "Workflow",
                                        mode: "load"
                                    };
                                }
                            }
                        });
                        modalInstance.result.then(function() {

                        }, function() {
                            $state.go('home');
                        });
                    }]
                })

                // New Federation
                .state('federations-new', {
                    url: '/new_federation',
                    params: {
                        diagramType: {
                            value: "Federation"
                        }
                    },
//      data: {
//        requireLogin: true
//      },
                    onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
                        var modalInstance = $uibModal.open({
                            backdrop: 'static',
                            keyboard: false,
                            templateUrl: "views/workflows/new_workflow.html",
                            controller: "NewWorkflowController"
                        });

                        modalInstance.result.then(function() {

                        }, function() {
                            $state.go('home');
                        });
                    }]
                })

                // Load Federation
                .state('federations-load', {
                    url: '/load_federation',
//      data: {
//        requireLogin: true
//      },
                    onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
                        var modalInstance = $uibModal.open({
                            backdrop: 'static',
                            keyboard: false,
                            templateUrl: "views/workflows/load_workflow.html",
                            controller: 'LoadWorkflowController',
                            size: 'lg',
                            resolve: {
                                configParams: function() {
                                    return {
                                        diagramType: "Federation",
                                        mode: "load"
                                    };
                                }
                            }
                        });
                        modalInstance.result.then(function() {

                        }, function() {
                            $state.go('home');
                        });
                    }]
                })

                // New Workflow From Inside Editor
                .state('workflows.editor.workflows-new', {
                    url: '/new_workflow',
                    params: {
                        group: {
                            value: false
                        }
                    },
//      data: {
//        requireLogin: true
//      },
                    onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
                        var modalInstance = $uibModal.open({
                            backdrop: 'static',
                            keyboard: false,
                            templateUrl: "views/workflows/new_workflow.html",
                            controller: "NewWorkflowController"
                        });

                        modalInstance.result.then(function() {

                        }, function() {
                            $state.go('^');
                        });
                    }]
                })

                // New Instance
                .state('instances-new', {
                    url: '/new_instance',
//      data: {
//        requireLogin: true
//      },
                    onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
                        var modalInstance = $uibModal.open({
                            backdrop: 'static',
                            keyboard: false,
                            templateUrl: "views/instance/new_instance.html",
                            controller: "NewInstanceController"
                        });

                        modalInstance.result.then(function() {

                        }, function() {
                            $state.go('home');
                        });
                    }]
                })

                // Load Instance
                .state('instances-load', {
                    url: '/load_instance',
//      data: {
//        requireLogin: true
//      },
                    onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
                        var modalInstance = $uibModal.open({
                            backdrop: 'static',
                            keyboard: false,
                            templateUrl: "views/instance/load_instance.html",
                            controller: 'LoadInstanceController',
                            size: 'lg'
                        });
                        modalInstance.result.then(function() {

                        }, function() {
                            $state.go('home');
                        });
                    }]
                })
//
//    // New Instance From Inside Editor
//    .state('instances.editor.instances-new', {
//      url: '/new_instance',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          backdrop: 'static',
//          keyboard: false,
//          templateUrl: "views/instance/new_instance.html",
//          controller: "NewInstanceController"
//        });
//
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('^');
//        });
//      }]
//    })
//
//    // Load Instance From Inside Editor
//    .state('instaces.editor.instances-load', {
//      url: '/load_instance',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          backdrop: 'static',
//          keyboard: false,
//          templateUrl: "views/instance/load_instance.html",
//          controller: 'LoadInstanceController',
//          size: 'lg'
//        });
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('^');
//        });
//      }]
//    })
//
//
//    // New Workflow Sets
//    .state('workflow-sets-new', {
//      url: '/new_workflow_Linkage',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          backdrop: 'static',
//          keyboard: false,
//          templateUrl: "views/workflow-sets/new_workflow_sets.html",
//          controller: "NewWorkflowSetsController"
//        });
//
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('home');
//        });
//      }]
//    })
//
//    // Load Workflow Sets
//    .state('workflow-sets-load', {
//      url: '/load_workflow_sets',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          templateUrl: "views/workflow-sets/load_workflow_sets.html",
//          controller: 'LoadWorkflowSetsController',
//          backdrop: 'static',
//          keyboard: false,
//          size: 'lg'
//        });
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('home');
//        });
//      }]
//    })
//
//    // New Workflow Sets From Inside Editor
//    .state('workflow-sets.editor.workflow-sets-new', {
//      url: '/new_workflow_Linkage',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          backdrop: 'static',
//          keyboard: false,
//          templateUrl: "views/workflow-sets/new_workflow_sets.html",
//          controller: "NewWorkflowSetsController"
//        });
//
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('^');
//        });
//      }]
//    })
//
//    // Load Workflow Sets From Inside Editor
//    .state('workflow-sets.editor.workflow-sets-load', {
//      url: '/load_workflow_sets',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          templateUrl: "views/workflow-sets/load_workflow_sets.html",
//          controller: 'LoadWorkflowSetsController',
//          backdrop: 'static',
//          keyboard: false,
//          size: 'lg'
//        });
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('^');
//        });
//      }]
//    })
//
//
//    // New Role-Product Sets
//    .state('role-product-set-new', {
//      url: '/new_role_product_set',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          backdrop: 'static',
//          keyboard: false,
//          templateUrl: "views/role-product-sets/new_role_product_set.html",
//          controller: "NewRoleProductSetController"
//        });
//
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('home');
//        });
//      }]
//    })
//
//    // Load Role-Product Sets
//    .state('role-product-set-load', {
//      url: '/load_role_product_set',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          templateUrl: "views/role-product-sets/load_role_product_set.html",
//          controller: 'LoadRoleProductSetsController',
//          backdrop: 'static',
//          keyboard: false,
//          size: 'lg'
//        });
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('home');
//        });
//      }]
//    })
//
//
//    // New Role-Product Sets From Inside Editor
//    .state('role-product-set.editor.role-product-set-new', {
//      url: '/new_role_product_set',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          backdrop: 'static',
//          keyboard: false,
//          templateUrl: "views/role-product-sets/new_role_product_set.html",
//          controller: "NewRoleProductSetController"
//        });
//
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('^');
//        });
//      }]
//    })
//
//    // Load Role-Product Sets From Inside Editor
//    .state('role-product-set.editor.role-product-set-load', {
//      url: '/load_role_product_set',
//      data: {
//        requireLogin: true
//      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          templateUrl: "views/role-product-sets/load_role_product_set.html",
//          controller: 'LoadRoleProductSetsController',
//          backdrop: 'static',
//          keyboard: false,
//          size: 'lg'
//        });
//        modalInstance.result.then(function() {
//
//        }, function() {
//          $state.go('^');
//        });
//      }]
//    })


            // Select Asset File
            /*
               .state('workflows.editor.instantiate', {
             url: '/instantiate',

             onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
                 var modalInstance = $uibModal.open({
               templateUrl: "views/select_asset_file.html",
               controller: 'SelectAssetController',
               size : 'lg'});
                 modalInstance.result.then(function() {

                 }, function () {
               $state.go('workflows.editor', { workflowId: $stateParams.workflowId } );
                 });
             }]
               })
             */
//    .state('login', {
//      url: '/login',
////      data: {
////        requireLogin: false
////      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          backdrop: 'static',
//          keyboard: false,
//          templateUrl: 'views/authentication/login.html',
//          controller: 'AuthenticationController',
//          size: 'sm'
//        });
//        modalInstance.result.then(function() {
//          $state.go('home');
//        }, function() {
//          $state.go('login');
//        });
//      }]
//    })
//
//    .state('logout', {
//      url: '/logout',
////      data: {
////        requireLogin: false
////      },
//      onEnter: ['$stateParams', '$state', '$log', '$uibModal', function($stateParams, $state, $log, $uibModal) {
//        var modalInstance = $uibModal.open({
//          backdrop: 'static',
//          keyboard: false,
//          templateUrl: 'views/authentication/logout.html',
//          controller: 'LogoutController',
//          size: 'sm'
//        });
//        modalInstance.result.then(function() {
//          $state.go('login');
//        }, function() {
//          $state.go('home');
//        });
//      }]
//    });


            // Send to login if the URL was not found
            //$urlRouterProvider.otherwise("/login");
            $urlRouterProvider.otherwise("home");

            // Workflow Modal Choosers
            modalStateProvider.state('workflows.editor.selectRole', {
                backdrop: 'static',
                keyboard: false,
                url: '/selectRole',
                templateUrl: "views/workflows/select_role.html",
                controller: 'SelectRoleController'
            });

            modalStateProvider.state('workflows.editor.selectProduct', {
                backdrop: 'static',
                keyboard: false,
                url: '/selectProduct',
                templateUrl: "views/workflows/select_product.html",
                controller: 'SelectProductController'
            });

            modalStateProvider.state('workflows.editor.addRoleProduct', {
                backdrop: 'static',
                keyboard: false,
                size: 'xxl',
                url: '/addRoleProduct/{roleProductSetId}',
                templateUrl: "views/workflows/add_role_product.html",
                controller: 'AddRoleProductController'
            });

            modalStateProvider.state('workflows.editor.addBPMNFederate', {
                backdrop: 'static',
                keyboard: false,
                size: 'md',
                url: '/addBPMNFederate',
                templateUrl: "views/workflows/add_bpmn_federate.html",
                controller: 'AddBpmnFederateController'
            });

            modalStateProvider.state('workflows.editor.addFederateGroup', {
                backdrop: 'static',
                keyboard: false,
                size: 'lg',
                url: '/addFederateGroup',
                templateUrl: "views/workflows/add_federate_group.html",
                controller: 'AddFederateGroupController'
            });

            modalStateProvider.state('workflows.editor.addFederationOutgoing', {
                backdrop: 'static',
                keyboard: false,
                size: 'lg',
                url: '/addFederationOutgoing',
                templateUrl: "views/workflows/add_federation_outgoing.html",
                controller: 'AddFederationOutgoingController'
            });

            modalStateProvider.state('workflows.editor.addBPMNFederatePolicy', {
                backdrop: 'static',
                keyboard: false,
                size: 'md',
                url: '/addBPMNFederatePolicy',
                templateUrl: "views/workflows/add_bpmn_federate_policy.html",
                controller: 'AddBpmnFederatePolicyController'
            });

            modalStateProvider.state('workflows.editor.addBPMNFederatePolicy.addPolicyFilter', {
                backdrop: 'static',
                keyboard: false,
                size: 'md',
                url: '/addPolicyFilter',
                templateUrl: "views/workflows/add_policy_filter.html",
                controller: 'AddPolicyFilterController'
            });

            modalStateProvider.state('workflows.editor.addOntologyElement', {
                backdrop: 'static',
                keyboard: false,
                size: 'xl',
                url: '/addOntologyElement',
                templateUrl: "views/workflows/add_ontology_element.html",
                controller: 'AddOntologyElementController'
            });

            modalStateProvider.state('workflows.editor.rpset', {
                // backdrop : 'static',
                keyboard: false,
                size: 'xxl',
                // url: "/rpset/{roleProductSetId}/dataset/{initialDataSet}",
                url: "/rpset/{roleProductSetId}/dataset/{initialDataSet}/class/{initialClass}",
                //ncyBreadcrumb: {label: 'Role Product Set Editor'},
                templateUrl: "views/role-product-sets/role_product_set_editor.html",
                controller: 'RoleProductSetController'
            });

            modalStateProvider.state('workflows.editor.addBPMNSubProcess', {
                backdrop: 'static',
                keyboard: false,
                url: '/addBPMNSubProcess',
                size: 'lg',
                templateUrl: "views/workflows/add_bpmn_subprocess.html",
                controller: 'AddBPMNSubProcessController'
            });

            modalStateProvider.state('workflows.editor.addBPMNTransition', {
                backdrop: 'static',
                keyboard: false,
                url: '/addTransition',
                templateUrl: "views/workflows/add_bpmn_transition.html",
                controller: 'AddBPMNTransitionController'
            });

            modalStateProvider.state('workflows.editor.addSparqlQuery', {
                backdrop: 'static',
                params: {
                    mode: {
                        value: "new_request",
                        squash: false
                    },
                    roleProductSetId: {
                        value: ""
                    }
                },
                keyboard: false,
                url: '/addSparqlQuery?mode&roleProductSetId',
                templateUrl: "views/workflows/add_sparql_query.html",
                controller: 'AddSparqlQueryController'
            });

            modalStateProvider.state('workflows.editor.lifecycleCommand', {
                backdrop: 'static',
                keyboard: false,
                url: '/lifecycleCommand/{lifecycleEventId:int}',
                templateUrl: "views/workflows/lifecycle_command.html",
                controller: 'LifecycleCommandController'
            });

//    // Workflow Sets Modal Choosers
//    modalStateProvider.state('workflow-sets.editor.addWorkflow', {
//      backdrop: 'static',
//      keyboard: false,
//      url: '/addWorkflow',
//      templateUrl: "views/workflow-sets/add_workflow.html",
//      controller: 'AddWorkflowController'
//    });
//
//    modalStateProvider.state('workflow-sets.editor.addWorkflowSet', {
//      backdrop: 'static',
//      keyboard: false,
//      url: '/addWorkflowSet',
//      templateUrl: "views/workflow-sets/add_workflow_set.html",
//      controller: 'AddWorkflowSetController'
//    });
//
//    modalStateProvider.state('workflow-sets.editor.addTransition', {
//      backdrop: 'static',
//      keyboard: false,
//      url: '/addTransition/{sourceId:int}/{targetId:int}',
//      templateUrl: "views/workflow-sets/add_workflow_transition.html",
//      controller: 'AddWorkflowTransitionController'
//    });
//
//    modalStateProvider.state('workflow-sets.editor.modifyTransition', {
//      backdrop: 'static',
//      keyboard: false,
//      url: '/modifyTransition/{transitionId:int}',
//      templateUrl: "views/workflow-sets/add_workflow_transition.html",
//      controller: 'AddWorkflowTransitionController'
//    });
//
//    modalStateProvider.state('workflow-sets.editor.lifecycleEvents', {
//      backdrop: 'static',
//      keyboard: false,
//      url: '/lifecycleEvents/{workflowSetGraphItemId:int}/{entityType}',
//      templateUrl: "views/workflow-sets/lifecycle_events.html",
//      controller: 'LifecycleEventsController'
//    });
//
//    modalStateProvider.state('workflows.editor.editRoleAttributeExpressions', {
//      backdrop: 'static',
//      params: {
//        mode: {
//          value: "new_request",
//          squash: false
//        }
//      },
//      keyboard: false,
//      url: '/attributeExpressions/:graphItemId',
//      templateUrl: "views/workflows/attribute_expression_editor.html",
//      controller: 'AttributeExpressionEditorController'
//    });
        }
    ])

    .config(function (JSONFormatterConfigProvider) {
        JSONFormatterConfigProvider.hoverPreviewEnabled = true;
    })
//
//.run(function($rootScope, $state, AUTH_EVENTS, ACCESS_EVENTS, growl, AuthenticationService) {
//
//  $rootScope.$on(ACCESS_EVENTS.accessFailed, function(event, result) {
//    var url = "";
//    if (typeof result.config.url !== "undefined") {
//      url = "<br>Url: " + result.config.url;
//    }
//    growl.error("Unable to connect to server." + url);
//    event.preventDefault();
//    $state.go('config');
//  });
//
//
//  $rootScope.$on('$stateChangeStart', function(event, next) {
//    if (next.name === "home") { //Hide scroll bars on all views except the Home page.
//      document.body.style.overflow = "auto";
//    } else {
//      document.body.style.overflow = "hidden";
//    }
//    var requireLogin = next.data.requireLogin;
//    if (requireLogin && !AuthenticationService.isAuthenticated()) {
//      event.preventDefault();
//      // user is not logged in
//      $state.go("login");
//    }
//  });
//
//});

function setContainerSize() {
    try {
        var ctr = $("#container");
        if (ctr.length !== 0) {
            ctr.height(window.innerHeight - ctr.offset().top - 15);
        }
    } catch (e) {
        console.log(e);
    } finally {}
}
$(window).resize(function() {
    setContainerSize();
});
$("body").click(function(event) {
    //In some situations the popovers like to stick around. This global click event listner ensures that doesn't happen.
    $('.popover').popover('hide');
});
