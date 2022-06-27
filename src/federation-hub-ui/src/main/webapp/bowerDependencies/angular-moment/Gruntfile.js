/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/

'use strict';

module.exports = function (grunt) {
	// Load grunt tasks automatically
	require('load-grunt-tasks')(grunt);

	grunt.initConfig({
		karma: {
			unit: {
				configFile: 'karma.conf.js',
				singleRun: true
			}
		},
		jshint: {
			options: {
				jshintrc: '.jshintrc'
			},
			all: [
				'Gruntfile.js',
				'angular-moment.js',
				'tests.js'
			]
		},
		uglify: {
			dist: {
				options: {
					sourceMap: true
				},
				files: {
					'angular-moment.min.js': 'angular-moment.js'
				}
			}
		}
	});

	grunt.registerTask('test', [
		'jshint',
		'karma'
	]);

	grunt.registerTask('build', [
		'jshint',
		'uglify'
	]);

	grunt.registerTask('default', ['build']);
};
