package org.python.pydev.editor.codecompletion;

import junit.framework.TestCase;

import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.visitors.NodeUtils;

public class NodeUtilsTest extends TestCase {

    public void testHandledParamType0() throws Exception {
        PyAstFactory factory = new PyAstFactory(new AdapterPrefs("\n", null));
        FunctionDef functionDef = factory.createFunctionDef("foo");
        factory.setBody(functionDef, factory.createString(":type a: Bar"));
        assertEquals("Bar", NodeUtils.getTypeForParameterFromDocstring("a", functionDef));
    }

    public void testHandledParamType0a() throws Exception {
        PyAstFactory factory = new PyAstFactory(new AdapterPrefs("\n", null));
        FunctionDef functionDef = factory.createFunctionDef("foo");
        factory.setBody(functionDef, factory.createString(":type a: list of Bar"));
        assertEquals("list of Bar", NodeUtils.getTypeForParameterFromDocstring("a", functionDef));
    }

    public void testHandledParamType1() throws Exception {
        PyAstFactory factory = new PyAstFactory(new AdapterPrefs("\n", null));
        FunctionDef functionDef = factory.createFunctionDef("foo");
        factory.setBody(functionDef, factory.createString(":param Bar a:"));
        assertEquals("Bar", NodeUtils.getTypeForParameterFromDocstring("a", functionDef));
    }

    public void testHandledParamType2() throws Exception {
        PyAstFactory factory = new PyAstFactory(new AdapterPrefs("\n", null));
        FunctionDef functionDef = factory.createFunctionDef("foo");
        factory.setBody(functionDef, factory.createString("@param a: Bar"));
        assertEquals("Bar", NodeUtils.getTypeForParameterFromDocstring("a", functionDef));
    }

    public void testHandledParamType3() throws Exception {
        PyAstFactory factory = new PyAstFactory(new AdapterPrefs("\n", null));
        FunctionDef functionDef = factory.createFunctionDef("foo");
        factory.setBody(functionDef, factory.createString(":param Bar a: some string"));
        assertEquals("Bar", NodeUtils.getTypeForParameterFromDocstring("a", functionDef));
    }

    public void testHandledReturnType() throws Exception {
        PyAstFactory factory = new PyAstFactory(new AdapterPrefs("\n", null));
        FunctionDef functionDef = factory.createFunctionDef("foo");
        factory.setBody(functionDef, factory.createString(":rtype Bar"));
        assertEquals("Bar", NodeUtils.getReturnTypeFromDocstring(functionDef));
    }

    public void testHandledReturnType1() throws Exception {
        PyAstFactory factory = new PyAstFactory(new AdapterPrefs("\n", null));
        FunctionDef functionDef = factory.createFunctionDef("foo");
        factory.setBody(functionDef, factory.createString("@return Foo:\n    this is the foo return"));
        assertEquals("Foo", NodeUtils.getReturnTypeFromDocstring(functionDef));
    }

    public void testHandledReturnType2() throws Exception {
        PyAstFactory factory = new PyAstFactory(new AdapterPrefs("\n", null));
        FunctionDef functionDef = factory.createFunctionDef("foo");
        factory.setBody(functionDef, factory.createString(":rtype :class:`Bar`"));
        assertEquals("Bar", NodeUtils.getReturnTypeFromDocstring(functionDef));
    }

    public void testHandledReturnType3() throws Exception {
        PyAstFactory factory = new PyAstFactory(new AdapterPrefs("\n", null));
        FunctionDef functionDef = factory.createFunctionDef("foo");
        factory.setBody(functionDef, factory.createString(":rtype :function:`IgnoreTitle GUITest`"));
        assertEquals("GUITest", NodeUtils.getReturnTypeFromDocstring(functionDef));
    }

    public void testGetUnpackedType() throws Exception {
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("list[str]"));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("list [str]"));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("list(str)"));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("list of [str]"));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("list of str"));
        assertEquals("int", NodeUtils.getUnpackedTypeFromDocstring("dict[int,str]"));
        assertEquals("int", NodeUtils.getUnpackedTypeFromDocstring("dict [int,str]"));
        assertEquals("int", NodeUtils.getUnpackedTypeFromDocstring("dict[int->str]"));
        assertEquals("int", NodeUtils.getUnpackedTypeFromDocstring("dict[int:str]"));
    }
}
