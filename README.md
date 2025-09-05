# My Capstone Project
This is my final Capstone Project to be shared with future employers. Below is a description of the Catscript Language that I created a compiler for. Enjoy!

# Catscript

CatScript is a scripting language whose primary purpose is to build a deeper understanding in creating a compiler. Because of this, the target audience of CatScript is mainly the programmer itself. This language is inspired by the JavaScript programming language and compiles down to JVM bytecode.

## Features
Some exciting features of CatScript: the integration of covariant lists and non-explicit type declaration.
### Covariant Lists
CatScript lists hold multiple values of the same type; the type is called a component type. Once a list is defined, it cannot be edited. Because of this factor of immutability, list types can be covariant on their component type, as defined in the statement below. 
```
A list L1 is assignable to a variable of list type L2 if and only if
the component type of L1 is assignable to the component type of L2
```
This attribute of covariance allows a list to be assigned to a list with a different component type. But, when a case like this occurs, the component type of the first list must be assignable to the component type of the second list.

|Type | Assignable From | Assignable To |
|:-----|:----------------|:--------------|
|object| all types	 | -			 |
|int   | int & null	 | int & object |
|string| string & null   | string & object|
|bool | bool & null     | bool & object |
|list | list \<component type\>| list \<component type\> |
|	 | *with limits...   | *with limits... |
|void | -			 | -     |
|null | null		 | all non-void types|

### Non-Explicit Type Declarations
When declaring a variable in CatScript it is not essential to declare the type. To declare a variable, you must first use the keyword var. This attribute is defined in the CatScript grammar, seen below, for variable_statement. Once you set an identifier, you have the option to explicitly define the type; this action is represented by a colon(:) followed by a type declaration. If no type is declared, the variable is set to the default type null. Due to the declaration of null, as seen in the table above, this variable can be assigned to a value of any type. 

## Writing â€œHello World!â€
```
var x = â€œHello World!â€
print(x)
```
## CatScript Grammar

```ebnf
catscript_program = { program_statement };

program_statement = statement |
                    function_declaration;

statement = for_statement |
            if_statement |
            print_statement |
            variable_statement |
            assignment_statement |
            return_statement |
            function_call_statement;

for_statement = 'for', '(', IDENTIFIER, 'in', expression ')', 
                '{', { statement }, '}';

if_statement = 'if', '(', expression, ')', '{', 
                    { statement }, 
               '}' [ 'else', ( if_statement | '{', { statement }, '}' ) ];

print_statement = 'print', '(', expression, ')'

variable_statement = 'var', IDENTIFIER, 
     [':', type_expression, ] '=', expression;

function_call_statement = function_call;

assignment_statement = IDENTIFIER, '=', expression;

function_declaration = 'function', IDENTIFIER, '(', parameter_list, ')' + 
                       [ ':' + type_expression ], '{',  { statement },  '}';

parameter_list = [ parameter, {',' parameter } ];

parameter = IDENTIFIER [ , ':', type_expression ];

return_statement = 'return' [, expression];

expression = equality_expression;

equality_expression = comparison_expression { ("!=" | "==") comparison_expression };

comparison_expression = additive_expression { (">" | ">=" | "<" | "<=" ) additive_expression };

additive_expression = factor_expression { ("+" | "-" ) factor_expression };

factor_expression = unary_expression { ("/" | "*" ) unary_expression };

unary_expression = ( "not" | "-" ) unary_expression | primary_expression;

primary_expression = IDENTIFIER | STRING | INTEGER | "true" | "false" | "null"| 
                     list_literal | function_call | "(", expression, ")"

list_literal = '[', expression,  { ',', expression } ']'; 

function_call = IDENTIFIER, '(', argument_list , ')'

argument_list = [ expression , { ',' , expression } ]

type_expression = 'int' | 'string' | 'bool' | 'object' | 'list' [, '<' , type_expression, '>']

```
## Expressions
Expressions are the first data type that the CatScript parser runs through. 
### Additive Expressions
CatScript additive expressions can be expressed two ways: integer and string
#### Integer
Integer addition applies basic arithmetic addition which includes the concept of order of operation.
```
var x = 4 + 18                   // x = 21
var y = 9 + (7 + 42)             // y = 58
var z = (8 - 3) + 12 - (6 + 2)   // z = 9
```
#### String
In CatScript, string concatenation is represented by a plus sign(+). There is no demand to explicitly define the type, instead the parser and type checker will determine the correct type in compile time. 
```
var x = "self" + "sufficient"    // x = "selfsufficient"
var y = 12 + times               // y = "12times"
var z: string = null + "a"       // z = nulla
```
### Boolean Expressions
There are two CatScript Expressions that return a boolean value (true/false): comparison expression and equality expression. These expressions are often used in the parameter of if statements. 
#### Comparison Expression
Comparison expressions are signified by "greater than" and "less than" symbols. There are four options to choose from. They are, in order, greater than, greater than or equal, less than or equal, and less than. ```> | >= | <= | <```
#### Equality Expression
Equality expressions can ask whether two expressions are equal or not equal to each other. ```== | !=``` 
### Unary Expression
Unary expressions are prefixes to other expressions. The symbols for a unary expressions are ``` - | not ```. Syntactically, Catscript requires that the negative(-) symbol comes before an integer type and that the keyword 'not' comes before a boolean type. 
## Functions & Statements
Statements are approached after parsing all the way through expressions. This is done because expressions can be defined outside of functions and before they any function is defined. 

The parser runs all the way through each expression, sends a syntax error when no expression is found, and begins to parse statements if more tokens are present. 

To begin parsing statements, CatScript grammar provides two options, either defining a function (function_declaration) or parsing through statements (statements). This, as with expression parsing, allows statements to be called outside of a function. 
### Function Declaration
Function declarations are parsed separately from statements. In this way, statements can be embedded within each other, for example making a function call in an if statement. 

The grammar of the language allows the programmer to define the type of the function, this is represented by a colon followed by a type declaration. If no function type is defined, the type is set to void and does not require a return statement. Alternatively, a function defined with a type requires complete return coverage. This same property is present in the parameter definition, by the same grammar rules. The difference being that the default type of the parameter is object. 
```
function foo (count: int, stay, actual): string) {
	// this function is required to return a string value
	// count has an explicit type of int
	// stay & actual are set, as a default, to object

	// you can add as many statements as you want 
  	// within this function call
}
```
#### Calling a function
Function calls are parsed as both expressions and statements. It is designed that way so that functions can be called inside statements as well as on their own. A function of the same name must be defined ahead of the call to the function. 
### Statements
#### For Loops
For loops iterate through expressions given an identifier and utilizing the keyword "in". This layout can be found in the CatScript Grammar (for_statement). Within the for statement there can be multiple calls to other statements. 
```
for (verb in ["crispy", "fruity", "red"]) {
  var sentence = verb + "apple"
  print(sentence)
}
// Returns...
// crispy apple
// fruity apple
// red apple
```
#### If Statements
If statements consist of the keyword if, a parameter containing a boolean expression, and a body which can hold multiple statements. There are three cases for an if statement.
1. #### stand alone if
```
if (boolean expression) {
	// statements
}
```
2. #### if with else if
```
if (boolean expression) {
	// statements
}
else if (boolean expression) {
	// statements
} ...
// you can include multiple else if statements
// and end with an optional else statement
```
3. #### else statement
```
if (boolean expression) {
	// statements
}
else {
	// statements
} 
```
When an if statement is embedded within a function, there must still be full return coverage. This requires that either the end of the function has a return statement or that each if statement segment includes a return statement. 

## Scope and Lifetime
Catscript does not support variable shadowing meaning that defining duplicate variables within the same scope will cause a compile-time error
### Global Scope
Global scopes refer to any predefined variables. These variables are stored in fields in the bytecode. By storing them in fields they can be accessed throughout the entire program life cycle. 
```
var x: int = 7
// can be accessed in every future defined function and statement
```
### Function Scope
Function scopes include local variable definitions created inside of a function. These variables can only be accessed withing the function in which they are defined, and can not be duplicated in any embedded statements. The variable name can be reutilized, however,  in seperate function definitions because one the function is declared the variable is popped off of the scope.
```
function foo() {
	var str = "joy"
	// this string may be used in any statement defined
	// the variable str may NOT be redefined in this block of code
}
function sar() {
	var str = [1,2,3]
	// because we are in a new block of code, str can be redefined
	// and does not have to be the same type as above
}
```
### Block Scope
Block scope has to do with variables defined inside of statements. this is often found in "if statements" and "for statements". If the statement resides within a function, the variable name must not be an already defined local variable. Once you leave the statement, the variable can not be accessed and assigned to anything
```
function foo() {
	var str = "joy"
	if(str in ["happy", "joy", "fun"] {
		print(str) // str can still be accessed
		var num: int = 10
	}
	// num can no longer be accessed
	var num: int = 60 // this is a valid variable definition
}

function sar() {
var str = "joy"
	var num: int = 60

	if(str in ["happy", "joy", "fun"] {
		print(str) // str can still be accessed
		var num: int = 10 // *this is no longer valid
		// num can NOT be redefined bc. it is a local variable
	}
	
}
```
## Error Handling
The CatScript language handles errors fairly simply. When we parse through our language, we can add syntax errors directly in the parser. If the code parses through all the statements and does not return a matched grammar, a syntax error is returned. Doing this is incredibly simple and inexpensive.

This is an easy way to handle errors however it can result in cascading errors in the rest of the code.  Unfortunately, when an error is found, there is a chance that the parser will never recover from it, creating a high volume of errors that may not be necessary. This is not an issue for CatScript since our code's use case is simply to learn how to make a compiler.

A more efficient approach to error handling would be the Panic Mode approach. With this approach a syntax error is thrown, as is done in CatScript, but two additional steps are taken. The first step is catching the error in a try...catch statement. Once caught the parser advances to the next statement keyword and resumes parsing when this keyword is reached. Though this is a feasible option to handle errors, it is just not necessary for CatScript and it's basic utilization. 
