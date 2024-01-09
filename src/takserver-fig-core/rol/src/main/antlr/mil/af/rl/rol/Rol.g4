// Resource Operation Language
// grammar

grammar Rol;

// start with this production
program : (statement)+ EOF;

statement : operation resource (assignment)? (constraintsClause)? (assertions)? (parameters)? SEMI ;

operation : 'create' | 'add' | 'remove' | 'update' | 'assign' | 'revoke' | 'get' | 'post' | 'put' | 'delete' | 'contains' | 'request' | 'announce' | 'disperse' | 'store' | 'enable' | 'disable' ;

resource : 'query' | 'subscription' | 'staticsubscription' | 'role' role | 'role' | 'priority' | 'resource' | 'package' | 'mission' | 'input' | 'federation' | 'federate' | 'federated_connection' | 'data_feed' ;

entity: 'MSG' | 'PUBLISHER' | 'CONSUMER' ;
assignment : 'for' entity ;

// assertions clause - execution decision
assertions : assertion (binaryOp assertion)* ;
assertion : 'matchrole' role | 'match attribute' parameter | LPAREN assertions RPAREN | unaryOp assertions;

// constraints clause - determine the domain subset for the specified operation
constraintsClause : 'constraint' constraints ;
constraints : constraint (binaryOp constraint)* ;
constraint :   simpleLeafConstraint      # constraintsLeaf
		     | LPAREN constraints RPAREN # constraintsParen
		     | unaryOp constraints       # constraintsUnaryOp
		     ;

simpleLeafConstraint : 'min' value | 'max' value ;

// binary (infix) operators
binaryOp : 'and' | 'or' ;

// unary (prefix) operator
unaryOp : 'not' ;

// AMT types
role : IDENT | STRING;
product : IDENT ;

// parameters
parameters : IDENT | LCBRACE parameter (',' parameter)* RCBRACE | LCBRACE RCBRACE ;
parameter : STRING
			':'
			value # stringParamValue
			|
			IDENT
			':'
			value # identParamValue
			;
valuearray : LSBRACKET value (',' value)* RSBRACKET | LSBRACKET RSBRACKET ;
value : STRING | IDENT | NUMBER | parameters | valuearray | 'true' | 'false' ;

// identifier
IDENT : [a-zA-Z0-9_.]+ ;

// primitive types
STRING :  DBLQUOTE (ESC | ~["\\])* DBLQUOTE ;
fragment ESC :   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

NUMBER : '-'? INT '.' [0-9]+ EXP? // 1.35, 1.35E-9, 0.3, -4.5
    |   '-'? INT EXP              // 1e10 -3e4
    |   '-'? INT                  // -3, 45
    ;
fragment INT : '0' | [1-9] [0-9]* ; // no leading zeros
fragment EXP : [Ee] [+\-]? INT ; // \- since - means "range" inside [...]

// punctuation
SEMI : ';' ;
LPAREN : '(' ;
RPAREN : ')' ;
LCBRACE : '{' ;
RCBRACE : '}' ;
LSBRACKET : '[' ;
RSBRACKET : ']' ;
DBLQUOTE : '"' ;

// ignore whitespace
WS : [ \r\t\n]+ -> skip ;
