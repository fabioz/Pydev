package org.python.pydev.parser.visitors;

import static org.junit.Assert.assertArrayEquals;

import java.util.List;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Subscript;

public class NodeUtilsPython3Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            NodeUtilsPython3Test test = new NodeUtilsPython3Test();
            test.setUp();
            test.testBinOpRep();
            test.tearDown();
            junit.textui.TestRunner.run(NodeUtilsTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.LATEST_GRAMMAR_PY3_VERSION);
    }

    public void testBinOpRep() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo: A | B");
        Assign assign = (Assign) ast.body[0];
        assertEquals("A|B", NodeUtils.getFullRepresentationString(assign.type));
    }

    public void testBinOpRep2() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo: A | B | C");
        Assign assign = (Assign) ast.body[0];
        assertEquals("A|B|C", NodeUtils.getFullRepresentationString(assign.type));
    }

    public void testBinOpRep3() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a + b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a+b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep4() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a - b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a-b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep5() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a * b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a*b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep6() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a / b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a/b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep7() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a % b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a%b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep8() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a ** b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a**b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep9() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a << b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a<<b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep10() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a >> b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a>>b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep11() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a | b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a|b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep12() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a ^ b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a^b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep13() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a & b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a&b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep14() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a // b");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a//b", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep15() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a + b - c");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a+b-c", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testBinOpRep16() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo = a + b - c * d");
        Assign assign = (Assign) ast.body[0];
        assertEquals("a+b-c*d", NodeUtils.getFullRepresentationString(assign.value));
    }

    public void testValuesFromSubscriptSlice() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo: Union[A, B]");
        Assign assign = (Assign) ast.body[0];
        Subscript subscript = (Subscript) assign.type;
        String[] expectedValues = new String[] { "A", "B" };
        List<String> actualValuesList = NodeUtils.extractValuesFromSubscriptSlice(subscript.slice);
        assertArrayEquals(expectedValues, actualValuesList.toArray());
    }

    public void testValuesFromSubscriptSlice2() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo: Union[A|B]");
        Assign assign = (Assign) ast.body[0];
        Subscript subscript = (Subscript) assign.type;
        String[] expectedValues = new String[] { "A", "B" };
        List<String> actualValuesList = NodeUtils.extractValuesFromSubscriptSlice(subscript.slice);
        assertArrayEquals(expectedValues, actualValuesList.toArray());
    }
}
