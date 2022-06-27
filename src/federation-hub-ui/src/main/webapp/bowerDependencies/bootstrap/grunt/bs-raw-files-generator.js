/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/

'use strict';

var fs = require('fs');
var btoa = require('btoa');
var glob = require('glob');

function getFiles(type) {
  var files = {};
  var recursive = type === 'less';
  var globExpr = recursive ? '/**/*' : '/*';
  glob.sync(type + globExpr)
    .filter(function (path) {
      return type === 'fonts' ? true : new RegExp('\\.' + type + '$').test(path);
    })
    .forEach(function (fullPath) {
      var relativePath = fullPath.replace(/^[^/]+\//, '');
      files[relativePath] = type === 'fonts' ? btoa(fs.readFileSync(fullPath)) : fs.readFileSync(fullPath, 'utf8');
    });
  return 'var __' + type + ' = ' + JSON.stringify(files) + '\n';
}

module.exports = function generateRawFilesJs(grunt, banner) {
  if (!banner) {
    banner = '';
  }
  var dirs = ['js', 'less', 'fonts'];
  var files = banner + dirs.map(getFiles).reduce(function (combined, file) {
    return combined + file;
  }, '');
  var rawFilesJs = 'docs/assets/js/raw-files.min.js';
  try {
    fs.writeFileSync(rawFilesJs, files);
  } catch (err) {
    grunt.fail.warn(err);
  }
  grunt.log.writeln('File ' + rawFilesJs.cyan + ' created.');
};
