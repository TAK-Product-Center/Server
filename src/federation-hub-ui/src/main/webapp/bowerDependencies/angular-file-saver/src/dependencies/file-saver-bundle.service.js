/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
'use strict';

module.exports = function SaveAs() {
  return require('file-saver').saveAs || function() {};
};
