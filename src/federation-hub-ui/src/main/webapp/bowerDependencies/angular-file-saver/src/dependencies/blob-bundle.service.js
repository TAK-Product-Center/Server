/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
'use strict';

require('blob-tmp');

module.exports = function Blob($window) {
  return $window.Blob;
};
