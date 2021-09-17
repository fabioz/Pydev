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
            test.testValuesFromSubscriptSlice();
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

    public void testValuesFromSubscriptSlice3() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo: Union[A, B, C]");
        Assign assign = (Assign) ast.body[0];
        Subscript subscript = (Subscript) assign.type;
        String[] expectedValues = new String[] { "A", "B", "C" };
        List<String> actualValuesList = NodeUtils.extractValuesFromSubscriptSlice(subscript.slice);
        assertArrayEquals(expectedValues, actualValuesList.toArray());
    }

    public void testValuesFromSubscriptSlice4() throws Exception {
        Module ast = (Module) parseLegalDocStr("foo: Union[A|B|C]");
        Assign assign = (Assign) ast.body[0];
        Subscript subscript = (Subscript) assign.type;
        String[] expectedValues = new String[] { "A", "B", "C" };
        List<String> actualValuesList = NodeUtils.extractValuesFromSubscriptSlice(subscript.slice);
        assertArrayEquals(expectedValues, actualValuesList.toArray());
    }
}
