/*
 * Created on Jul 10, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;

public class FindScopeVisitorTest extends PyParserTestBase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIt() throws Exception {
        String s = "" +
                "#file mod3.py \n" + //line = 1 (in ast)
                "class SomeA(object):\n" +
                "    def fun(self):\n" +
                "        pass\n" +
                "    \n" +
                "class C1(object):\n" +
                "  a = SomeA() #yes, these are class-defined\n" +
                "  \n" +
                "  def someFunct(self):\n" +
                "      pass\n" +
                "    \n" +
                "\n" +
                "";
        SimpleNode ast = parseLegalDocStr(s);
        FindScopeVisitor scopeVisitor = new FindScopeVisitor(8, 3);
        if (ast != null){
            try {
                ast.accept(scopeVisitor);
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
        assertTrue(scopeVisitor.scope.getClassDef() != null);

    }
}
