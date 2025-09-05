package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.eval.ReturnException;
import edu.montana.csci.csci468.parser.*;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Opcodes;

import javax.lang.model.type.NullType;

public class ReturnStatement extends Statement {
    private Expression expression;

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }

    public Expression getExpression() {
        return expression;
    }

    public FunctionDefinitionStatement getFunctionDefinitionStatement() {
        ParseElement parent = getParent();
        while (parent != null) {
            if (parent instanceof FunctionDefinitionStatement) {
                FunctionDefinitionStatement f = (FunctionDefinitionStatement) parent;
                return f;
            }
            parent = parent.getParent();
        }

        // use the `instanceof` operator in java
        // if there are none, return null
        return null;
    }


    @Override
    public void validate(SymbolTable symbolTable) {
        FunctionDefinitionStatement function = getFunctionDefinitionStatement();
        if (function == null) {
            addError(ErrorType.INVALID_RETURN_STATEMENT);
        } else {
            // ensure it is compatible with function.getType() or add an ErrorType.INCOMPATIBLE_TYPE error
            if (function.getType() == CatscriptType.VOID) {
                if (expression != null) {
                    addError(ErrorType.INCOMPATIBLE_TYPES);
                }
            } else {
                if (expression == null) {
                    addError(ErrorType.INCOMPATIBLE_TYPES);
                } else {
                    expression.validate(symbolTable);
                    if (!function.getType().isAssignableFrom(expression.getType())) {
                        addError(ErrorType.INCOMPATIBLE_TYPES);
                    }
                }
            }

        }
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        Object value = null;
        if (expression != null) {
            value = expression.evaluate(runtime);
        }
        throw new ReturnException(value);
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        if (expression != null) {
            expression.compile(code);

            if(getFunctionDefinitionStatement().getType().equals(CatscriptType.OBJECT)) {
                box(code, expression.getType());
            }

            if(expression.getType().equals(CatscriptType.BOOLEAN) || expression.getType().equals(CatscriptType.INT)) {
                if(getFunctionDefinitionStatement().getType().equals(CatscriptType.OBJECT)) {
                    code.addInstruction(Opcodes.ARETURN);
                } else {
                    code.addInstruction(Opcodes.IRETURN);
                }

            } else {
                code.addInstruction(Opcodes.ARETURN);
            }
        } else {
            code.addInstruction(Opcodes.RETURN);
        }
    }

}