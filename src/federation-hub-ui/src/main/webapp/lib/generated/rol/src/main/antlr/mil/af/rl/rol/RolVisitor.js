// Generated from rol/src/main/antlr/mil/af/rl/rol/Rol.g4 by ANTLR 4.5.1
// jshint ignore: start
var antlr4 = require('antlr4/index');

// This class defines a complete generic visitor for a parse tree produced by RolParser.

function RolVisitor() {
	antlr4.tree.ParseTreeVisitor.call(this);
	return this;
}

RolVisitor.prototype = Object.create(antlr4.tree.ParseTreeVisitor.prototype);
RolVisitor.prototype.constructor = RolVisitor;

// Visit a parse tree produced by RolParser#program.
RolVisitor.prototype.visitProgram = function(ctx) {
};


// Visit a parse tree produced by RolParser#statement.
RolVisitor.prototype.visitStatement = function(ctx) {
};


// Visit a parse tree produced by RolParser#operation.
RolVisitor.prototype.visitOperation = function(ctx) {
};


// Visit a parse tree produced by RolParser#resource.
RolVisitor.prototype.visitResource = function(ctx) {
};


// Visit a parse tree produced by RolParser#entity.
RolVisitor.prototype.visitEntity = function(ctx) {
};


// Visit a parse tree produced by RolParser#assignment.
RolVisitor.prototype.visitAssignment = function(ctx) {
};


// Visit a parse tree produced by RolParser#assertions.
RolVisitor.prototype.visitAssertions = function(ctx) {
};


// Visit a parse tree produced by RolParser#assertion.
RolVisitor.prototype.visitAssertion = function(ctx) {
};


// Visit a parse tree produced by RolParser#constraintsClause.
RolVisitor.prototype.visitConstraintsClause = function(ctx) {
};


// Visit a parse tree produced by RolParser#constraints.
RolVisitor.prototype.visitConstraints = function(ctx) {
};


// Visit a parse tree produced by RolParser#constraintsLeaf.
RolVisitor.prototype.visitConstraintsLeaf = function(ctx) {
};


// Visit a parse tree produced by RolParser#constraintsParen.
RolVisitor.prototype.visitConstraintsParen = function(ctx) {
};


// Visit a parse tree produced by RolParser#constraintsUnaryOp.
RolVisitor.prototype.visitConstraintsUnaryOp = function(ctx) {
};


// Visit a parse tree produced by RolParser#simpleLeafConstraint.
RolVisitor.prototype.visitSimpleLeafConstraint = function(ctx) {
};


// Visit a parse tree produced by RolParser#binaryOp.
RolVisitor.prototype.visitBinaryOp = function(ctx) {
};


// Visit a parse tree produced by RolParser#unaryOp.
RolVisitor.prototype.visitUnaryOp = function(ctx) {
};


// Visit a parse tree produced by RolParser#role.
RolVisitor.prototype.visitRole = function(ctx) {
};


// Visit a parse tree produced by RolParser#product.
RolVisitor.prototype.visitProduct = function(ctx) {
};


// Visit a parse tree produced by RolParser#parameters.
RolVisitor.prototype.visitParameters = function(ctx) {
};


// Visit a parse tree produced by RolParser#stringParamValue.
RolVisitor.prototype.visitStringParamValue = function(ctx) {
};


// Visit a parse tree produced by RolParser#identParamValue.
RolVisitor.prototype.visitIdentParamValue = function(ctx) {
};


// Visit a parse tree produced by RolParser#valuearray.
RolVisitor.prototype.visitValuearray = function(ctx) {
};


// Visit a parse tree produced by RolParser#value.
RolVisitor.prototype.visitValue = function(ctx) {
};



exports.RolVisitor = RolVisitor;
