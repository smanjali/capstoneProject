package edu.montana.csci.csci468.partner_tests;

import edu.montana.csci.csci468.CatscriptTestBase;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.statements.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MichaelDownsCatScriptTests extends CatscriptTestBase {

    //==================================
    // example code for sequence diagram
    //===================================
    @Test
    void IfStatmentParsingWorks() {
        Statement practice = parseStatement("var x = 8 \n if(x < 12) {\n  print(x)\n   }\n");
        assertNotNull(practice);
    }

    //================
    // Michaels Tests
    //================

    @Test
    public void elseIfStatementParses() {
        IfStatement expr = parseStatement(
                "if(false){print(-1)} else if(true){print(0)} else {print(1)}", true);
        assertNotNull(expr);
        assertEquals(1, expr.getTrueStatements().size());
        assertEquals(1, expr.getTrueStatements().size());
        assertEquals(1, expr.getTrueStatements().size());
    }

    @Test
    public void nestedForStatementWorksProperly() {
        assertEquals("1\n2\n3\n1\n2\n3\n1\n2\n3\n",
                executeProgram("for(x in [1, 2, 3]){ for(y in [1, 2, 3]){ print(y) } }"));
    }

    @Test
    public void heterogeneousListLiteralTypeError() {
        assertEquals(ErrorType.INCOMPATIBLE_TYPES,
                getParseError("var x : list<int> = [1, true]"));
    }

}
