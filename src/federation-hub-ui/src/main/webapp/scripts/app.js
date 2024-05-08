
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
        'ngStorage',
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
        'roger_federation.Config',
        'roger_federation.Workflows',
        'roger_federation.OntologyService',
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
                    templateUrl: "views/dashboard/dashboard.html",
                    controller: "HomeController"
                })
                .state('about', {
                    url: "/about",
                    ncyBreadcrumb: {
                        label: 'About'
                    },
                    templateUrl: "views/about.html"

                })
                .state('settings', {
                    url: "/settings",
                    ncyBreadcrumb: {
                        label: 'Settings'
                    },
                    templateUrl: "views/settings.html",
                    controller: "SettingsController"
                })
                // Template States
                .state('workflows', {
                    abstract: true,
                    url: "/workflows",
                    templateUrl: "views/workflows/workflows.html",
                    controller: 'WorkflowsController'

                })
                .state('workflows.editor', {
                    url: "/editor/{workflowId}",
                    ncyBreadcrumb: {
                        label: 'Federation Editor'
                    },
                    templateUrl: "views/workflows/workflow_editor.html",
                    controller: "WorkflowsController"
                })

                .state('manage.federations', {
                    url: "/federations",
                    ncyBreadcrumb: {
                        label: 'Manage Federations'
                    },
                    templateUrl: "views/manage/manage_federations.html"
                })

                // New Workflow
                .state('diagrams-new', {
                    url: '/new_workflow',
                    params: {
                        diagramType: {
                            value: "Workflow"
                        }
                    },

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

            // Send to login if the URL was not found
            //$urlRouterProvider.otherwise("/login");
            $urlRouterProvider.otherwise("home");

            // Workflow Modal Choosers
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

            modalStateProvider.state('workflows.editor.connections', {
                backdrop: 'static',
                keyboard: false,
                size: 'lg',
                url: '/connections',
                templateUrl: "views/connections/connections.html",
                controller: 'ConnectionsController'
            });

             modalStateProvider.state('workflows.editor.modifyCa', {
                backdrop: 'static',
                keyboard: false,
                size: 'lg',
                url: '/modifyCa',
                templateUrl: "views/ca/ca.html",
                controller: 'CaController'
            });

            modalStateProvider.state('workflows.editor.addBPMNFederatePolicy', {
                backdrop: 'static',
                keyboard: false,
                size: 'md',
                url: '/addBPMNFederatePolicy',
                templateUrl: "views/workflows/add_bpmn_federate_policy.html",
                controller: 'AddBpmnFederatePolicyController'
            });
        }
    ])

    .config(function (JSONFormatterConfigProvider) {
        JSONFormatterConfigProvider.hoverPreviewEnabled = true;
    })


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
