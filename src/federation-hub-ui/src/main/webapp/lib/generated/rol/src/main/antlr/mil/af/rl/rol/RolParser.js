// Generated from rol/src/main/antlr/mil/af/rl/rol/Rol.g4 by ANTLR 4.5.1
// jshint ignore: start
var antlr4 = require('antlr4/index');
var RolListener = require('./RolListener').RolListener;
var RolVisitor = require('./RolVisitor').RolVisitor;

var grammarFileName = "Rol.g4";

var serializedATN = ["\u0003\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd",
    "\u0003\'\u00ba\u0004\u0002\t\u0002\u0004\u0003\t\u0003\u0004\u0004\t",
    "\u0004\u0004\u0005\t\u0005\u0004\u0006\t\u0006\u0004\u0007\t\u0007\u0004",
    "\b\t\b\u0004\t\t\t\u0004\n\t\n\u0004\u000b\t\u000b\u0004\f\t\f\u0004",
    "\r\t\r\u0004\u000e\t\u000e\u0004\u000f\t\u000f\u0004\u0010\t\u0010\u0004",
    "\u0011\t\u0011\u0004\u0012\t\u0012\u0004\u0013\t\u0013\u0004\u0014\t",
    "\u0014\u0004\u0015\t\u0015\u0003\u0002\u0006\u0002,\n\u0002\r\u0002",
    "\u000e\u0002-\u0003\u0002\u0003\u0002\u0003\u0003\u0003\u0003\u0003",
    "\u0003\u0005\u00035\n\u0003\u0003\u0003\u0005\u00038\n\u0003\u0003\u0003",
    "\u0005\u0003;\n\u0003\u0003\u0003\u0005\u0003>\n\u0003\u0003\u0003\u0003",
    "\u0003\u0003\u0004\u0003\u0004\u0003\u0005\u0003\u0005\u0003\u0005\u0003",
    "\u0005\u0003\u0005\u0003\u0005\u0005\u0005J\n\u0005\u0003\u0006\u0003",
    "\u0006\u0003\u0007\u0003\u0007\u0003\u0007\u0003\b\u0003\b\u0003\b\u0003",
    "\b\u0007\bU\n\b\f\b\u000e\bX\u000b\b\u0003\t\u0003\t\u0003\t\u0003\t",
    "\u0003\t\u0003\t\u0003\t\u0003\t\u0003\t\u0003\t\u0003\t\u0005\te\n",
    "\t\u0003\n\u0003\n\u0003\n\u0003\u000b\u0003\u000b\u0003\u000b\u0003",
    "\u000b\u0007\u000bn\n\u000b\f\u000b\u000e\u000bq\u000b\u000b\u0003\f",
    "\u0003\f\u0003\f\u0003\f\u0003\f\u0003\f\u0003\f\u0003\f\u0005\f{\n",
    "\f\u0003\r\u0003\r\u0003\r\u0003\r\u0005\r\u0081\n\r\u0003\u000e\u0003",
    "\u000e\u0003\u000f\u0003\u000f\u0003\u0010\u0003\u0010\u0003\u0011\u0003",
    "\u0011\u0003\u0012\u0003\u0012\u0003\u0012\u0003\u0012\u0007\u0012\u008f",
    "\n\u0012\f\u0012\u000e\u0012\u0092\u000b\u0012\u0003\u0012\u0003\u0012",
    "\u0003\u0012\u0003\u0012\u0005\u0012\u0098\n\u0012\u0003\u0013\u0003",
    "\u0013\u0003\u0013\u0003\u0013\u0003\u0013\u0003\u0013\u0005\u0013\u00a0",
    "\n\u0013\u0003\u0014\u0003\u0014\u0003\u0014\u0003\u0014\u0007\u0014",
    "\u00a6\n\u0014\f\u0014\u000e\u0014\u00a9\u000b\u0014\u0003\u0014\u0003",
    "\u0014\u0003\u0014\u0003\u0014\u0005\u0014\u00af\n\u0014\u0003\u0015",
    "\u0003\u0015\u0003\u0015\u0003\u0015\u0003\u0015\u0003\u0015\u0003\u0015",
    "\u0005\u0015\u00b8\n\u0015\u0003\u0015\u0002\u0002\u0016\u0002\u0004",
    "\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e ",
    "\"$&(\u0002\u0006\u0003\u0002\u0003\u0007\u0003\u0002\f\u000e\u0003",
    "\u0002\u0015\u0016\u0003\u0002\u001c\u001d\u00c1\u0002+\u0003\u0002",
    "\u0002\u0002\u00041\u0003\u0002\u0002\u0002\u0006A\u0003\u0002\u0002",
    "\u0002\bI\u0003\u0002\u0002\u0002\nK\u0003\u0002\u0002\u0002\fM\u0003",
    "\u0002\u0002\u0002\u000eP\u0003\u0002\u0002\u0002\u0010d\u0003\u0002",
    "\u0002\u0002\u0012f\u0003\u0002\u0002\u0002\u0014i\u0003\u0002\u0002",
    "\u0002\u0016z\u0003\u0002\u0002\u0002\u0018\u0080\u0003\u0002\u0002",
    "\u0002\u001a\u0082\u0003\u0002\u0002\u0002\u001c\u0084\u0003\u0002\u0002",
    "\u0002\u001e\u0086\u0003\u0002\u0002\u0002 \u0088\u0003\u0002\u0002",
    "\u0002\"\u0097\u0003\u0002\u0002\u0002$\u009f\u0003\u0002\u0002\u0002",
    "&\u00ae\u0003\u0002\u0002\u0002(\u00b7\u0003\u0002\u0002\u0002*,\u0005",
    "\u0004\u0003\u0002+*\u0003\u0002\u0002\u0002,-\u0003\u0002\u0002\u0002",
    "-+\u0003\u0002\u0002\u0002-.\u0003\u0002\u0002\u0002./\u0003\u0002\u0002",
    "\u0002/0\u0007\u0002\u0002\u00030\u0003\u0003\u0002\u0002\u000212\u0005",
    "\u0006\u0004\u000224\u0005\b\u0005\u000235\u0005\f\u0007\u000243\u0003",
    "\u0002\u0002\u000245\u0003\u0002\u0002\u000257\u0003\u0002\u0002\u0002",
    "68\u0005\u0012\n\u000276\u0003\u0002\u0002\u000278\u0003\u0002\u0002",
    "\u00028:\u0003\u0002\u0002\u00029;\u0005\u000e\b\u0002:9\u0003\u0002",
    "\u0002\u0002:;\u0003\u0002\u0002\u0002;=\u0003\u0002\u0002\u0002<>\u0005",
    "\"\u0012\u0002=<\u0003\u0002\u0002\u0002=>\u0003\u0002\u0002\u0002>",
    "?\u0003\u0002\u0002\u0002?@\u0007\u001f\u0002\u0002@\u0005\u0003\u0002",
    "\u0002\u0002AB\t\u0002\u0002\u0002B\u0007\u0003\u0002\u0002\u0002CJ",
    "\u0007\b\u0002\u0002DJ\u0007\t\u0002\u0002EF\u0007\n\u0002\u0002FJ\u0005",
    "\u001e\u0010\u0002GJ\u0007\n\u0002\u0002HJ\u0007\u000b\u0002\u0002I",
    "C\u0003\u0002\u0002\u0002ID\u0003\u0002\u0002\u0002IE\u0003\u0002\u0002",
    "\u0002IG\u0003\u0002\u0002\u0002IH\u0003\u0002\u0002\u0002J\t\u0003",
    "\u0002\u0002\u0002KL\t\u0003\u0002\u0002L\u000b\u0003\u0002\u0002\u0002",
    "MN\u0007\u000f\u0002\u0002NO\u0005\n\u0006\u0002O\r\u0003\u0002\u0002",
    "\u0002PV\u0005\u0010\t\u0002QR\u0005\u001a\u000e\u0002RS\u0005\u0010",
    "\t\u0002SU\u0003\u0002\u0002\u0002TQ\u0003\u0002\u0002\u0002UX\u0003",
    "\u0002\u0002\u0002VT\u0003\u0002\u0002\u0002VW\u0003\u0002\u0002\u0002",
    "W\u000f\u0003\u0002\u0002\u0002XV\u0003\u0002\u0002\u0002YZ\u0007\u0010",
    "\u0002\u0002Ze\u0005\u001e\u0010\u0002[\\\u0007\u0011\u0002\u0002\\",
    "e\u0005$\u0013\u0002]^\u0007 \u0002\u0002^_\u0005\u000e\b\u0002_`\u0007",
    "!\u0002\u0002`e\u0003\u0002\u0002\u0002ab\u0005\u001c\u000f\u0002bc",
    "\u0005\u000e\b\u0002ce\u0003\u0002\u0002\u0002dY\u0003\u0002\u0002\u0002",
    "d[\u0003\u0002\u0002\u0002d]\u0003\u0002\u0002\u0002da\u0003\u0002\u0002",
    "\u0002e\u0011\u0003\u0002\u0002\u0002fg\u0007\u0012\u0002\u0002gh\u0005",
    "\u0014\u000b\u0002h\u0013\u0003\u0002\u0002\u0002io\u0005\u0016\f\u0002",
    "jk\u0005\u001a\u000e\u0002kl\u0005\u0016\f\u0002ln\u0003\u0002\u0002",
    "\u0002mj\u0003\u0002\u0002\u0002nq\u0003\u0002\u0002\u0002om\u0003\u0002",
    "\u0002\u0002op\u0003\u0002\u0002\u0002p\u0015\u0003\u0002\u0002\u0002",
    "qo\u0003\u0002\u0002\u0002r{\u0005\u0018\r\u0002st\u0007 \u0002\u0002",
    "tu\u0005\u0014\u000b\u0002uv\u0007!\u0002\u0002v{\u0003\u0002\u0002",
    "\u0002wx\u0005\u001c\u000f\u0002xy\u0005\u0014\u000b\u0002y{\u0003\u0002",
    "\u0002\u0002zr\u0003\u0002\u0002\u0002zs\u0003\u0002\u0002\u0002zw\u0003",
    "\u0002\u0002\u0002{\u0017\u0003\u0002\u0002\u0002|}\u0007\u0013\u0002",
    "\u0002}\u0081\u0005(\u0015\u0002~\u007f\u0007\u0014\u0002\u0002\u007f",
    "\u0081\u0005(\u0015\u0002\u0080|\u0003\u0002\u0002\u0002\u0080~\u0003",
    "\u0002\u0002\u0002\u0081\u0019\u0003\u0002\u0002\u0002\u0082\u0083\t",
    "\u0004\u0002\u0002\u0083\u001b\u0003\u0002\u0002\u0002\u0084\u0085\u0007",
    "\u0017\u0002\u0002\u0085\u001d\u0003\u0002\u0002\u0002\u0086\u0087\t",
    "\u0005\u0002\u0002\u0087\u001f\u0003\u0002\u0002\u0002\u0088\u0089\u0007",
    "\u001c\u0002\u0002\u0089!\u0003\u0002\u0002\u0002\u008a\u008b\u0007",
    "\"\u0002\u0002\u008b\u0090\u0005$\u0013\u0002\u008c\u008d\u0007\u0018",
    "\u0002\u0002\u008d\u008f\u0005$\u0013\u0002\u008e\u008c\u0003\u0002",
    "\u0002\u0002\u008f\u0092\u0003\u0002\u0002\u0002\u0090\u008e\u0003\u0002",
    "\u0002\u0002\u0090\u0091\u0003\u0002\u0002\u0002\u0091\u0093\u0003\u0002",
    "\u0002\u0002\u0092\u0090\u0003\u0002\u0002\u0002\u0093\u0094\u0007#",
    "\u0002\u0002\u0094\u0098\u0003\u0002\u0002\u0002\u0095\u0096\u0007\"",
    "\u0002\u0002\u0096\u0098\u0007#\u0002\u0002\u0097\u008a\u0003\u0002",
    "\u0002\u0002\u0097\u0095\u0003\u0002\u0002\u0002\u0098#\u0003\u0002",
    "\u0002\u0002\u0099\u009a\u0007\u001d\u0002\u0002\u009a\u009b\u0007\u0019",
    "\u0002\u0002\u009b\u00a0\u0005(\u0015\u0002\u009c\u009d\u0007\u001c",
    "\u0002\u0002\u009d\u009e\u0007\u0019\u0002\u0002\u009e\u00a0\u0005(",
    "\u0015\u0002\u009f\u0099\u0003\u0002\u0002\u0002\u009f\u009c\u0003\u0002",
    "\u0002\u0002\u00a0%\u0003\u0002\u0002\u0002\u00a1\u00a2\u0007$\u0002",
    "\u0002\u00a2\u00a7\u0005(\u0015\u0002\u00a3\u00a4\u0007\u0018\u0002",
    "\u0002\u00a4\u00a6\u0005(\u0015\u0002\u00a5\u00a3\u0003\u0002\u0002",
    "\u0002\u00a6\u00a9\u0003\u0002\u0002\u0002\u00a7\u00a5\u0003\u0002\u0002",
    "\u0002\u00a7\u00a8\u0003\u0002\u0002\u0002\u00a8\u00aa\u0003\u0002\u0002",
    "\u0002\u00a9\u00a7\u0003\u0002\u0002\u0002\u00aa\u00ab\u0007%\u0002",
    "\u0002\u00ab\u00af\u0003\u0002\u0002\u0002\u00ac\u00ad\u0007$\u0002",
    "\u0002\u00ad\u00af\u0007%\u0002\u0002\u00ae\u00a1\u0003\u0002\u0002",
    "\u0002\u00ae\u00ac\u0003\u0002\u0002\u0002\u00af\'\u0003\u0002\u0002",
    "\u0002\u00b0\u00b8\u0007\u001d\u0002\u0002\u00b1\u00b8\u0007\u001c\u0002",
    "\u0002\u00b2\u00b8\u0007\u001e\u0002\u0002\u00b3\u00b8\u0005\"\u0012",
    "\u0002\u00b4\u00b8\u0005&\u0014\u0002\u00b5\u00b8\u0007\u001a\u0002",
    "\u0002\u00b6\u00b8\u0007\u001b\u0002\u0002\u00b7\u00b0\u0003\u0002\u0002",
    "\u0002\u00b7\u00b1\u0003\u0002\u0002\u0002\u00b7\u00b2\u0003\u0002\u0002",
    "\u0002\u00b7\u00b3\u0003\u0002\u0002\u0002\u00b7\u00b4\u0003\u0002\u0002",
    "\u0002\u00b7\u00b5\u0003\u0002\u0002\u0002\u00b7\u00b6\u0003\u0002\u0002",
    "\u0002\u00b8)\u0003\u0002\u0002\u0002\u0013-47:=IVdoz\u0080\u0090\u0097",
    "\u009f\u00a7\u00ae\u00b7"].join("");


var atn = new antlr4.atn.ATNDeserializer().deserialize(serializedATN);

var decisionsToDFA = atn.decisionToState.map( function(ds, index) { return new antlr4.dfa.DFA(ds, index); });

var sharedContextCache = new antlr4.PredictionContextCache();

var literalNames = [ 'null', "'create'", "'remove'", "'update'", "'assign'", 
                     "'revoke'", "'query'", "'subscription'", "'role'", 
                     "'priority'", "'MSG'", "'PUBLISHER'", "'CONSUMER'", 
                     "'for'", "'matchrole'", "'match attribute'", "'constraint'", 
                     "'min'", "'max'", "'and'", "'or'", "'not'", "','", 
                     "':'", "'true'", "'false'", 'null', 'null', 'null', 
                     "';'", "'('", "')'", "'{'", "'}'", "'['", "']'", "'\"'" ];

var symbolicNames = [ 'null', 'null', 'null', 'null', 'null', 'null', 'null', 
                      'null', 'null', 'null', 'null', 'null', 'null', 'null', 
                      'null', 'null', 'null', 'null', 'null', 'null', 'null', 
                      'null', 'null', 'null', 'null', 'null', "IDENT", "STRING", 
                      "NUMBER", "SEMI", "LPAREN", "RPAREN", "LCBRACKET", 
                      "RCBRACKET", "LSBRACKET", "RSBRACKET", "DBLQUOTE", 
                      "WS" ];

var ruleNames =  [ "program", "statement", "operation", "resource", "entity", 
                   "assignment", "assertions", "assertion", "constraintsClause", 
                   "constraints", "constraint", "simpleLeafConstraint", 
                   "binaryOp", "unaryOp", "role", "product", "parameters", 
                   "parameter", "valuearray", "value" ];

function RolParser (input) {
	antlr4.Parser.call(this, input);
    this._interp = new antlr4.atn.ParserATNSimulator(this, atn, decisionsToDFA, sharedContextCache);
    this.ruleNames = ruleNames;
    this.literalNames = literalNames;
    this.symbolicNames = symbolicNames;
    return this;
}

RolParser.prototype = Object.create(antlr4.Parser.prototype);
RolParser.prototype.constructor = RolParser;

Object.defineProperty(RolParser.prototype, "atn", {
	get : function() {
		return atn;
	}
});

RolParser.EOF = antlr4.Token.EOF;
RolParser.T__0 = 1;
RolParser.T__1 = 2;
RolParser.T__2 = 3;
RolParser.T__3 = 4;
RolParser.T__4 = 5;
RolParser.T__5 = 6;
RolParser.T__6 = 7;
RolParser.T__7 = 8;
RolParser.T__8 = 9;
RolParser.T__9 = 10;
RolParser.T__10 = 11;
RolParser.T__11 = 12;
RolParser.T__12 = 13;
RolParser.T__13 = 14;
RolParser.T__14 = 15;
RolParser.T__15 = 16;
RolParser.T__16 = 17;
RolParser.T__17 = 18;
RolParser.T__18 = 19;
RolParser.T__19 = 20;
RolParser.T__20 = 21;
RolParser.T__21 = 22;
RolParser.T__22 = 23;
RolParser.T__23 = 24;
RolParser.T__24 = 25;
RolParser.IDENT = 26;
RolParser.STRING = 27;
RolParser.NUMBER = 28;
RolParser.SEMI = 29;
RolParser.LPAREN = 30;
RolParser.RPAREN = 31;
RolParser.LCBRACKET = 32;
RolParser.RCBRACKET = 33;
RolParser.LSBRACKET = 34;
RolParser.RSBRACKET = 35;
RolParser.DBLQUOTE = 36;
RolParser.WS = 37;

RolParser.RULE_program = 0;
RolParser.RULE_statement = 1;
RolParser.RULE_operation = 2;
RolParser.RULE_resource = 3;
RolParser.RULE_entity = 4;
RolParser.RULE_assignment = 5;
RolParser.RULE_assertions = 6;
RolParser.RULE_assertion = 7;
RolParser.RULE_constraintsClause = 8;
RolParser.RULE_constraints = 9;
RolParser.RULE_constraint = 10;
RolParser.RULE_simpleLeafConstraint = 11;
RolParser.RULE_binaryOp = 12;
RolParser.RULE_unaryOp = 13;
RolParser.RULE_role = 14;
RolParser.RULE_product = 15;
RolParser.RULE_parameters = 16;
RolParser.RULE_parameter = 17;
RolParser.RULE_valuearray = 18;
RolParser.RULE_value = 19;

function ProgramContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_program;
    return this;
}

ProgramContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ProgramContext.prototype.constructor = ProgramContext;

ProgramContext.prototype.EOF = function() {
    return this.getToken(RolParser.EOF, 0);
};

ProgramContext.prototype.statement = function(i) {
    if(i===undefined) {
        i = null;
    }
    if(i===null) {
        return this.getTypedRuleContexts(StatementContext);
    } else {
        return this.getTypedRuleContext(StatementContext,i);
    }
};

ProgramContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterProgram(this);
	}
};

ProgramContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitProgram(this);
	}
};

ProgramContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitProgram(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.ProgramContext = ProgramContext;

RolParser.prototype.program = function() {

    var localctx = new ProgramContext(this, this._ctx, this.state);
    this.enterRule(localctx, 0, RolParser.RULE_program);
    var _la = 0; // Token type
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 41; 
        this._errHandler.sync(this);
        _la = this._input.LA(1);
        do {
            this.state = 40;
            this.statement();
            this.state = 43; 
            this._errHandler.sync(this);
            _la = this._input.LA(1);
        } while((((_la) & ~0x1f) == 0 && ((1 << _la) & ((1 << RolParser.T__0) | (1 << RolParser.T__1) | (1 << RolParser.T__2) | (1 << RolParser.T__3) | (1 << RolParser.T__4))) !== 0));
        this.state = 45;
        this.match(RolParser.EOF);
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function StatementContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_statement;
    return this;
}

StatementContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
StatementContext.prototype.constructor = StatementContext;

StatementContext.prototype.operation = function() {
    return this.getTypedRuleContext(OperationContext,0);
};

StatementContext.prototype.resource = function() {
    return this.getTypedRuleContext(ResourceContext,0);
};

StatementContext.prototype.SEMI = function() {
    return this.getToken(RolParser.SEMI, 0);
};

StatementContext.prototype.assignment = function() {
    return this.getTypedRuleContext(AssignmentContext,0);
};

StatementContext.prototype.constraintsClause = function() {
    return this.getTypedRuleContext(ConstraintsClauseContext,0);
};

StatementContext.prototype.assertions = function() {
    return this.getTypedRuleContext(AssertionsContext,0);
};

StatementContext.prototype.parameters = function() {
    return this.getTypedRuleContext(ParametersContext,0);
};

StatementContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterStatement(this);
	}
};

StatementContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitStatement(this);
	}
};

StatementContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitStatement(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.StatementContext = StatementContext;

RolParser.prototype.statement = function() {

    var localctx = new StatementContext(this, this._ctx, this.state);
    this.enterRule(localctx, 2, RolParser.RULE_statement);
    var _la = 0; // Token type
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 47;
        this.operation();
        this.state = 48;
        this.resource();
        this.state = 50;
        _la = this._input.LA(1);
        if(_la===RolParser.T__12) {
            this.state = 49;
            this.assignment();
        }

        this.state = 53;
        _la = this._input.LA(1);
        if(_la===RolParser.T__15) {
            this.state = 52;
            this.constraintsClause();
        }

        this.state = 56;
        _la = this._input.LA(1);
        if((((_la) & ~0x1f) == 0 && ((1 << _la) & ((1 << RolParser.T__13) | (1 << RolParser.T__14) | (1 << RolParser.T__20) | (1 << RolParser.LPAREN))) !== 0)) {
            this.state = 55;
            this.assertions();
        }

        this.state = 59;
        _la = this._input.LA(1);
        if(_la===RolParser.LCBRACKET) {
            this.state = 58;
            this.parameters();
        }

        this.state = 61;
        this.match(RolParser.SEMI);
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function OperationContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_operation;
    return this;
}

OperationContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
OperationContext.prototype.constructor = OperationContext;


OperationContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterOperation(this);
	}
};

OperationContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitOperation(this);
	}
};

OperationContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitOperation(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.OperationContext = OperationContext;

RolParser.prototype.operation = function() {

    var localctx = new OperationContext(this, this._ctx, this.state);
    this.enterRule(localctx, 4, RolParser.RULE_operation);
    var _la = 0; // Token type
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 63;
        _la = this._input.LA(1);
        if(!((((_la) & ~0x1f) == 0 && ((1 << _la) & ((1 << RolParser.T__0) | (1 << RolParser.T__1) | (1 << RolParser.T__2) | (1 << RolParser.T__3) | (1 << RolParser.T__4))) !== 0))) {
        this._errHandler.recoverInline(this);
        }
        else {
            this.consume();
        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function ResourceContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_resource;
    return this;
}

ResourceContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ResourceContext.prototype.constructor = ResourceContext;

ResourceContext.prototype.role = function() {
    return this.getTypedRuleContext(RoleContext,0);
};

ResourceContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterResource(this);
	}
};

ResourceContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitResource(this);
	}
};

ResourceContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitResource(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.ResourceContext = ResourceContext;

RolParser.prototype.resource = function() {

    var localctx = new ResourceContext(this, this._ctx, this.state);
    this.enterRule(localctx, 6, RolParser.RULE_resource);
    try {
        this.state = 71;
        var la_ = this._interp.adaptivePredict(this._input,5,this._ctx);
        switch(la_) {
        case 1:
            this.enterOuterAlt(localctx, 1);
            this.state = 65;
            this.match(RolParser.T__5);
            break;

        case 2:
            this.enterOuterAlt(localctx, 2);
            this.state = 66;
            this.match(RolParser.T__6);
            break;

        case 3:
            this.enterOuterAlt(localctx, 3);
            this.state = 67;
            this.match(RolParser.T__7);
            this.state = 68;
            this.role();
            break;

        case 4:
            this.enterOuterAlt(localctx, 4);
            this.state = 69;
            this.match(RolParser.T__7);
            break;

        case 5:
            this.enterOuterAlt(localctx, 5);
            this.state = 70;
            this.match(RolParser.T__8);
            break;

        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function EntityContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_entity;
    return this;
}

EntityContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
EntityContext.prototype.constructor = EntityContext;


EntityContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterEntity(this);
	}
};

EntityContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitEntity(this);
	}
};

EntityContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitEntity(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.EntityContext = EntityContext;

RolParser.prototype.entity = function() {

    var localctx = new EntityContext(this, this._ctx, this.state);
    this.enterRule(localctx, 8, RolParser.RULE_entity);
    var _la = 0; // Token type
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 73;
        _la = this._input.LA(1);
        if(!((((_la) & ~0x1f) == 0 && ((1 << _la) & ((1 << RolParser.T__9) | (1 << RolParser.T__10) | (1 << RolParser.T__11))) !== 0))) {
        this._errHandler.recoverInline(this);
        }
        else {
            this.consume();
        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function AssignmentContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_assignment;
    return this;
}

AssignmentContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
AssignmentContext.prototype.constructor = AssignmentContext;

AssignmentContext.prototype.entity = function() {
    return this.getTypedRuleContext(EntityContext,0);
};

AssignmentContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterAssignment(this);
	}
};

AssignmentContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitAssignment(this);
	}
};

AssignmentContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitAssignment(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.AssignmentContext = AssignmentContext;

RolParser.prototype.assignment = function() {

    var localctx = new AssignmentContext(this, this._ctx, this.state);
    this.enterRule(localctx, 10, RolParser.RULE_assignment);
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 75;
        this.match(RolParser.T__12);
        this.state = 76;
        this.entity();
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function AssertionsContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_assertions;
    return this;
}

AssertionsContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
AssertionsContext.prototype.constructor = AssertionsContext;

AssertionsContext.prototype.assertion = function(i) {
    if(i===undefined) {
        i = null;
    }
    if(i===null) {
        return this.getTypedRuleContexts(AssertionContext);
    } else {
        return this.getTypedRuleContext(AssertionContext,i);
    }
};

AssertionsContext.prototype.binaryOp = function(i) {
    if(i===undefined) {
        i = null;
    }
    if(i===null) {
        return this.getTypedRuleContexts(BinaryOpContext);
    } else {
        return this.getTypedRuleContext(BinaryOpContext,i);
    }
};

AssertionsContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterAssertions(this);
	}
};

AssertionsContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitAssertions(this);
	}
};

AssertionsContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitAssertions(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.AssertionsContext = AssertionsContext;

RolParser.prototype.assertions = function() {

    var localctx = new AssertionsContext(this, this._ctx, this.state);
    this.enterRule(localctx, 12, RolParser.RULE_assertions);
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 78;
        this.assertion();
        this.state = 84;
        this._errHandler.sync(this);
        var _alt = this._interp.adaptivePredict(this._input,6,this._ctx)
        while(_alt!=2 && _alt!=antlr4.atn.ATN.INVALID_ALT_NUMBER) {
            if(_alt===1) {
                this.state = 79;
                this.binaryOp();
                this.state = 80;
                this.assertion(); 
            }
            this.state = 86;
            this._errHandler.sync(this);
            _alt = this._interp.adaptivePredict(this._input,6,this._ctx);
        }

    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function AssertionContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_assertion;
    return this;
}

AssertionContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
AssertionContext.prototype.constructor = AssertionContext;

AssertionContext.prototype.role = function() {
    return this.getTypedRuleContext(RoleContext,0);
};

AssertionContext.prototype.parameter = function() {
    return this.getTypedRuleContext(ParameterContext,0);
};

AssertionContext.prototype.LPAREN = function() {
    return this.getToken(RolParser.LPAREN, 0);
};

AssertionContext.prototype.assertions = function() {
    return this.getTypedRuleContext(AssertionsContext,0);
};

AssertionContext.prototype.RPAREN = function() {
    return this.getToken(RolParser.RPAREN, 0);
};

AssertionContext.prototype.unaryOp = function() {
    return this.getTypedRuleContext(UnaryOpContext,0);
};

AssertionContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterAssertion(this);
	}
};

AssertionContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitAssertion(this);
	}
};

AssertionContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitAssertion(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.AssertionContext = AssertionContext;

RolParser.prototype.assertion = function() {

    var localctx = new AssertionContext(this, this._ctx, this.state);
    this.enterRule(localctx, 14, RolParser.RULE_assertion);
    try {
        this.state = 98;
        switch(this._input.LA(1)) {
        case RolParser.T__13:
            this.enterOuterAlt(localctx, 1);
            this.state = 87;
            this.match(RolParser.T__13);
            this.state = 88;
            this.role();
            break;
        case RolParser.T__14:
            this.enterOuterAlt(localctx, 2);
            this.state = 89;
            this.match(RolParser.T__14);
            this.state = 90;
            this.parameter();
            break;
        case RolParser.LPAREN:
            this.enterOuterAlt(localctx, 3);
            this.state = 91;
            this.match(RolParser.LPAREN);
            this.state = 92;
            this.assertions();
            this.state = 93;
            this.match(RolParser.RPAREN);
            break;
        case RolParser.T__20:
            this.enterOuterAlt(localctx, 4);
            this.state = 95;
            this.unaryOp();
            this.state = 96;
            this.assertions();
            break;
        default:
            throw new antlr4.error.NoViableAltException(this);
        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function ConstraintsClauseContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_constraintsClause;
    return this;
}

ConstraintsClauseContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ConstraintsClauseContext.prototype.constructor = ConstraintsClauseContext;

ConstraintsClauseContext.prototype.constraints = function() {
    return this.getTypedRuleContext(ConstraintsContext,0);
};

ConstraintsClauseContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterConstraintsClause(this);
	}
};

ConstraintsClauseContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitConstraintsClause(this);
	}
};

ConstraintsClauseContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitConstraintsClause(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.ConstraintsClauseContext = ConstraintsClauseContext;

RolParser.prototype.constraintsClause = function() {

    var localctx = new ConstraintsClauseContext(this, this._ctx, this.state);
    this.enterRule(localctx, 16, RolParser.RULE_constraintsClause);
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 100;
        this.match(RolParser.T__15);
        this.state = 101;
        this.constraints();
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function ConstraintsContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_constraints;
    return this;
}

ConstraintsContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ConstraintsContext.prototype.constructor = ConstraintsContext;

ConstraintsContext.prototype.constraint = function(i) {
    if(i===undefined) {
        i = null;
    }
    if(i===null) {
        return this.getTypedRuleContexts(ConstraintContext);
    } else {
        return this.getTypedRuleContext(ConstraintContext,i);
    }
};

ConstraintsContext.prototype.binaryOp = function(i) {
    if(i===undefined) {
        i = null;
    }
    if(i===null) {
        return this.getTypedRuleContexts(BinaryOpContext);
    } else {
        return this.getTypedRuleContext(BinaryOpContext,i);
    }
};

ConstraintsContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterConstraints(this);
	}
};

ConstraintsContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitConstraints(this);
	}
};

ConstraintsContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitConstraints(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.ConstraintsContext = ConstraintsContext;

RolParser.prototype.constraints = function() {

    var localctx = new ConstraintsContext(this, this._ctx, this.state);
    this.enterRule(localctx, 18, RolParser.RULE_constraints);
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 103;
        this.constraint();
        this.state = 109;
        this._errHandler.sync(this);
        var _alt = this._interp.adaptivePredict(this._input,8,this._ctx)
        while(_alt!=2 && _alt!=antlr4.atn.ATN.INVALID_ALT_NUMBER) {
            if(_alt===1) {
                this.state = 104;
                this.binaryOp();
                this.state = 105;
                this.constraint(); 
            }
            this.state = 111;
            this._errHandler.sync(this);
            _alt = this._interp.adaptivePredict(this._input,8,this._ctx);
        }

    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function ConstraintContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_constraint;
    return this;
}

ConstraintContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ConstraintContext.prototype.constructor = ConstraintContext;


 
ConstraintContext.prototype.copyFrom = function(ctx) {
    antlr4.ParserRuleContext.prototype.copyFrom.call(this, ctx);
};


function ConstraintsLeafContext(parser, ctx) {
	ConstraintContext.call(this, parser);
    ConstraintContext.prototype.copyFrom.call(this, ctx);
    return this;
}

ConstraintsLeafContext.prototype = Object.create(ConstraintContext.prototype);
ConstraintsLeafContext.prototype.constructor = ConstraintsLeafContext;

RolParser.ConstraintsLeafContext = ConstraintsLeafContext;

ConstraintsLeafContext.prototype.simpleLeafConstraint = function() {
    return this.getTypedRuleContext(SimpleLeafConstraintContext,0);
};
ConstraintsLeafContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterConstraintsLeaf(this);
	}
};

ConstraintsLeafContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitConstraintsLeaf(this);
	}
};

ConstraintsLeafContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitConstraintsLeaf(this);
    } else {
        return visitor.visitChildren(this);
    }
};


function ConstraintsParenContext(parser, ctx) {
	ConstraintContext.call(this, parser);
    ConstraintContext.prototype.copyFrom.call(this, ctx);
    return this;
}

ConstraintsParenContext.prototype = Object.create(ConstraintContext.prototype);
ConstraintsParenContext.prototype.constructor = ConstraintsParenContext;

RolParser.ConstraintsParenContext = ConstraintsParenContext;

ConstraintsParenContext.prototype.LPAREN = function() {
    return this.getToken(RolParser.LPAREN, 0);
};

ConstraintsParenContext.prototype.constraints = function() {
    return this.getTypedRuleContext(ConstraintsContext,0);
};

ConstraintsParenContext.prototype.RPAREN = function() {
    return this.getToken(RolParser.RPAREN, 0);
};
ConstraintsParenContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterConstraintsParen(this);
	}
};

ConstraintsParenContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitConstraintsParen(this);
	}
};

ConstraintsParenContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitConstraintsParen(this);
    } else {
        return visitor.visitChildren(this);
    }
};


function ConstraintsUnaryOpContext(parser, ctx) {
	ConstraintContext.call(this, parser);
    ConstraintContext.prototype.copyFrom.call(this, ctx);
    return this;
}

ConstraintsUnaryOpContext.prototype = Object.create(ConstraintContext.prototype);
ConstraintsUnaryOpContext.prototype.constructor = ConstraintsUnaryOpContext;

RolParser.ConstraintsUnaryOpContext = ConstraintsUnaryOpContext;

ConstraintsUnaryOpContext.prototype.unaryOp = function() {
    return this.getTypedRuleContext(UnaryOpContext,0);
};

ConstraintsUnaryOpContext.prototype.constraints = function() {
    return this.getTypedRuleContext(ConstraintsContext,0);
};
ConstraintsUnaryOpContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterConstraintsUnaryOp(this);
	}
};

ConstraintsUnaryOpContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitConstraintsUnaryOp(this);
	}
};

ConstraintsUnaryOpContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitConstraintsUnaryOp(this);
    } else {
        return visitor.visitChildren(this);
    }
};



RolParser.ConstraintContext = ConstraintContext;

RolParser.prototype.constraint = function() {

    var localctx = new ConstraintContext(this, this._ctx, this.state);
    this.enterRule(localctx, 20, RolParser.RULE_constraint);
    try {
        this.state = 120;
        switch(this._input.LA(1)) {
        case RolParser.T__16:
        case RolParser.T__17:
            localctx = new ConstraintsLeafContext(this, localctx);
            this.enterOuterAlt(localctx, 1);
            this.state = 112;
            this.simpleLeafConstraint();
            break;
        case RolParser.LPAREN:
            localctx = new ConstraintsParenContext(this, localctx);
            this.enterOuterAlt(localctx, 2);
            this.state = 113;
            this.match(RolParser.LPAREN);
            this.state = 114;
            this.constraints();
            this.state = 115;
            this.match(RolParser.RPAREN);
            break;
        case RolParser.T__20:
            localctx = new ConstraintsUnaryOpContext(this, localctx);
            this.enterOuterAlt(localctx, 3);
            this.state = 117;
            this.unaryOp();
            this.state = 118;
            this.constraints();
            break;
        default:
            throw new antlr4.error.NoViableAltException(this);
        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function SimpleLeafConstraintContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_simpleLeafConstraint;
    return this;
}

SimpleLeafConstraintContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
SimpleLeafConstraintContext.prototype.constructor = SimpleLeafConstraintContext;

SimpleLeafConstraintContext.prototype.value = function() {
    return this.getTypedRuleContext(ValueContext,0);
};

SimpleLeafConstraintContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterSimpleLeafConstraint(this);
	}
};

SimpleLeafConstraintContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitSimpleLeafConstraint(this);
	}
};

SimpleLeafConstraintContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitSimpleLeafConstraint(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.SimpleLeafConstraintContext = SimpleLeafConstraintContext;

RolParser.prototype.simpleLeafConstraint = function() {

    var localctx = new SimpleLeafConstraintContext(this, this._ctx, this.state);
    this.enterRule(localctx, 22, RolParser.RULE_simpleLeafConstraint);
    try {
        this.state = 126;
        switch(this._input.LA(1)) {
        case RolParser.T__16:
            this.enterOuterAlt(localctx, 1);
            this.state = 122;
            this.match(RolParser.T__16);
            this.state = 123;
            this.value();
            break;
        case RolParser.T__17:
            this.enterOuterAlt(localctx, 2);
            this.state = 124;
            this.match(RolParser.T__17);
            this.state = 125;
            this.value();
            break;
        default:
            throw new antlr4.error.NoViableAltException(this);
        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function BinaryOpContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_binaryOp;
    return this;
}

BinaryOpContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
BinaryOpContext.prototype.constructor = BinaryOpContext;


BinaryOpContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterBinaryOp(this);
	}
};

BinaryOpContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitBinaryOp(this);
	}
};

BinaryOpContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitBinaryOp(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.BinaryOpContext = BinaryOpContext;

RolParser.prototype.binaryOp = function() {

    var localctx = new BinaryOpContext(this, this._ctx, this.state);
    this.enterRule(localctx, 24, RolParser.RULE_binaryOp);
    var _la = 0; // Token type
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 128;
        _la = this._input.LA(1);
        if(!(_la===RolParser.T__18 || _la===RolParser.T__19)) {
        this._errHandler.recoverInline(this);
        }
        else {
            this.consume();
        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function UnaryOpContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_unaryOp;
    return this;
}

UnaryOpContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
UnaryOpContext.prototype.constructor = UnaryOpContext;


UnaryOpContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterUnaryOp(this);
	}
};

UnaryOpContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitUnaryOp(this);
	}
};

UnaryOpContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitUnaryOp(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.UnaryOpContext = UnaryOpContext;

RolParser.prototype.unaryOp = function() {

    var localctx = new UnaryOpContext(this, this._ctx, this.state);
    this.enterRule(localctx, 26, RolParser.RULE_unaryOp);
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 130;
        this.match(RolParser.T__20);
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function RoleContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_role;
    return this;
}

RoleContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
RoleContext.prototype.constructor = RoleContext;

RoleContext.prototype.IDENT = function() {
    return this.getToken(RolParser.IDENT, 0);
};

RoleContext.prototype.STRING = function() {
    return this.getToken(RolParser.STRING, 0);
};

RoleContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterRole(this);
	}
};

RoleContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitRole(this);
	}
};

RoleContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitRole(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.RoleContext = RoleContext;

RolParser.prototype.role = function() {

    var localctx = new RoleContext(this, this._ctx, this.state);
    this.enterRule(localctx, 28, RolParser.RULE_role);
    var _la = 0; // Token type
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 132;
        _la = this._input.LA(1);
        if(!(_la===RolParser.IDENT || _la===RolParser.STRING)) {
        this._errHandler.recoverInline(this);
        }
        else {
            this.consume();
        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function ProductContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_product;
    return this;
}

ProductContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ProductContext.prototype.constructor = ProductContext;

ProductContext.prototype.IDENT = function() {
    return this.getToken(RolParser.IDENT, 0);
};

ProductContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterProduct(this);
	}
};

ProductContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitProduct(this);
	}
};

ProductContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitProduct(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.ProductContext = ProductContext;

RolParser.prototype.product = function() {

    var localctx = new ProductContext(this, this._ctx, this.state);
    this.enterRule(localctx, 30, RolParser.RULE_product);
    try {
        this.enterOuterAlt(localctx, 1);
        this.state = 134;
        this.match(RolParser.IDENT);
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function ParametersContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_parameters;
    return this;
}

ParametersContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ParametersContext.prototype.constructor = ParametersContext;

ParametersContext.prototype.LCBRACKET = function() {
    return this.getToken(RolParser.LCBRACKET, 0);
};

ParametersContext.prototype.parameter = function(i) {
    if(i===undefined) {
        i = null;
    }
    if(i===null) {
        return this.getTypedRuleContexts(ParameterContext);
    } else {
        return this.getTypedRuleContext(ParameterContext,i);
    }
};

ParametersContext.prototype.RCBRACKET = function() {
    return this.getToken(RolParser.RCBRACKET, 0);
};

ParametersContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterParameters(this);
	}
};

ParametersContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitParameters(this);
	}
};

ParametersContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitParameters(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.ParametersContext = ParametersContext;

RolParser.prototype.parameters = function() {

    var localctx = new ParametersContext(this, this._ctx, this.state);
    this.enterRule(localctx, 32, RolParser.RULE_parameters);
    var _la = 0; // Token type
    try {
        this.state = 149;
        var la_ = this._interp.adaptivePredict(this._input,12,this._ctx);
        switch(la_) {
        case 1:
            this.enterOuterAlt(localctx, 1);
            this.state = 136;
            this.match(RolParser.LCBRACKET);
            this.state = 137;
            this.parameter();
            this.state = 142;
            this._errHandler.sync(this);
            _la = this._input.LA(1);
            while(_la===RolParser.T__21) {
                this.state = 138;
                this.match(RolParser.T__21);
                this.state = 139;
                this.parameter();
                this.state = 144;
                this._errHandler.sync(this);
                _la = this._input.LA(1);
            }
            this.state = 145;
            this.match(RolParser.RCBRACKET);
            break;

        case 2:
            this.enterOuterAlt(localctx, 2);
            this.state = 147;
            this.match(RolParser.LCBRACKET);
            this.state = 148;
            this.match(RolParser.RCBRACKET);
            break;

        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function ParameterContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_parameter;
    return this;
}

ParameterContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ParameterContext.prototype.constructor = ParameterContext;


 
ParameterContext.prototype.copyFrom = function(ctx) {
    antlr4.ParserRuleContext.prototype.copyFrom.call(this, ctx);
};


function IdentParamValueContext(parser, ctx) {
	ParameterContext.call(this, parser);
    ParameterContext.prototype.copyFrom.call(this, ctx);
    return this;
}

IdentParamValueContext.prototype = Object.create(ParameterContext.prototype);
IdentParamValueContext.prototype.constructor = IdentParamValueContext;

RolParser.IdentParamValueContext = IdentParamValueContext;

IdentParamValueContext.prototype.IDENT = function() {
    return this.getToken(RolParser.IDENT, 0);
};

IdentParamValueContext.prototype.value = function() {
    return this.getTypedRuleContext(ValueContext,0);
};
IdentParamValueContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterIdentParamValue(this);
	}
};

IdentParamValueContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitIdentParamValue(this);
	}
};

IdentParamValueContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitIdentParamValue(this);
    } else {
        return visitor.visitChildren(this);
    }
};


function StringParamValueContext(parser, ctx) {
	ParameterContext.call(this, parser);
    ParameterContext.prototype.copyFrom.call(this, ctx);
    return this;
}

StringParamValueContext.prototype = Object.create(ParameterContext.prototype);
StringParamValueContext.prototype.constructor = StringParamValueContext;

RolParser.StringParamValueContext = StringParamValueContext;

StringParamValueContext.prototype.STRING = function() {
    return this.getToken(RolParser.STRING, 0);
};

StringParamValueContext.prototype.value = function() {
    return this.getTypedRuleContext(ValueContext,0);
};
StringParamValueContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterStringParamValue(this);
	}
};

StringParamValueContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitStringParamValue(this);
	}
};

StringParamValueContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitStringParamValue(this);
    } else {
        return visitor.visitChildren(this);
    }
};



RolParser.ParameterContext = ParameterContext;

RolParser.prototype.parameter = function() {

    var localctx = new ParameterContext(this, this._ctx, this.state);
    this.enterRule(localctx, 34, RolParser.RULE_parameter);
    try {
        this.state = 157;
        switch(this._input.LA(1)) {
        case RolParser.STRING:
            localctx = new StringParamValueContext(this, localctx);
            this.enterOuterAlt(localctx, 1);
            this.state = 151;
            this.match(RolParser.STRING);
            this.state = 152;
            this.match(RolParser.T__22);
            this.state = 153;
            this.value();
            break;
        case RolParser.IDENT:
            localctx = new IdentParamValueContext(this, localctx);
            this.enterOuterAlt(localctx, 2);
            this.state = 154;
            this.match(RolParser.IDENT);
            this.state = 155;
            this.match(RolParser.T__22);
            this.state = 156;
            this.value();
            break;
        default:
            throw new antlr4.error.NoViableAltException(this);
        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function ValuearrayContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_valuearray;
    return this;
}

ValuearrayContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ValuearrayContext.prototype.constructor = ValuearrayContext;

ValuearrayContext.prototype.LSBRACKET = function() {
    return this.getToken(RolParser.LSBRACKET, 0);
};

ValuearrayContext.prototype.value = function(i) {
    if(i===undefined) {
        i = null;
    }
    if(i===null) {
        return this.getTypedRuleContexts(ValueContext);
    } else {
        return this.getTypedRuleContext(ValueContext,i);
    }
};

ValuearrayContext.prototype.RSBRACKET = function() {
    return this.getToken(RolParser.RSBRACKET, 0);
};

ValuearrayContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterValuearray(this);
	}
};

ValuearrayContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitValuearray(this);
	}
};

ValuearrayContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitValuearray(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.ValuearrayContext = ValuearrayContext;

RolParser.prototype.valuearray = function() {

    var localctx = new ValuearrayContext(this, this._ctx, this.state);
    this.enterRule(localctx, 36, RolParser.RULE_valuearray);
    var _la = 0; // Token type
    try {
        this.state = 172;
        var la_ = this._interp.adaptivePredict(this._input,15,this._ctx);
        switch(la_) {
        case 1:
            this.enterOuterAlt(localctx, 1);
            this.state = 159;
            this.match(RolParser.LSBRACKET);
            this.state = 160;
            this.value();
            this.state = 165;
            this._errHandler.sync(this);
            _la = this._input.LA(1);
            while(_la===RolParser.T__21) {
                this.state = 161;
                this.match(RolParser.T__21);
                this.state = 162;
                this.value();
                this.state = 167;
                this._errHandler.sync(this);
                _la = this._input.LA(1);
            }
            this.state = 168;
            this.match(RolParser.RSBRACKET);
            break;

        case 2:
            this.enterOuterAlt(localctx, 2);
            this.state = 170;
            this.match(RolParser.LSBRACKET);
            this.state = 171;
            this.match(RolParser.RSBRACKET);
            break;

        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};

function ValueContext(parser, parent, invokingState) {
	if(parent===undefined) {
	    parent = null;
	}
	if(invokingState===undefined || invokingState===null) {
		invokingState = -1;
	}
	antlr4.ParserRuleContext.call(this, parent, invokingState);
    this.parser = parser;
    this.ruleIndex = RolParser.RULE_value;
    return this;
}

ValueContext.prototype = Object.create(antlr4.ParserRuleContext.prototype);
ValueContext.prototype.constructor = ValueContext;

ValueContext.prototype.STRING = function() {
    return this.getToken(RolParser.STRING, 0);
};

ValueContext.prototype.IDENT = function() {
    return this.getToken(RolParser.IDENT, 0);
};

ValueContext.prototype.NUMBER = function() {
    return this.getToken(RolParser.NUMBER, 0);
};

ValueContext.prototype.parameters = function() {
    return this.getTypedRuleContext(ParametersContext,0);
};

ValueContext.prototype.valuearray = function() {
    return this.getTypedRuleContext(ValuearrayContext,0);
};

ValueContext.prototype.enterRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.enterValue(this);
	}
};

ValueContext.prototype.exitRule = function(listener) {
    if(listener instanceof RolListener ) {
        listener.exitValue(this);
	}
};

ValueContext.prototype.accept = function(visitor) {
    if ( visitor instanceof RolVisitor ) {
        return visitor.visitValue(this);
    } else {
        return visitor.visitChildren(this);
    }
};




RolParser.ValueContext = ValueContext;

RolParser.prototype.value = function() {

    var localctx = new ValueContext(this, this._ctx, this.state);
    this.enterRule(localctx, 38, RolParser.RULE_value);
    try {
        this.state = 181;
        switch(this._input.LA(1)) {
        case RolParser.STRING:
            this.enterOuterAlt(localctx, 1);
            this.state = 174;
            this.match(RolParser.STRING);
            break;
        case RolParser.IDENT:
            this.enterOuterAlt(localctx, 2);
            this.state = 175;
            this.match(RolParser.IDENT);
            break;
        case RolParser.NUMBER:
            this.enterOuterAlt(localctx, 3);
            this.state = 176;
            this.match(RolParser.NUMBER);
            break;
        case RolParser.LCBRACKET:
            this.enterOuterAlt(localctx, 4);
            this.state = 177;
            this.parameters();
            break;
        case RolParser.LSBRACKET:
            this.enterOuterAlt(localctx, 5);
            this.state = 178;
            this.valuearray();
            break;
        case RolParser.T__23:
            this.enterOuterAlt(localctx, 6);
            this.state = 179;
            this.match(RolParser.T__23);
            break;
        case RolParser.T__24:
            this.enterOuterAlt(localctx, 7);
            this.state = 180;
            this.match(RolParser.T__24);
            break;
        default:
            throw new antlr4.error.NoViableAltException(this);
        }
    } catch (re) {
    	if(re instanceof antlr4.error.RecognitionException) {
	        localctx.exception = re;
	        this._errHandler.reportError(this, re);
	        this._errHandler.recover(this, re);
	    } else {
	    	throw re;
	    }
    } finally {
        this.exitRule();
    }
    return localctx;
};


exports.RolParser = RolParser;
