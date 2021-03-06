/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/

document.addEventListener('DOMContentLoaded', function () {
    ng.platform.browser.bootstrap(SampleAppComponent);
});

var SampleAppComponent = function() {
    // put columnDefs directly onto the controller
    this.columnDefs = [
        {headerName: "Make", field: "make"},
        {headerName: "Model", field: "model"},
        {headerName: "Price", field: "price"}
    ];
    // put data directly onto the controller
    this.rowData = [
        {make: "Toyota", model: "Celica", price: 35000},
        {make: "Ford", model: "Mondeo", price: 32000},
        {make: "Porsche", model: "Boxter", price: 72000}
    ];
};

// the template is simple, just include ag-Grid
var templateForSampleAppComponent =
    '<ag-grid-ng2 ' +
        // use one of the ag-Grid themes
        'class="ag-fresh" ' +
        // give some size to the grid
        'style="height: 100%;" ' +
        // use AngularJS 2 properties for column-defs and row-data
        '[columnDefs]="columnDefs" ' +
        '[rowData]="rowData" ' +
    '></ag-grid-ng2>';

SampleAppComponent.annotations = [
    new ng.core.Component({
        selector: 'simple-ng2-grid'
    }),
    new ng.core.View({
        // register the ag-Grid directive with this directive
        directives: [ag.grid.AgGridNg2],
        template: templateForSampleAppComponent
    })
];
