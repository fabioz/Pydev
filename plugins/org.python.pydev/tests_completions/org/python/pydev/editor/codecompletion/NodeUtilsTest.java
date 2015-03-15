package org.python.pydev.editor.codecompletion;

import junit.framework.TestCase;

import org.python.pydev.core.UnpackInfo;
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
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("list[str]", new UnpackInfo(true, -1)));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("list [str]", new UnpackInfo(true, -1)));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("list(str)", new UnpackInfo(true, -1)));
        assertEquals("int,str", NodeUtils.getUnpackedTypeFromDocstring("dict[int,str]", new UnpackInfo(true, -1)));
        assertEquals("int,str", NodeUtils.getUnpackedTypeFromDocstring("dict [int,str]", new UnpackInfo(true, -1)));
        assertEquals("int->str", NodeUtils.getUnpackedTypeFromDocstring("dict[int->str]", new UnpackInfo(true, -1)));
        assertEquals("int:str", NodeUtils.getUnpackedTypeFromDocstring("dict[int:str]", new UnpackInfo(true, -1)));
        assertEquals("int", NodeUtils.getUnpackedTypeFromDocstring("dict[int,str]", new UnpackInfo(false, 0)));
        assertEquals("int", NodeUtils.getUnpackedTypeFromDocstring("dict [int,str]", new UnpackInfo(false, 0)));
        assertEquals("int", NodeUtils.getUnpackedTypeFromDocstring("dict[int->str]", new UnpackInfo(false, 0)));
        assertEquals("int", NodeUtils.getUnpackedTypeFromDocstring("dict[int:str]", new UnpackInfo(false, 0)));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("dict[int,str]", new UnpackInfo(false, 1)));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("dict [int,str]", new UnpackInfo(false, 1)));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("dict[int->str]", new UnpackInfo(false, 1)));
        assertEquals("str", NodeUtils.getUnpackedTypeFromDocstring("dict[int:str]", new UnpackInfo(false, 1)));
        assertEquals("foo(str,a)",
                NodeUtils
                        .getUnpackedTypeFromDocstring("list(dict[int,str], foo(str,a), bar)", new UnpackInfo(false, 1)));
        assertEquals("dict[int,str]",
                NodeUtils
                        .getUnpackedTypeFromDocstring("list(dict[int,str], foo(str,a), bar)", new UnpackInfo(false, 0)));
        assertEquals("foo(str,a)",
                NodeUtils.getUnpackedTypeFromDocstring("list(dict[int,str], foo(str,a))", new UnpackInfo(false, 1)));
        assertEquals("str",
                NodeUtils.getUnpackedTypeFromDocstring("list(dict[int,str], str)", new UnpackInfo(false, 1)));
    }
}
