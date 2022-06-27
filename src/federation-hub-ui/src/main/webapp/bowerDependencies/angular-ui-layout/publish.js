/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
/* jshint node:true */

'use strict';

var fs = require('fs');

module.exports = function() {

  return {
    humaName : 'UI.Layout',
    repoName : 'ui-layout',
    inlineHTML : fs.readFileSync(__dirname + '/demo/demo.html'),
    inlineJS : 'angular.module(\'doc.ui-layout\', [\'ui.layout\', \'prettifyDirective\', \'ui.bootstrap\', \'plunker\' ]);',
    css : ['demo/demo.css', 'dist/ui-layout.css'],
    js : ['dist/ui-layout.js'],
    bowerData: {
      main: ['ui-layout.js', 'ui-layout.css']
    }
  };
};
