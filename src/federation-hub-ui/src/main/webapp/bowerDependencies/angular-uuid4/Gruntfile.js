/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
module.exports = function( grunt ) {

  'use strict';

  var SRC = 'src';

  grunt.initConfig({
    pkg    : grunt.file.readJSON('package.json'),
    banner : '/* \n * <%= pkg.name %> <%= pkg.version %>\n * <%= pkg.homepage %>\n * \n * Licensed under the <%= pkg.license %> license\n */',
    uglify : {
      production : {
        src: [ SRC + '/**/*.js' ],
        dest: 'angular-uuid4.min.js'
      }
    },

    copy : {
      production : {
        files : [
          { src: SRC + '/angular-uuid4.js', dest : 'angular-uuid4.js' }
        ]
      }
    },

    karma : {
      spec: {
        configFile : 'karma.conf.js'
      }
    }
  });

  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-karma');

  grunt.registerTask('default', ['build']);
  grunt.registerTask('test', [ 'karma' ]);
  grunt.registerTask('build', [ 'test', 'copy', 'uglify']);
};
