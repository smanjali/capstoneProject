package edu.montana.csci.csci468.demo;

import edu.montana.csci.csci468.CatscriptTestBase;
import edu.montana.csci.csci468.parser.expressions.Expression;
import edu.montana.csci.csci468.parser.expressions.ListLiteralExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PartnerTest extends CatscriptTestBase  {

    @Test
    public void parseListLiteralExpression() {
        ListLiteralExpression expr = parseExpression("[[1, 2], 2, 3]");
        assertEquals(3, expr.getValues().size());
        ListLiteralExpression innerList = (ListLiteralExpression) expr.getValues().get(0);
        assertEquals(2, innerList.getValues().size());
    }

}
