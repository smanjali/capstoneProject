package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement funcDefStmt = parseFunctionDefintionStatement();
        if(funcDefStmt != null) {
            return funcDefStmt;
        }
        return parseStatement();
    }


    private Statement parseStatement() {
        Statement forStmt = parseForStatement();
        if (forStmt != null) {
            return forStmt;
        }

        Statement ifStmt = parseIfStatement();
        if (ifStmt != null) {
            return ifStmt;
        }

        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }

        Statement varStatement = parseVarStatement();
        if (varStatement != null) {
            return varStatement;
        }

        Statement assignmentOrFunctionCall = parseAssignmentStatementOrFunctionCall();
        if (assignmentOrFunctionCall != null) {
            return assignmentOrFunctionCall;
        }

        Statement returnStatement = parseReturnStatement();
        if (returnStatement != null) {
            return returnStatement;
        }

        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseForStatement() {
        if (tokens.match(FOR)) {
            ForStatement forStatement = new ForStatement();
            forStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, forStatement);
            forStatement.setVariableName(tokens.consumeToken().getStringValue());
            require(IN, forStatement);
            forStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, forStatement);

            require(LEFT_BRACE, forStatement);
            LinkedList<Statement> body = new LinkedList<>();
            do {
               body.add(parseStatement());
               if (!tokens.hasMoreTokens()) {
                   forStatement.addError(ErrorType.UNTERMINATED_ARG_LIST);
                   return forStatement;
               }
            }
            while (!tokens.match(RIGHT_BRACE));
            forStatement.setEnd(require(RIGHT_BRACE, forStatement));
            forStatement.setBody(body);

            return forStatement;
        }
        else {
            return null;
        }
    }

    private Statement parseIfStatement() {

        if (tokens.match(IF)) {
            IfStatement ifStmt = new IfStatement();
            ifStmt.setStart(tokens.consumeToken());

            require(LEFT_PAREN, ifStmt);
            ifStmt.setExpression(parseExpression());
            require(RIGHT_PAREN, ifStmt);

            LinkedList<Statement> truths = new LinkedList<>();

            require(LEFT_BRACE, ifStmt);

            do {
                truths.add(parseStatement());
                if (!tokens.hasMoreTokens()) {
                    ifStmt.addError(ErrorType.UNTERMINATED_ARG_LIST);
                    return ifStmt;
                }
            } while (!tokens.match(RIGHT_BRACE));

            ifStmt.setTrueStatements(truths);
            require(RIGHT_BRACE, ifStmt);

            if (tokens.matchAndConsume(ELSE)) {
                if (tokens.matchAndConsume(LEFT_BRACE)) {
                    LinkedList<Statement> elses = new LinkedList<>();
                    do {
                        if (!tokens.hasMoreTokens()) {
                            ifStmt.addError(ErrorType.UNTERMINATED_ARG_LIST);
                            return ifStmt;
                        }
                        elses.add(parseStatement());
                    } while (!tokens.match(RIGHT_BRACE));
                    ifStmt.setElseStatements(elses);
                    require(RIGHT_BRACE, ifStmt);
                } else {
                    parseIfStatement();
                }
            }
            return ifStmt;
        } else {
            return null;
        }
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    private Statement parseVarStatement() {
        if (tokens.match(VAR)) {
            VariableStatement varStatement = new VariableStatement();
            varStatement.setStart(tokens.consumeToken());

            varStatement.setVariableName(tokens.consumeToken().getStringValue());

            if (tokens.matchAndConsume(COLON)) {
                TypeLiteral type = parseTypeExpression();
                varStatement.setExplicitType(type.getType());
            }

            require(EQUAL, varStatement);
            varStatement.setExpression(parseExpression());

            return varStatement;
        } else {
            return null;
        }
    }

    private Statement parseAssignmentStatementOrFunctionCall() {
        if (tokens.match(IDENTIFIER)) {
            Token id = tokens.consumeToken();
            if (tokens.match(LEFT_PAREN)) {
                FunctionCallExpression fe = parseFunctionCallExpression(id);
                FunctionCallStatement function = new FunctionCallStatement(fe);
                return function;
            } else {
                AssignmentStatement as = new AssignmentStatement();
                require(EQUAL, as);
                as.setVariableName(id.getStringValue());
                as.setExpression(parseExpression());
                return as;
            }
        } else {
            return null;
        }
    }

    private Statement parseFunctionDefintionStatement() {
        if (tokens.match(FUNCTION)) {
            FunctionDefinitionStatement functionDefinitionStatement = new FunctionDefinitionStatement();
            functionDefinitionStatement.setStart(tokens.consumeToken());
            Token name = require(IDENTIFIER, functionDefinitionStatement);
            functionDefinitionStatement.setName(name.getStringValue());
            require(LEFT_PAREN, functionDefinitionStatement);
            if(!tokens.match(RIGHT_PAREN)) {
                do {
                   Token parameterName = require(IDENTIFIER, functionDefinitionStatement);
                   TypeLiteral parameterType = null;
                   if(tokens.matchAndConsume(COLON)) {
                       parameterType = parseTypeExpression();
                   }
                   functionDefinitionStatement.addParameter(parameterName.getStringValue(), parameterType);
                } while (tokens.matchAndConsume(COMMA));
            }
            require(RIGHT_PAREN, functionDefinitionStatement);
            TypeLiteral functionType = null;
            if(tokens.matchAndConsume(COLON)) {
                functionType = parseTypeExpression();
            }
            functionDefinitionStatement.setType(functionType);
            require(LEFT_BRACE, functionDefinitionStatement);
            LinkedList<Statement> body = new LinkedList<>();
            while (!tokens.match(RIGHT_BRACE)) {
                body.add(parseStatement());
            }
            functionDefinitionStatement.setBody(body);
            require(RIGHT_BRACE, functionDefinitionStatement);

            return functionDefinitionStatement;
        }
        else {
            return null;
        }
    }

    private Statement parseReturnStatement() {
        if (tokens.match(RETURN)) {
            ReturnStatement returnStatement = new ReturnStatement();
            returnStatement.setStart(tokens.consumeToken());
            if (!tokens.match(RIGHT_BRACE)) {
                returnStatement.setExpression(parseExpression());
            }
            return returnStatement;
        } else {
            return null;
        }
    }



    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseComparisonExpression();
        while (tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(rightHandSide.getEnd());
            expression = equalityExpression;
        }
        return expression;
    }

    private Expression parseComparisonExpression() {
        Expression expression = parseAdditiveExpression();
        while (tokens.match(LESS, GREATER, LESS_EQUAL, GREATER_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rightHandSide);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(rightHandSide.getEnd());
            expression = comparisonExpression;
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(STAR, SLASH)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(STRING)) {
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return stringExpression;
        } else if (tokens.match(LEFT_PAREN)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseExpression();
            ParenthesizedExpression parenthesizedExpression = new ParenthesizedExpression(rhs);
            parenthesizedExpression.setStart(token);
            Token endToken = require(RIGHT_PAREN, parenthesizedExpression);
            parenthesizedExpression.setEnd(endToken);
            return parenthesizedExpression;
        } else if (tokens.match(TRUE)) {
            Token boolToken = tokens.consumeToken();
            BooleanLiteralExpression booleanLiteralExpression = new BooleanLiteralExpression(true);
            booleanLiteralExpression.setToken(boolToken);
            return booleanLiteralExpression;
        } else if (tokens.match(FALSE)) {
            Token boolToken = tokens.consumeToken();
            BooleanLiteralExpression booleanLiteralExpression = new BooleanLiteralExpression(false);
            booleanLiteralExpression.setToken(boolToken);
            return booleanLiteralExpression;
        } else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullLiteralExpression = new NullLiteralExpression();
            nullLiteralExpression.setToken(nullToken);
            return nullLiteralExpression;
        } else if (tokens.match(IDENTIFIER)) {
            Token identifierToken = tokens.consumeToken();
            IdentifierExpression identifierExpression = new IdentifierExpression(identifierToken.getStringValue());
            identifierExpression.setToken(identifierToken);
            if(tokens.match(LEFT_PAREN)) {
                return parseFunctionCallExpression(identifierToken);
            }
            return identifierExpression;
        } else if (tokens.match(LEFT_BRACKET)) {
            return parseListLiteralExpression();
        } else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    private Expression parseListLiteralExpression() {
        Token start = tokens.consumeToken();
        List<Expression> values = new ArrayList<>();
        if (!tokens.match(RIGHT_BRACKET)) {
            Expression expression = parseExpression();
            values.add(expression);
            do {
                require(COMMA, expression);
                expression = parseExpression();
                values.add(expression);
            } while (!tokens.match(RIGHT_BRACKET, EOF));
        }
        ListLiteralExpression listLiteralExpression = new ListLiteralExpression(values);
        Token end = require(RIGHT_BRACKET, listLiteralExpression);
        if (end.getType() == EOF) {
            listLiteralExpression.addError(ErrorType.UNTERMINATED_LIST);
        }
        listLiteralExpression.setStart(start);
        listLiteralExpression.setEnd(end);

        return listLiteralExpression;
    }

    private FunctionCallExpression parseFunctionCallExpression(Token name) {
        //Token start = tokens.consumeToken();
        List<Expression> argumentList = parseArgumentList();
        FunctionCallExpression functionCallExpression = new FunctionCallExpression(name.getStringValue(), argumentList);
        Token end = require(RIGHT_PAREN,functionCallExpression);
        if (end.getType() == EOF) {
            functionCallExpression.addError(ErrorType.UNTERMINATED_ARG_LIST);
        }
        functionCallExpression.setStart(name);
        functionCallExpression.setEnd(end);
        return functionCallExpression;
    }

    private List<Expression> parseArgumentList() {
        List<Expression> arguments = new ArrayList<>();
        if (tokens.matchAndConsume(LEFT_PAREN)) {
            if (!tokens.match(RIGHT_PAREN)) {
                Expression expression = parseExpression();
                arguments.add(expression);
                while(!tokens.match(RIGHT_PAREN, EOF)) {
                    require(COMMA, expression);
                    expression = parseExpression();
                    arguments.add(expression);
                }
            }
        }
        return arguments;
    }

    private TypeLiteral parseTypeExpression() {
        TypeLiteral typeLiteral = new TypeLiteral();
        if (tokens.match(IDENTIFIER)) {
            Token identifierToken = tokens.consumeToken();
            typeLiteral.setStart(identifierToken);
            String typeName = identifierToken.getStringValue();
            if (typeName.equals("int")) {
                typeLiteral.setType(CatscriptType.INT);
            } else if(typeName.equals("string")) {
                typeLiteral.setType(CatscriptType.STRING);
            } else if(typeName.equals("bool")) {
                typeLiteral.setType(CatscriptType.BOOLEAN);
            } else if(typeName.equals("object")) {
                typeLiteral.setType(CatscriptType.OBJECT);
            } else if(typeName.equals("list")) {
                if (tokens.matchAndConsume(LESS)) {
                    var innerType = parseTypeExpression();
                    typeLiteral.setType(CatscriptType.getListType(innerType.getType()));
                    typeLiteral.addChild(innerType);
                    require(GREATER, typeLiteral);
                } else {
                    typeLiteral.setType(CatscriptType.OBJECT);
                }
            }
            return typeLiteral;

        } else {
            return null;
        }
    }


    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
