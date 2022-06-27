//# Constants
var epsilon = 1E-6

//# Standard pixel size of 0.28mm, as defined by WMTS
var METERS_PER_PIXEL = 0.28E-3

//# Specific to EPSG:900913 / EPSG:3857
var METERS_PER_UNIT = 1.0

var scaleDenominator = []
scaleDenominator[0] = 5.590822639508929E8
scaleDenominator[1] = 2.7954113197544646E8
scaleDenominator[2] = 1.3977056598772323E8
scaleDenominator[3] = 6.988528299386162E7
scaleDenominator[4] = 3.494264149693081E7
scaleDenominator[5] = 1.7471320748465404E7
scaleDenominator[6] = 8735660.374232702
scaleDenominator[7] = 4367830.187116351
scaleDenominator[8] = 2183915.0935581755
scaleDenominator[9] = 1091957.5467790877
scaleDenominator[10] = 545978.7733895439
scaleDenominator[11] = 272989.38669477194
scaleDenominator[12] = 136494.69334738597
scaleDenominator[13] = 68247.34667369298
scaleDenominator[14] = 34123.67333684649
scaleDenominator[15] = 17061.836668423246
scaleDenominator[16] = 8530.918334211623
scaleDenominator[17] = 4265.4591671058115
scaleDenominator[18] = 2132.7295835529058
scaleDenominator[19] = 1066.3647917764529
scaleDenominator[20] = 533.1823958882264

var matrix_min_x = -2.0037508E7
var matrix_max_y = 2.003750834E7

//# Configurable
var tile_width = 256.0
var tile_height = 256.0

function getNumTilesForCoords(min_x, min_y, max_x, max_y) {
   var ret = []
   for(z = 0; z<scaleDenominator.length; z++) {
      var tiles = getTilesForCoords(min_x, min_y, max_x, max_y, z);
      ret[z] = (tiles.max_col - tiles.min_col + 1) * (tiles.max_row - tiles.min_row + 1);
   }
   return ret;
}

function getTilesForCoords(min_x, min_y, max_x, max_y, z) {
   var pixel_span = scaleDenominator[z] * (METERS_PER_PIXEL / METERS_PER_UNIT)
   var tile_span_x = tile_width * pixel_span
   var tile_span_y = tile_height * pixel_span
   
   var min_col = Math.floor((min_x - matrix_min_x) / tile_span_x + epsilon)
   var max_col = Math.floor((max_x - matrix_min_x) / tile_span_x - epsilon)
   var min_row = Math.floor((matrix_max_y - max_y) / tile_span_y + epsilon)
   var max_row = Math.floor((matrix_max_y - min_y) / tile_span_y - epsilon)
   
   return {
      "min_col" : min_col,
      "max_col" : max_col,
      "min_row" : min_row,
      "max_row" : max_row
   };
}

function getCoordsOfTile(min_col, max_col, min_row, max_row) {
   leftX = min_col * tile_span_x + matrix_min_x
   upperY = matrix_max_y - min_row * tile_span_y
   rightX = (max_col + 1) * tile_span_x + matrix_min_x
   lowerY = matrix_max_y - (max_row + 1) * tile_span_y
   return {
      "leftX" : leftX,
      "upperY" : upperY,
      "rightX" : rightX,
      "lowerY" : lowerY
   };
}

