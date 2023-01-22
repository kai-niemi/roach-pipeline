/*
    ANLTR4 grammar for conditional rule logic and arithmetic expressions.
    @author Kai Niemi
 */
grammar RuleExpression;

// -------------------------------------------------------------
// Parser rules that translates the token stream into structures
// -------------------------------------------------------------

evaluate
    : conditional_expr EOF
    | arithmetic_expr EOF
    | string_expr EOF
    | literal EOF
    ;

conditional_expr
    : IF condition THEN outcome ELSE outcome ';'?
    ;

condition
    : logical_expr
    ;

outcome
    : arithmetic_expr
    | string_expr
    | literal
    ;

logical_expr
    : logical_expr AND logical_expr                                         # LogicalExpressionAnd
    | logical_expr OR logical_expr                                          # LogicalExpressionOr
    | NOT logical_expr                                                      # LogicalExpressionNot
    | comparison_expr                                                       # ComparisonExpression
    | LPAREN logical_expr RPAREN                                            # LogicalExpressionInParen
    | logical_entity                                                        # LogicalEntity
    ;

logical_entity
    : booleanLiteral
    | identifier
    | function                                                              
    ;

comparison_expr
    : left=comparison_operand op=comp_operator right=comparison_operand     # ComparisonExpressionOperand
    | left=dateLiteral op=comp_operator right=dateLiteral                   # ComparisonExpressionDate
    | left=timeLiteral op=comp_operator right=timeLiteral                   # ComparisonExpressionTime
    | left=dateTimeLiteral op=comp_operator right=dateTimeLiteral           # ComparisonExpressionDateTime
    | left=stringLiteral op=comp_operator right=stringLiteral               # ComparisonExpressionString
    | left=stringLiteral op=IN right=string_list                            # ComparisonExpressionStringList
    | LPAREN comparison_expr RPAREN                                         # ComparisonExpressionParens
    ;

comparison_operand
    : arithmetic_expr
    ;

comp_operator
    : GT
    | GE
    | LT
    | LE
    | EQ
    | NE
    ;

string_list
    : Identifier                                                            # StringListVariable
    | LPAREN stringLiteral (',' stringLiteral)* RPAREN                      # StringArgumentList
    ;

literal
    : booleanLiteral
    | decimalLiteral
    | dateLiteral
    | timeLiteral
    | dateTimeLiteral
    | stringLiteral
    ;

arithmetic_expr
    : left=arithmetic_expr POW right=arithmetic_expr                        # ArithmeticPower
    | left=arithmetic_expr operator=(MULT|DIV) right=arithmetic_expr        # ArithmeticMultiplicationOrDivision
    | left=arithmetic_expr operator=(PLUS|MINUS) right=arithmetic_expr      # ArithmeticPlusOrMinus
    | left=arithmetic_expr operator=(MIN|MAX) right=arithmetic_expr         # ArithmeticMinOrMax
    | left=arithmetic_expr MOD right=arithmetic_expr                        # ArithmeticModulus
    | operator=(MINUS|PLUS) right=arithmetic_expr                           # ArithmeticUnaryMinusOrPlus
    | LPAREN inner=arithmetic_expr RPAREN                                   # ArithmeticParentheses
    | numeric_entity                                                        # ArithmeticNumericEntity
    ;

numeric_entity
    : decimalLiteral                                                        # NumericConstant
    | identifier                                                            # NumericVariable
    | function                                                              # NumericFunction
    ;

string_expr
    : left=string_expr operator=PLUS right=string_expr                      # StringPlus
    | LPAREN inner=string_expr RPAREN                                       # StringParentheses
    | string_entity                                                         # StringEntity
    ;

string_entity
    : stringLiteral                                                         # StringConstant
    | decimalLiteral                                                        # StringDecimal
    | identifier                                                            # StringVariable
    | function                                                              # StringFunction
    ;

booleanLiteral
    : BooleanLiteral
    ;

decimalLiteral
    : DecimalLiteral
    ;

stringLiteral
    : StringLiteral
    ;

dateLiteral
    : '{d ' DateLiteral '}'
    ;

timeLiteral
    : '{t ' TimeLiteral '}'
    ;

dateTimeLiteral
    : '{dt ' DateTimeLiteral '}'
    ;

identifier
    : Identifier
    ;

function
    : Identifier LPAREN functionArguments RPAREN
    ;

functionArguments
    : // none
    | functionArgument (',' functionArgument)* // some
    ;

functionArgument
    : arithmetic_expr
    | string_expr
    | literal
    ;

// ----------------------------------------
// Lexer rules that split input into tokens
// ----------------------------------------

// Arithmetics
POW : '^' ;
MULT : '*' ;
DIV : '/' ;
PLUS : '+' ;
MINUS : '-' ;
MIN : ('min'|'MIN') ;
MAX : ('max'|'MAX') ;
MOD : ('mod'|'MOD'|'%') ;

// Conditionals

IF   : ('if'|'IF') ;
THEN : ('then'|'THEN');
ELSE : ('else'|'ELSE'|'otherwise'|'OTHERWISE');
IN : ('in'|'IN');
LPAREN : '(' ;
RPAREN : ')' ;

// Logical

AND : ('and'|'AND'|'&&') ;
OR  : ('or'|'OR'|'||') ;
NOT : ('not'|'NOT'|'!') ;

// Comparative

GT : '>' ;
GE : '>=' ;
LT : '<' ;
LE : '<=' ;
EQ : '==' ;
NE : '!=' ;

// Literals

DateTimeLiteral
    : '\'' Date ' ' Time '\'' ;

DateLiteral
    : '\'' Date '\'' ;

TimeLiteral
    : '\'' Time '\'' ;

Date
    : FourDigit '-' TwoDigit '-' TwoDigit ;

Time
    : TwoDigit ':' TwoDigit ':' TwoDigit ;

DecimalLiteral
    : Digit+ ('.' Digit+)? ;

fragment SignedDecimal
    : Sign? Digit+ ('.' Digit+)? ;

BooleanLiteral : TRUE | FALSE ;

TRUE  : ('true'|'TRUE') ;
FALSE : ('false'|'FALSE') ;

StringLiteral
    :  '\'' (EscapeChars | ~['"\\])* '\'' ;

Identifier
    : Letter LetterOrDigit* ;

fragment FourDigit
    : Digit Digit Digit Digit ;

fragment TwoDigit
    : Digit Digit ;

fragment Sign
    : [+-] ;

fragment Digit
    : [0-9] ;

fragment Digits
    : '0' | [1-9] [0-9]* ;

fragment EscapeChars
    :   '\\' (["\\/bfnrt] | UniCode) ;

fragment UniCode
    : 'u' Hex Hex Hex Hex ;

fragment Hex
    : [0-9a-fA-F] ;

fragment Letter
    : [a-zA-Z$_] ;

fragment LetterOrDigit
    : [a-zA-Z0-9$_] ;

WS  : [ \r\n\t]+ -> skip ;
