/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
'use strict';

var gulp = require('gulp');

//////////////////////////////////////////////////////////////////////////////
// TASK
//////////////////////////////////////////////////////////////////////////////

gulp.task('default', ['jshint', 'karma']);
gulp.task('serve', ['dist', 'continuousMode']);
gulp.task('dist', ['clean:dist', 'uglify'], function(){
  return gulp.src('*.css', { cwd: './src' })
    .pipe(gulp.dest('./dist'));
});



//////////////////////////////////////////////////////////////////////////////
// MORE
//////////////////////////////////////////////////////////////////////////////
var karmaHelper = require('node-karma-wrapper');

var lodash = {};
lodash.assign = require('lodash.assign');
lodash.after = require('lodash.after');

//
// ACCESS TO THE ANGULAR-UI-PUBLISHER
// inspired by https://github.com/angular-ui/angular-ui-publisher
function targetTask(){
  var spawn = require('child_process').spawn;

  return function(done){
    // I'm using the global gulp "" ci ""
    spawn('gulp', process.argv.slice(2), {
      cwd : './node_modules/angular-ui-publisher',
      stdio: 'inherit'
    }).on('close', done);
  }
}

gulp.task('build', targetTask('build'));
gulp.task('publish', targetTask('publish'));
//
//




//////////////////////////////////////////////////////////////////////////////
// KARMA
//////////////////////////////////////////////////////////////////////////////

var kwjQlite = karmaHelper( testConfig( './test/karma-jqlite.conf.js' ));
var kwjQuery = karmaHelper( testConfig( './test/karma-jquery.conf.js' ));

function testConfig(configFile, customOptions){
  var options = { configFile: configFile };
  var travisOptions = process.env.TRAVIS && { browsers: [ 'Firefox', 'PhantomJS'], reporters: ['dots'], singleRun: true  };
  return lodash.assign(options, customOptions, travisOptions);
}


gulp.task('karma', function(cb){
  var done = lodash.after(2, cb);
  kwjQuery.simpleRun(done);
  kwjQlite.simpleRun(done);
});

gulp.task('karma:jqlite:unit', kwjQlite.simpleRun);
gulp.task('karma:jqlite:watch', function(){
  kwjQlite.inBackground();
  gulp.watch('./test/**', function(){
    kwjQlite.run();
  });
});

gulp.task('karma:jquery:unit', kwjQuery.simpleRun);
gulp.task('karma:jquery:watch', function(){
  kwjQuery.inBackground();
  gulp.watch('./test/**', function(){
    kwjQuery.run();
  });
});



//////////////////////////////////////////////////////////////////////////////
// OTHER
//////////////////////////////////////////////////////////////////////////////
// load plugins
var $ = require('gulp-load-plugins')();

gulp.task('clean:dist', function () {
  return gulp.src('./dist')
    .pipe($.rimraf());
});

gulp.task('ngmin', function () {
  var options = {
    remove: true,
    add: true,
    single_quotes: true
  };
  return gulp.src('*.js', { cwd: './src' })
    .pipe($.ngAnnotate(options))
    .pipe(gulp.dest('./dist'));
});


//////////////////////////////////////////////////////////////////////////////
// LINTING
//////////////////////////////////////////////////////////////////////////////

gulp.task('jshint:src', function(done){
  return gulp.src('./src/*.js')
    .pipe($.jshint())
    .pipe($.jshint.reporter('jshint-stylish'))
    .pipe($.jshint.reporter('fail'));
});

gulp.task('jshint:test', function(done){
  return gulp.src('./test/*.spec.js')
    .pipe($.jshint({
      globals: {
        "angular"    : false,
        "inject"    : false,
        "_jQuery"    : false,

        "jasmine"    : false,
        "it"         : false,
        "iit"        : false,
        "xit"        : false,
        "describe"   : false,
        "ddescribe"  : false,
        "xdescribe"  : false,
        "dump"      : false,
        "beforeEach" : false,
        "afterEach"  : false,
        "expect"     : false,
        "spyOn"      : false
      }
    }))
    .pipe($.jshint.reporter('jshint-stylish'))
    .pipe($.jshint.reporter('fail'));
});

gulp.task('jshint', ['jshint:src', 'jshint:test']);

//////////////////////////////////////////////////////////////////////////////
// MINIFYING
//////////////////////////////////////////////////////////////////////////////
gulp.task('uglify', ['ngmin'], function () {
  return gulp.src('./dist/*.js')
    .pipe($.rename({ suffix: '.min'}))
    .pipe($.uglify({mangle: false}))
    .pipe(gulp.dest('./dist'));
});



//////////////////////////////////////////////////////////////////////////////
// CONTINUOUS MODE
//////////////////////////////////////////////////////////////////////////////
gulp.task('continuousMode', function(){
  kwjQuery.inBackground();
  kwjQlite.inBackground();

  gulp.task('_continuousMode:runTests', function(cb){
    var done = lodash.after(2, cb);
    kwjQuery.run(done);
    kwjQlite.run(done);
  });

  // watch the tests
  gulp.watch('./test/**', ['jshint:test', '_continuousMode:runTests']);

  gulp.watch('./src/**', ['jshint:src', '_continuousMode:runTests']);

});
