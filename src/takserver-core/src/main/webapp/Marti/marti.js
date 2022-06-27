
VariableType = {
   FLOAT : "float",
   BOOLEAN : "boolean",
   STRING : "string"
};

function getType(str) {
   var fltVal = parseFloat(str);
   if( !isNaN(fltVal) ) {
      return VariableType.FLOAT;
   }
   if( str.toLowerCase() == "true" 
      || str.toLowerCase() == "false" ) {
      return VariableType.BOOLEAN;
   }
   return VariableType.STRING;
};

function checkValue(val, orig_text) {
   return getType(orig_text) == getType(val);
};

function attrName(td) {
   return td[0].parentNode.cells[0].innerHTML;
};
