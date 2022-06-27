/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
(function (angular) {
  'use strict';

  // RFC4122 version 4 compliant UUID generator.
  // Based on: https://github.com/pnegri/uuid-js
  angular.module('uuid4', []).factory('uuid4', function () {

    var maxFromBits = function(bits) {
      return Math.pow(2, bits);
    };

    var limitUI04 = maxFromBits(4);
    var limitUI06 = maxFromBits(6);
    var limitUI08 = maxFromBits(8);
    var limitUI12 = maxFromBits(12);
    var limitUI14 = maxFromBits(14);
    var limitUI16 = maxFromBits(16);
    var limitUI32 = maxFromBits(32);
    var limitUI40 = maxFromBits(40);
    var limitUI48 = maxFromBits(48);

    var getRandomInt = function(min, max) {
      return Math.floor(Math.random() * (max - min + 1)) + min;
    }

    var randomUI06 = function() {
      return getRandomInt(0, limitUI06-1);
    };

    var randomUI08 = function() {
      return getRandomInt(0, limitUI08-1);
    };

    var randomUI12 = function() {
      return getRandomInt(0, limitUI12-1);
    };

    var randomUI16 = function() {
      return getRandomInt(0, limitUI16-1);
    };

    var randomUI32 = function() {
      return getRandomInt(0, limitUI32-1);
    };

    var randomUI48 = function() {
      return (0 | Math.random() * (1 << 30)) + (0 | Math.random() * (1 << 48 - 30)) * (1 << 30);
    };

    var paddedString =  function(string, length, z) {
      string = String(string);
      z = (!z) ? '0' : z;
      var i = length - string.length;
      for (; i > 0; i >>>= 1, z += z) {
        if (i & 1) {
          string = z + string;
        }
      }
      return string;
    };

    var fromParts = function(timeLow, timeMid, timeHiAndVersion, clockSeqHiAndReserved, clockSeqLow, node) {
      var hex = paddedString(timeLow.toString(16), 8)
                 + '-'
                 + paddedString(timeMid.toString(16), 4)
                 + '-'
                 + paddedString(timeHiAndVersion.toString(16), 4)
                 + '-'
                 + paddedString(clockSeqHiAndReserved.toString(16), 2)
                 + paddedString(clockSeqLow.toString(16), 2)
                 + '-'
                 + paddedString(node.toString(16), 12);
      return hex;
    };

    return {
      generate: function () {
        return fromParts(
          randomUI32(),
          randomUI16(),
          0x4000 | randomUI12(),
          0x80   | randomUI06(),
          randomUI08(),
          randomUI48()
        );
      },

      // addition by Ka-Jan to test for validity
      // Based on: http://stackoverflow.com/questions/7905929/how-to-test-valid-uuid-guid
      validate: function (uuid) {
        var testPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
        return testPattern.test(uuid);
      }
    };
  });

}(angular));
