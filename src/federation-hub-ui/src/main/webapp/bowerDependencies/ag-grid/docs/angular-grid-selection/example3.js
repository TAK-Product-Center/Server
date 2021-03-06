/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/

var module = angular.module("example", ["agGrid"]);

module.controller("exampleCtrl", function($scope, $http) {

    var columnDefs = [
        {headerName: "Athlete", field: "athlete", width: 150},
        {headerName: "Age", field: "age", width: 90},
        {headerName: "Country", field: "country", width: 120},
        {headerName: "Year", field: "year", width: 90},
        {headerName: "Date", field: "date", width: 110},
        {headerName: "Sport", field: "sport", width: 110},
        {headerName: "Gold", field: "gold", width: 100},
        {headerName: "Silver", field: "silver", width: 100},
        {headerName: "Bronze", field: "bronze", width: 100},
        {headerName: "Total", field: "total", width: 100}
    ];

    $scope.gridOptions = {
        columnDefs: columnDefs,
        rowSelection: 'multiple',
        rowData: null,
        onRowSelected: rowSelectedFunc,
        onRowDeselected: rowDeselectedFunc,
        onSelectionChanged: selectionChangedFunc
    };

    function rowDeselectedFunc(event) {
        window.alert("row " + event.node.data.athlete + " de-selected");
    }

    function rowSelectedFunc(event) {
        window.alert("row " + event.node.data.athlete + " selected");
    }

    function selectionChangedFunc(event) {
        window.alert('selection changed, ' + event.selectedRows.length + ' rows selected');
    }

    $http.get("../olympicWinners.json")
        .then(function(res){
            $scope.gridOptions.api.setRowData(res.data);
        });
});
