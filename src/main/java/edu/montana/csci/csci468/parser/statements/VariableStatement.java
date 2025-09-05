package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Opcodes;

public class VariableStatement extends Statement {
    private Expression expression;
    private String variableName;
    private CatscriptType explicitType;
    private CatscriptType type;

    public Expression getExpression() {
        return expression;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }

    public void setExplicitType(CatscriptType type) {
        this.explicitType = type;
    }

    public CatscriptType getExplicitType() {
        return explicitType;
    }

    public boolean isGlobal() {
        return getParent() instanceof CatScriptProgram;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
        if (symbolTable.hasSymbol(variableName)) {
            addError(ErrorType.DUPLICATE_NAME);
        } else {
            if(getExplicitType() != null) {
                if(!getExplicitType().isAssignableFrom(getExpression().getType())) {
                    addError(ErrorType.INCOMPATIBLE_TYPES);
                }
            } else {
                type = getExpression().getType();
            }
            symbolTable.registerSymbol(variableName, type);
        }
    }

    public CatscriptType getType() {
        return type;
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        Object result = expression.evaluate(runtime);
        runtime.setValue(variableName, result);
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        if (isGlobal()) {
            String descriptor = "I";
            if(getType() == CatscriptType.STRING) {
                descriptor = "Ljava/lang/String;";
            } else if (getType() == CatscriptType.OBJECT || getType() == CatscriptType.NULL) {
                descriptor = "Ljava/lang/Object;";
            }

            code.addField(variableName, descriptor);

            code.addVarInstruction(Opcodes.ALOAD, 0);
            expression.compile(code);
            code.addFieldInstruction(Opcodes.PUTFIELD, variableName, descriptor, code.getProgramInternalName());
        } else {
            Integer slot = code.createLocalStorageSlotFor(variableName);
            expression.compile(code);
            if(getType() != CatscriptType.INT) {
                code.addVarInstruction(Opcodes.ASTORE, slot);
            } else {
                code.addVarInstruction(Opcodes.ISTORE, slot);
            }
        }
    }
}
