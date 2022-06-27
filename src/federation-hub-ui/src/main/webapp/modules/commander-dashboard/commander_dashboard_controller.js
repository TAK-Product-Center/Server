
'use strict';

angular.module('roger_federation.CommanderDashboard')
  .controller('CommanderDashboardController', ['$scope', '$rootScope', '$state', '$stateParams', '$interval', '$log', '$http', 'uuid4', 'growl',
    'WorkflowService', 'WorkflowTemplate', 'InstanceService', 'RoleProductSetService', 'AssetMatchingService', commanderDashboardController
  ]);

function commanderDashboardController($scope, $rootScope, $state, $stateParams, $interval, $log, $http, uuid4, growl, WorkflowService,
   WorkflowTemplate, InstanceService, RoleProductSetService, AssetMatchingService) {
  $rootScope.$state = $state;
  $rootScope.$stateParams = $stateParams;

  $scope.workflowList = {};
  $scope.instanceList = {};
  $scope.displayedInstanceCollection = [];

  $scope.activeInstance = undefined;
  $scope.selectedInstanceTag = undefined;

  $scope.graphLayout = {
    zoomlevel: 1.0
  };

  $scope.canAutoWidth = function(scale) {
      if (scale.match(/.*?hour.*?/) || scale.match(/.*?minute.*?/)) {
          return false;
      }
      return true;
  };
  $scope.getColumnWidth = function(widthEnabled, scale, zoom) {
      //  if (!widthEnabled && $scope.canAutoWidth(scale)) {
      //      return undefined;
      //  }
       //
      //  if (scale.match(/.*?week.*?/)) {
      //      return 150 * zoom;
      //  }
       //
      //  if (scale.match(/.*?month.*?/)) {
      //      return 300 * zoom;
      //  }
       //
      //  if (scale.match(/.*?quarter.*?/)) {
      //      return 500 * zoom;
      //  }
       //
      //  if (scale.match(/.*?year.*?/)) {
      //      return 800 * zoom;
      //  }

       return 40 * zoom;
   };

  $scope.gantt = {
    api: {},
    options: {
      mode: 'custom',
      scale: 'day',
      sortMode: undefined,
      sideMode: 'TreeTable',
      daily: false,
      maxHeight: false,
      width: false,
      zoom: 1,
      columns: ['model.name', 'from', 'to'],
      treeTableColumns: ['from', 'to'],
      columnsHeaders: {
        'model.name': 'Name',
        'from': 'From',
        'to': 'To'
      },
      columnsClasses: {
        'model.name': 'gantt-column-name',
        'from': 'gantt-column-from',
        'to': 'gantt-column-to'
      },
      columnsFormatters: {
        'from': function(from) {
          return from !== undefined ? from.format('lll') : undefined;
        },
        'to': function(to) {
          return to !== undefined ? to.format('lll') : undefined;
        }
      },
      treeHeaderContent: '<i class="fa fa-align-justify"></i> {{getHeader()}}',
      columnsHeaderContents: {
        'model.name': '<i class="fa fa-align-justify"></i> {{getHeader()}}',
        'from': '<i class="fa fa-calendar"></i> {{getHeader()}}',
        'to': '<i class="fa fa-calendar"></i> {{getHeader()}}'
      },
      autoExpand: 'none',
      taskOutOfRange: 'truncate',
      fromDate: moment(null),
      toDate: undefined,
      // rowContent: '<i class="fa fa-align-justify"></i> {{row.model.name}}',
       taskContent: '<i class="fa fa-tasks"></i> {{task.model.name}}',
      allowSideResizing: true,
      labelsEnabled: true,
      currentDate: 'line',
      // currentDateValue: new Date(2013, 09, 26, 14, 24, 10),
      draw: false,
      readOnly: false,
      groupDisplayMode: 'group',
      filterTask: '',
      filterRow: '',
      timeFrames: {
        // 'day': {
        //   start: moment('8:00', 'HH:mm'),
        //   end: moment('20:00', 'HH:mm'),
        //   color: '#ACFFA3',
        //   working: true,
        //   default: true
        // },
        // 'noon': {
        //   start: moment('12:00', 'HH:mm'),
        //   end: moment('13:30', 'HH:mm'),
        //   working: false,
        //   default: true
        // },
        // 'closed': {
        //   working: false,
        //   default: true
        // },
        // 'weekend': {
        //   working: false
        // },
        'holiday': {
          working: false,
          color: 'red',
          classes: ['gantt-timeframe-holiday']
        }
      },
      dateFrames: {
        'weekend': {
          evaluator: function(date) {
            return date.isoWeekday() === 6 || date.isoWeekday() === 7;
          },
          targets: ['weekend']
        },
        '11-november': {
          evaluator: function(date) {
            return date.month() === 9 && date.date() === 30;
          },
          targets: ['holiday']
        }
      },
      timeFramesWorkingMode: 'hidden',
      timeFramesNonWorkingMode: 'visible',
      columnMagnet: '15 minutes',
      timeFramesMagnet: true,
      dependencies: true,
      canDraw: function(event) {
        var isLeftMouseButton = event.button === 0 || event.button === 1;
        return $scope.options.draw && !$scope.options.readOnly && isLeftMouseButton;
      },
      drawTaskFactory: function() {
        return {
          id: utils.randomUuid(), // Unique id of the task.
          name: 'Drawn task', // Name shown on top of each task.
          color: '#AA8833' // Color of the task in HEX format (Optional).
        };
      }
    },
    zoomIn: function() {
      this.options.zoom =  this.options.zoom + 1;
    },
    zoomOut: function() {
      if (this.options.zoom > 1 ) {
        this.options.zoom = this.options.zoom - 1;
      }
    },
    data: [],
    mockupdata: [{
        name: 'Roles',
        children: ['TCJ3-FC - SpecialAssignmentAirliftMissionPlanning', 'TCJ3-FC - SpecialAssignmentAirliftMissionMonitoring', 'SpecialAssignmentAirliftMissionExecution'],
        content: '<i class="fa fa-file-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'
      }, {
        name: 'TCJ3-FC - SpecialAssignmentAirliftMissionPlanning',
        tooltips: true,
        tasks: [{
            id: '3333',
            name: 'TCJ3-FC',
            color: 'salmon',
            from: new Date(2013, 9, 28, 8, 0, 0),
            to: new Date(2013, 10, 1, 15, 0, 0)
          }
          // {id: '1111', name: 'TCJ3-FC', color: '#F1C232', from: new Date(2013, 9, 21, 8, 0, 0), to: new Date(2013, 9, 25, 15, 0, 0), progress: 25 }
        ]
      }, {
        name: 'TCJ3-FC - SpecialAssignmentAirliftMissionMonitoring',
        tasks: [{
          id: '2222',
          name: 'TCJ3-FC',
          color: 'salmon',
          from: new Date(2013, 9, 28, 8, 0, 0),
          to: new Date(2013, 10, 1, 15, 0, 0)
        }]
      }, {
        name: 'SpecialAssignmentAirliftMissionExecution',
        tasks: [
          // {id: '3333', name: 'TCJ3-FC', color: '#F1C232', from: new Date(2013, 9, 28, 8, 0, 0), to: new Date(2013, 10, 1, 15, 0, 0)}
        ]
      },

      {
        name: 'Products',
        children: ['RadiologicalIncidentReport', 'SAAMRequest'],
        content: '<i class="fa fa-file-powerpoint-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'
      }, {
        name: 'RadiologicalIncidentReport',
        tooltips: true,
        tasks: [{
          id: '4444',
          name: 'RadiologicalIncidentReport',
          color: 'blue',
          from: new Date(2013, 9, 21, 8, 0, 0),
          to: new Date(2013, 9, 25, 15, 0, 0),
          progress: 25
        }]
      }, {
        name: 'SAAMRequest',
        tasks: [
          // {id: 'Order basket', name: 'Order basket', color: '#F1C232', from: new Date(2013, 9, 28, 8, 0, 0), to: new Date(2013, 10, 1, 15, 0, 0)}
        ]
      }
    ],
    mockupdata3: [
{name: 'Scenario', children: ['ISR Mission', 'Worflow 2', 'Worflow 3', 'Worflow 4', 'Worflow 5'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
// {name: 'ISR Mission', children: ['Mission Tasking', 'Mission Execution'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'ISR Mission', children: ['TF 3-10', 'ISARC', 'DY 14', 'Airspace Control', 'TF South', 'DCGS GA', 'SW 16'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'TF 3-10', tooltips: true, tasks: [
    {id: 'Role Z1', name: 'Submit ISR request', color: 'blue', from: new Date(2013, 9, 21, 8, 0, 0), to: new Date(2013, 9, 21, 15, 0, 0),
        progress: 25, dependencies: [{to: 'Process Request'}]}

]},
{name: 'ISARC', tasks: [
    // {id: '2133211', name: '', color: 'rgba(0, 0, 0, 0.35)', from: new Date(2013, 9, 20, 15, 0, 0), to: new Date(2013, 10, 5, 15, 0, 0) },
    {id: 'Process Request', name: 'Process Request', color: 'blue', from: new Date(2013, 9, 21, 15, 0, 0), to: new Date(2013, 9, 21, 16, 0, 0)}
]},
{name: 'DY 14', tasks: [
    {id: 'Role X1', name: 'Role X1', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 27, 15, 0, 0),
        dependencies: {to: 'Product B1'}}
]},
{name: 'Airspace Control', tasks: [
    {id: 'Role W1', name: 'Role W1', color: 'blue', from: new Date(2013, 9, 30, 15, 0, 0), to: new Date(2013, 9, 30, 15, 0, 0)}
]},
{name: 'TF South', tasks: [
    {id: 'Role W13', name: 'Role W12', color: 'blue', from: new Date(2013, 9, 30, 15, 0, 0), to: new Date(2013, 9, 30, 15, 0, 0)}
]},
{name: 'DCGS GA', tasks: [
    {id: 'Role W133', name: 'Role W122', color: 'blue', from: new Date(2013, 9, 30, 15, 0, 0), to: new Date(2013, 9, 30, 15, 0, 0)}
]},
{name: 'SW 16', tasks: [
    {id: 'Role W134', name: 'Role W134', color: 'blue', from: new Date(2013, 9, 30, 15, 0, 0), to: new Date(2013, 9, 30, 15, 0, 0)}
]},


],
    mockupdata2: [
{name: 'Worflow Set A', children: ['Worflow 1', 'Worflow 2', 'Worflow 3', 'Worflow 4', 'Worflow 5'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Worflow 1', children: ['Roles1', 'Products1'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Roles1', children: ['Role Z1', 'Role Y1', 'Role X1', 'Role W1'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{id: 'Role Z1', name: 'Role Z1', tooltips: false, tasks: [
    {id: 'Role Z1', name: 'Role Z1', color: 'blue', from: new Date(2013, 9, 21, 8, 0, 0), to: new Date(2013, 9, 25, 15, 0, 0),
        progress: 25, dependencies: [{to: 'Product A1'}]}

]},
{name: 'Role Y1', tasks: [
    {id: '2133211', name: '', color: 'rgba(0, 0, 0, 0.35)', from: new Date(2013, 9, 20, 15, 0, 0), to: new Date(2013, 10, 5, 15, 0, 0) },
    {id: 'Role Y1', name: 'Role Y1', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 28, 15, 0, 0),
        dependencies: [{to: 'Product C1'}, {to: 'Role W1'}]}
]},
{name: 'Role X1', tasks: [
    {id: 'Role X1', name: 'Role X1', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 27, 15, 0, 0),
        dependencies: {to: 'Product B1'}}
]},
{name: 'Role W1', tasks: [
    {id: 'Role W1', name: 'Role W1', color: 'blue', from: new Date(2013, 9, 30, 15, 0, 0), to: new Date(2013, 9, 30, 15, 0, 0),
        dependencies: [{to: 'Product D1'}]}
]},
{name: 'Products1', children: ['Product A1', 'Product B1', 'Product C1', 'Product D1'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Product A1', tooltips: false, tasks: [
    {id: 'Product A1', name: 'Product A1', color: '#F1C232', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 10, 25, 15, 0, 0),
        progress: 25}
]},
{name: 'Product B1', tasks: [
    {id: 'Product B1', name: 'Product B1', color: '#F1C232', from: new Date(2013, 9, 28, 15, 0, 0), to: new Date(2013, 10, 15, 15, 0, 0)}
]},
{name: 'Product C1', tasks: [
    {id: 'Product C1', name: 'Product C1', color: '#F1C232', from: new Date(2013, 9, 29, 15, 0, 0), to: new Date(2013, 9, 29, 15, 0, 0)}
]},
{name: 'Product D1', tasks: [
    {id: 'Product D1', name: 'Product D1', color: '#F1C232', from: new Date(2013, 10, 1, 15, 0, 0), to: new Date(2013, 10, 1, 15, 0, 0)}
]},


{name: 'Worflow 2', children: ['Roles2', 'Products2'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Roles2', children: ['Role Z2', 'Role Y2', 'Role X2', 'Role W2'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{id: 'Role Z2', name: 'Role Z2', tooltips: false, tasks: [
    {id: 'Role Z2', name: 'Role Z2', color: 'blue', from: new Date(2013, 9, 21, 8, 0, 0), to: new Date(2013, 9, 25, 15, 0, 0),
        progress: 25, dependencies: [{to: 'Product A2'}]}
]},
{name: 'Role Y2', tasks: [
    {id: 'Role Y2', name: 'Role Y2', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 28, 15, 0, 0),
        dependencies: [{to: 'Product C2'}, {to: 'Role W2'}]}
]},
{name: 'Role X2', tasks: [
    {id: 'Role X2', name: 'Role X2', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 27, 15, 0, 0),
        dependencies: {to: 'Product B2'}}
]},
{name: 'Role W2', tasks: [
    {id: 'Role W2', name: 'Role W2', color: 'blue', from: new Date(2013, 9, 30, 15, 0, 0), to: new Date(2013, 9, 30, 15, 0, 0),
        dependencies: [{to: 'Product D2'}]}
]},
{name: 'Products2', children: ['Product A2', 'Product B2', 'Product C2', 'Product D2'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Product A2', tooltips: false, tasks: [
    {id: 'Product A2', name: 'Product A2', color: '#F1C232', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 10, 25, 15, 0, 0),
        progress: 25}
]},
{name: 'Product B2', tasks: [
    {id: 'Product B2', name: 'Product B2', color: '#F1C232', from: new Date(2013, 9, 28, 15, 0, 0), to: new Date(2013, 10, 15, 15, 0, 0)}
]},
{name: 'Product C2', tasks: [
    {id: 'Product C2', name: 'Product C2', color: '#F1C232', from: new Date(2013, 9, 29, 15, 0, 0), to: new Date(2013, 9, 29, 15, 0, 0)}
]},
{name: 'Product D2', tasks: [
    {id: 'Product D2', name: 'Product D2', color: '#F1C232', from: new Date(2013, 10, 1, 15, 0, 0), to: new Date(2013, 10, 1, 15, 0, 0)}
]},
{name: 'Worflow 3', children: ['Roles3', 'Products3'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Roles3', children: ['Role Z3', 'Role Y3', 'Role X3', 'Role W3'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{id: 'Role Z3', name: 'Role Z3', tooltips: false, tasks: [
    {id: 'Role Z3', name: 'Role Z3', color: 'blue', from: new Date(2013, 9, 21, 8, 0, 0), to: new Date(2013, 9, 25, 15, 0, 0),
        progress: 25, dependencies: [{to: 'Product A3'}]}
]},
{name: 'Role Y3', tasks: [
    {id: 'Role Y3', name: 'Role Y3', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 28, 15, 0, 0),
        dependencies: [{to: 'Product C3'}, {to: 'Role W3'}]}
]},
{name: 'Role X3', tasks: [
    {id: 'Role X3', name: 'Role X3', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 27, 15, 0, 0),
        dependencies: {to: 'Product B3'}}
]},
{name: 'Role W3', tasks: [
    {id: 'Role W3', name: 'Role W3', color: 'blue', from: new Date(2013, 9, 30, 15, 0, 0), to: new Date(2013, 9, 30, 15, 0, 0),
        dependencies: [{to: 'Product D3'}]}
]},
{name: 'Products3', children: ['Product A3', 'Product B3', 'Product C3', 'Product D3'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Product A3', tooltips: false, tasks: [
    {id: 'Product A3', name: 'Product A3', color: '#F1C233', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 10, 25, 15, 0, 0),
        progress: 25}
]},
{name: 'Product B3', tasks: [
    {id: 'Product B3', name: 'Product B3', color: '#F1C233', from: new Date(2013, 9, 28, 15, 0, 0), to: new Date(2013, 10, 15, 15, 0, 0)}
]},
{name: 'Product C3', tasks: [
    {id: 'Product C3', name: 'Product C3', color: '#F1C233', from: new Date(2013, 9, 29, 15, 0, 0), to: new Date(2013, 9, 29, 15, 0, 0)}
]},
{name: 'Product D3', tasks: [
    {id: 'Product D3', name: 'Product D3', color: '#F1C233', from: new Date(2013, 10, 1, 15, 0, 0), to: new Date(2013, 10, 1, 15, 0, 0)}
]},
{name: 'Worflow 4', children: ['Roles4', 'Products4'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Roles4', children: ['Role Z4', 'Role Y4', 'Role X4', 'Role W4'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{id: 'Role Z4', name: 'Role Z4', tooltips: false, tasks: [
    {id: 'Role Z4', name: 'Role Z4', color: 'blue', from: new Date(2013, 9, 21, 8, 0, 0), to: new Date(2013, 9, 25, 15, 0, 0),
        progress: 25, dependencies: [{to: 'Product A4'}]}
]},
{name: 'Role Y4', tasks: [
    {id: 'Role Y4', name: 'Role Y4', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 28, 15, 0, 0),
        dependencies: [{to: 'Product C4'}, {to: 'Role W4'}]}
]},
{name: 'Role X4', tasks: [
    {id: 'Role X4', name: 'Role X4', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 27, 15, 0, 0),
        dependencies: {to: 'Product B4'}}
]},
{name: 'Role W4', tasks: [
    {id: 'Role W4', name: 'Role W4', color: 'blue', from: new Date(2013, 9, 30, 15, 0, 0), to: new Date(2013, 9, 30, 15, 0, 0),
        dependencies: [{to: 'Product D4'}]}
]},
{name: 'Products4', children: ['Product A4', 'Product B4', 'Product C4', 'Product D4'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Product A4', tooltips: false, tasks: [
    {id: 'Product A4', name: 'Product A4', color: '#F1C234', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 10, 25, 15, 0, 0),
        progress: 25}
]},
{name: 'Product B4', tasks: [
    {id: 'Product B4', name: 'Product B4', color: '#F1C234', from: new Date(2013, 9, 28, 15, 0, 0), to: new Date(2013, 10, 15, 15, 0, 0)}
]},
{name: 'Product C4', tasks: [
    {id: 'Product C4', name: 'Product C4', color: '#F1C234', from: new Date(2013, 9, 29, 15, 0, 0), to: new Date(2013, 9, 29, 15, 0, 0)}
]},
{name: 'Product D4', tasks: [
    {id: 'Product D4', name: 'Product D4', color: '#F1C234', from: new Date(2013, 10, 1, 15, 0, 0), to: new Date(2013, 10, 1, 15, 0, 0)}
]},
{name: 'Worflow 5', children: ['Roles5', 'Products5'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Roles5', children: ['Role Z5', 'Role Y5', 'Role X5', 'Role W5'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{id: 'Role Z5', name: 'Role Z5', tooltips: false, tasks: [
    {id: 'Role Z5', name: 'Role Z5', color: 'blue', from: new Date(2013, 9, 21, 8, 0, 0), to: new Date(2013, 9, 25, 15, 0, 0),
        progress: 25, dependencies: [{to: 'Product A5'}]}
]},
{name: 'Role Y5', tasks: [
    {id: 'Role Y5', name: 'Role Y5', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 28, 15, 0, 0),
        dependencies: [{to: 'Product C5'}, {to: 'Role W5'}]}
]},
{name: 'Role X5', tasks: [
    {id: 'Role X5', name: 'Role X5', color: 'blue', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 9, 27, 15, 0, 0),
        dependencies: {to: 'Product B5'}}
]},
{name: 'Role W5', tasks: [
    {id: 'Role W5', name: 'Role W5', color: 'blue', from: new Date(2013, 9, 30, 15, 0, 0), to: new Date(2013, 9, 30, 15, 0, 0),
        dependencies: [{to: 'Product D5'}]}
]},
{name: 'Products5', children: ['Product A5', 'Product B5', 'Product C5', 'Product D5'], content: '<i class="fa fa-file-code-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'},
{name: 'Product A5', tooltips: false, tasks: [
    {id: 'Product A5', name: 'Product A5', color: '#F1C235', from: new Date(2013, 9, 26, 15, 0, 0), to: new Date(2013, 10, 25, 15, 0, 0),
        progress: 25}
]},
{name: 'Product B5', tasks: [
    {id: 'Product B5', name: 'Product B5', color: '#F1C235', from: new Date(2013, 9, 28, 15, 0, 0), to: new Date(2013, 10, 15, 15, 0, 0)}
]},
{name: 'Product C5', tasks: [
    {id: 'Product C5', name: 'Product C5', color: '#F1C235', from: new Date(2013, 9, 29, 15, 0, 0), to: new Date(2013, 9, 29, 15, 0, 0)}
]},
{name: 'Product D5', tasks: [
    {id: 'Product D5', name: 'Product D5', color: '#F1C235', from: new Date(2013, 10, 1, 15, 0, 0), to: new Date(2013, 10, 1, 15, 0, 0)}
]},


              ],
  };


  $scope.initialize = function() {
    return; //JEM FixMe
    WorkflowService.getWorkflows().then(function(workflowList) {
      $scope.workflowList = workflowList;
      setContainerSize();

      InstanceService.getInstances().then(function(instanceList) {
        $scope.instanceList = instanceList;
        $scope.instanceList.forEach(function(instance) {
          instance.workflow = $scope.getWorkFlow(instance.workflowId);
        });
        $scope.displayedInstanceCollection = [].concat($scope.instanceList);

        //Select first row in table
        if ($scope.displayedInstanceCollection.length > 0) {
          $scope.selectRow($scope.displayedInstanceCollection[$scope.displayedInstanceCollection.length - 1]);
        }

      }, function(result) {
        growl.error("Failed getting instances. Error: " + result.data.error);
        $scope.instanceList.length = 0;
      });

    }, function(result) {
      growl.error("Failed getting workflows. Error: " + result.data.error);
      $scope.workflowList.length = 0;
    });

  };

  $scope.getWorkFlow = function(workflowId) {
    return $scope.workflowList.filter(function(item) {
      return item.id === workflowId;
    })[0];
  };

  $scope.getTagList = function(uri) {
    var ret = "";
    var maxTags = 10;
    $scope.activeInstance.instanceTags.forEach(function(instanceTag) {
      var numOfTags = Math.min(instanceTag.tags.length, maxTags);
      if (instanceTag.product === uri) {
        for (var i = 0; i < numOfTags; i++) {
          ret += $scope.getUriLabel(instanceTag.tags[i]) + ", ";
        }
      }
      ret = ret.slice(0, -2); //Remove last comma
      if (ret !== "" && instanceTag.tags.length > maxTags) {
        ret += "... (click product to show all tags)";
      }
    });
    return ret;
  };

  $scope.getUriLabel = function(uri) {
    if (uri.lastIndexOf("#") === -1) {
      return uri.substring(uri.lastIndexOf("/") + 1);
    } else {
      return uri.substring(uri.lastIndexOf("#") + 1);
    }
  };
  $scope.initializeWorkflowDiagram = function(instance) {
    $scope.activeInstance = instance;
    $scope.selectedInstanceTag = undefined;
    $scope.graphLayout.zoomLevel = 1;
    $scope.initGanttData();
    setTimeout(function() {

      //Restore joint diagram div to orignal state
      document.getElementById('pnlDiagram').innerHTML = '<joint-diagram id="joint-diagram" />';

      WorkflowGraphFactory.initCustomSize('#joint-diagram', $('#pnlDiagram').width(), 500);

      WorkflowService.getWorkflow(instance.workflow.id).then(function(workflow) {
        WorkflowGraphFactory.loadData(workflow.graphItems, workflow.graphLinks);

        WorkflowGraphFactory.getPaper().on('cell:pointerup', function(cellView, evt, x, y) {
          $log.debug('cell view ' + cellView.model.id + ' was double clicked');
          $scope.selectedInstanceTag = undefined;
          $scope.activeInstance.instanceTags.forEach(function(instanceTag) {
            if (instanceTag.product === cellView.model.attributes.uri) {
              $scope.selectedInstanceTag = instanceTag;
              $('#container').animate({
                scrollTop: $("#tagRegion").offset().top
              }, 500);
            }
          });
          $scope.$apply();
        });

        WorkflowGraphFactory.getPaper().on('cell:mouseover', function(cellView, evt) {
          var cellType = cellView.model.attributes.type;
          if (cellType === "bbn.Role" || cellType === "bbn.Product") {
            if (!cellView.$el.data('bs.popover')) {
              var ontology = cellView.model.attributes.ontology;
              var classURI = cellView.model.attributes.uri;
              var tagList = $scope.getTagList(cellView.model.attributes.uri);
              if (tagList !== "") {
                tagList = "<br>Tags: " + tagList;
              }
              RoleProductSetService.getClassDetails(ontology.dataset, classURI).then(function(result) {
                $('.popover').popover('hide'); //Hide any lingering popovers
                cellView.$el.popover({
                  placement: 'auto',
                  trigger: 'hover',
                  html: true,
                  delay: {
                    "show": 250,
                    "hide": 0
                  },
                  container: 'body',
                  content: 'Ontology: (' + ontology.name + ')<br>Comment: ' + (result.description === "" ? "none" : result.description) + tagList
                }).popover('show');
              }, function() {});
              $('.popover').popover('hide');
            }
          }
        });

        var graphItems = WorkflowGraphFactory.getGraph().getCells();

        instance.roleAssets.forEach(function(roleAsset) {
          var assetLabel = "";

          roleAsset.assets.forEach(function(asset) {
            assetLabel += $scope.getUriLabel(asset) + ", ";
          });
          assetLabel = assetLabel.slice(0, -2);

          if (assetLabel !== "") { //Replace original labels with assets

            var roleAssetGraphItems = graphItems.filter(function(item) {
              return item.attributes.graphItemId === roleAsset.graphItem;
            });

            roleAssetGraphItems.forEach(function(roleAssetGraphItem) {
              roleAssetGraphItem.attr('text/text', joint.util.breakText(assetLabel + "\n" + roleAssetGraphItem.attributes.label, {
                width: 100
              }));
            });
          }


          $scope.createGanttRole($scope.getUriLabel(roleAsset.role), assetLabel);

        });

        instance.instanceTags.forEach(function(instanceTag) {
          graphItems.forEach(function(graphItem) {
            if (instanceTag.product === graphItem.attributes.uri) {
              graphItem.attr('text/text', graphItem.attr('text/text') + "\n[Tags]");
            }
          });

          $scope.createGanttProduct($scope.getUriLabel(instanceTag.product));
        });


        //Hide delete icon on shapes and links
        $(".element-tool-remove").hide();
        $(".tool-remove").hide();
        WorkflowGraphFactory.getPaper().scaleContentToFit();
        // $scope.gantt.api.side.setWidth($scope.gantt.api.columns.getColumnsWidthToFit());
        $scope.gantt.api.side.setWidth(450);
      }, function(result) {
        growl.error("Failed to acquire role-product set. Error: " + result.data.error);
      });
    }, 100);
  };


  $scope.navigateToWorkflow = function(workflow) {
    WorkflowTemplate.setId(workflow.id);
    $state.go('workflows.editor', {
      workflowId: workflow.id
    });
  };

  $scope.selectRow = function(row) {
    for (var i = 0, len = $scope.displayedInstanceCollection.length; i < len; i += 1) {
      $scope.displayedInstanceCollection[i].isSelected = false;
    }
    row.isSelected = true;
    $scope.initializeWorkflowDiagram(row);
  };

  $scope.changeGraphZoom = function() {
    WorkflowGraphFactory.getPaper().scale($scope.graphLayout.zoomLevel, $scope.graphLayout.zoomLevel);
  };

  $scope.initGanttData = function() {
    $scope.gantt.data = [{
      name: 'Products',
      children: [],
      content: '<i class="fa fa-file-powerpoint-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'
    }, {
      name: 'Roles',
      children: [],
      content: '<i class="fa fa-file-o" ng-click="scope.handleRowIconClick(row.model)"></i> {{row.model.name}}'
    }];
  };


  $scope.createGanttRole = function(roleName, assestName) {
    var ganttRole = {
      name: roleName,
      tooltips: true,
      tasks: []
    };
    if (assestName !== "") {
      var task = {
        name: assestName,
        color: 'salmon',
        from: moment($scope.activeInstance.creationDate).add(1, 'days'),
        to: moment($scope.activeInstance.creationDate).add(3, 'days')
      };
      ganttRole.tasks.push(task);
    }
    $scope.gantt.data.push(ganttRole);
    $scope.gantt.data[1].children.push(roleName);
  };
  $scope.createGanttProduct = function(productName) {
    var ganttProduct = {
      name: productName,
      tooltips: true,
      tasks: []
    };

    var task = {
      name: productName,
      color: 'blue',
      from: moment($scope.activeInstance.creationDate),
      to: moment($scope.activeInstance.creationDate).add(1, 'days')
    };
    ganttProduct.tasks.push(task);
    $scope.gantt.data.push(ganttProduct);
    $scope.gantt.data[0].children.push(productName);
  };
  $scope.registerApi = function(api) {
    $scope.gantt.api = api;
  };

}
