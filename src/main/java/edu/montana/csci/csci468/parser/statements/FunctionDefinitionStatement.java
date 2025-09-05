package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.eval.ReturnException;
import edu.montana.csci.csci468.parser.*;
import edu.montana.csci.csci468.parser.expressions.TypeLiteral;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class FunctionDefinitionStatement extends Statement {
    private String name;
    private CatscriptType type;
    private List<CatscriptType> argumentTypes = new ArrayList<>();
    private List<String> argumentNames = new ArrayList<>();
    private LinkedList<Statement> body;

    public void setName(String name) {
        this.name = name;
    }

    public CatscriptType getType() {
        return type;
    }

    public void setType(TypeLiteral typeLiteral) {
        if (typeLiteral == null) {
            type = CatscriptType.VOID;
        } else {
            addChild(typeLiteral);
            type = typeLiteral.getType();
        }
    }

    public String getName() {
        return name;
    }

    public void addParameter(String name, TypeLiteral typeLiteral) {
        argumentNames.add(name);
        if (typeLiteral == null) {
            argumentTypes.add(CatscriptType.OBJECT);
        } else {
            addChild(typeLiteral);
            argumentTypes.add(typeLiteral.getType());
        }
    }

    public String getParameterName(int i) {
        return argumentNames.get(i);
    }

    public CatscriptType getParameterType(int i) {
        return argumentTypes.get(i);
    }

    public int getParameterCount() {
        return argumentNames.size();
    }

    public void setBody(List<Statement> statements) {
        this.body = new LinkedList<>();
        for (Statement statement : statements) {
            this.body.add(addChild(statement));
        }
    }

    public List<Statement> getBody() {
        return body;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        symbolTable.pushScope();
        for (int i = 0; i < getParameterCount(); i++) {
            if (symbolTable.hasSymbol(getParameterName(i))) {
                addError(ErrorType.DUPLICATE_NAME);
            } else {
                symbolTable.registerSymbol(getParameterName(i), getParameterType(i));
            }
        }
        for (Statement statement : body) {
            statement.validate(symbolTable);
        }
        symbolTable.popScope();
        if (!type.equals(CatscriptType.VOID)) {
            if (!validateReturnCoverage(body)) {
                addError(ErrorType.MISSING_RETURN_STATEMENT);
            }
        }
    }

    private boolean validateReturnCoverage(List<Statement> statements) {
        boolean validity = false;
        for (Statement statement : statements) {
            if (statement instanceof ReturnStatement) {
                validity = true;
            }

            if(statement instanceof IfStatement) {
                List<ParseElement> children = statement.getChildren();

                if(((IfStatement) statement).getElseStatements().isEmpty()) { //if there is no else statement
                    if (children.size() > 2) { // if children < 2 you are missing a return statement
                        for(int i = children.size() - 1; i > 0; i--) {
                            if (children.get(i) instanceof ReturnStatement) {
                                validity = true;
                            } else {
                                return false;
                            }
                        }
                    }
                } else {
                    for(int i = children.size() - 1; i > 0; i--) {
                        if (children.get(i) instanceof ReturnStatement) {
                            validity = true;
                        } else {
                            return false;
                        }
                    }
                }
            }

        }
        return validity;
    }

    public Object invoke(CatscriptRuntime runtime, List<Object> args) {
        runtime.pushScope();
        int parameterCount = getParameterCount();
        for (int i = 0; i < parameterCount; i++) {
            runtime.setValue(getParameterName(i), args.get(i));
        }
        Object returnVal = null;
        try {
            for (Statement statement : body) {
                statement.execute(runtime);
            }
        } catch (ReturnException re) {
            returnVal = re.getValue();
        } finally {
            runtime.popScope();
        }
        return returnVal;
    }

    public String getDescriptor() {
        StringBuilder sb = new StringBuilder("(");
        for (CatscriptType argumentType : argumentTypes) {
            if (argumentType.equals(CatscriptType.BOOLEAN) || argumentType.equals(CatscriptType.INT)) {
                sb.append("I");
            } else {
                sb.append("L").append(internalNameFor(getType().getJavaType())).append(";");
            }
        }
        sb.append(")");
        if (type.equals(CatscriptType.VOID)) {
            sb.append("V");
        } else if (type.equals(CatscriptType.BOOLEAN) || type.equals(CatscriptType.INT)) {
            sb.append("I");
        } else {
            sb.append("L").append(internalNameFor(getType().getJavaType())).append(";");
        }
        return sb.toString();
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        for (Statement statement : body) {
            statement.execute(runtime);
        }
        //super.execute(runtime);
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        code.pushMethod(Opcodes.ACC_PUBLIC, name, getDescriptor());
        code.addVarInstruction(Opcodes.ALOAD, 0);
        for (String argumentName : argumentNames) {
            code.createLocalStorageSlotFor(argumentName);
        }
        for (Statement statement : body) {
            statement.compile(code);
        }
        if(type.equals(CatscriptType.VOID)) {
            code.addInstruction(Opcodes.RETURN);
        }
        code.popMethod();
    }
}
