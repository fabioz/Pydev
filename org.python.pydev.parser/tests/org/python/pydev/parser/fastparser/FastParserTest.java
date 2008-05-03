package org.python.pydev.parser.fastparser;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * Note: tests don't have the correct syntax on purpose!
 *
 * @author Fabio
 */
public class FastParserTest extends TestCase {

    public static void main(String[] args) {
        try {
            FastParserTest test = new FastParserTest();
            test.setUp();
            test.testGettingClass3();
            test.tearDown();
            junit.textui.TestRunner.run(FastParserTest.class);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void testGettingClassOrFunc() throws Exception {
        Document doc = new Document();
        doc.set("def bar(a):\n" +
                "\n" +
                "class \\\n" +
                "  Bar\n" +
                "    def mm\n" +
                "def\n" +//no space after
                "class\n" + //no space after
                "class \n" + 
                "");
        
        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(4, all.size());
        check(all, 0, 1,  1, 1,  5);
        check(all, 1, 3,  1, 3,  7);
        check(all, 2, 5,  5, 5,  9);
        check(all, 3, 8,  1, 8,  7);
    }
    
    public void testGettingClass() throws Exception {
        Document doc = new Document();
        doc.set("class Foo:\n" +
        		"\n" +
        		"class Bar(object):\n" +
        		"\n" +
        		"    class My\n" +
        		"'''class Dont\n" +
        		"class Dont2\n" +
        		"\n" +
        		"'''\n" +
        		"class My2:\n" +
        		"" +
        		"");
        
        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(4, all.size());
        check(all, 0, 1,  1, 1,  7);
        check(all, 1, 3,  1, 3,  7);
        check(all, 2, 5,  5, 5,  11);
        check(all, 3, 10, 1, 10, 7);
        
        stmtType found = FastParser.firstClassOrFunction(doc, 1, true);
        checkNode(3,  1, 3,  7, (ClassDef)found);
        
        found = FastParser.firstClassOrFunction(doc, 0, true);
        checkNode(1,  1, 1,  7, (ClassDef)found);
        
        found = FastParser.firstClassOrFunction(doc, 5, true);
        checkNode(10, 1, 10, 7, (ClassDef)found);
        
        found = FastParser.firstClassOrFunction(doc, 5, false);
        checkNode(5,  5, 5,  11, (ClassDef)found);
        
        found = FastParser.firstClassOrFunction(doc, -1, false);
        assertNull(found);
        
        found = FastParser.firstClassOrFunction(doc, 15, true);
        assertNull(found);
        
        found = FastParser.firstClassOrFunction(doc, 15, false);
        checkNode(10, 1, 10, 7, (ClassDef)found);
        
    }
    
    public void testGettingClass2() throws Exception {
        Document doc = new Document();
        doc.set("def GetClassesAndData():\n" +
                "    curr_widget_class = 10\n" +
                "\n" +
                "" +
        "");
        
        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(1, all.size());
    }
    
    public void testGettingClass3() throws Exception {
        Document doc = new Document();
        doc.set("class A(object):\n" +
                "    curr_widget_class = 10\n" +
                "\n" +
                "" +
        "");
        
        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(1, all.size());
    }
    
    public void testGettingClass4() throws Exception {
        Document doc = new Document();
        doc.set("\nclass A(object):\n" +
                "    curr_widget_class = 10\n" +
                "\n" +
                "" +
        "");
        
        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(1, all.size());
    }
    
    public void testGettingMethod() throws Exception {
        Document doc = new Document();
        doc.set("def a():\n" +
                "    curr_widget_def = 10\n" +
                "\n" +
                "" +
        "");
        
        List<stmtType> all = FastParser.parseClassesAndFunctions(doc);
        assertEquals(1, all.size());
    }

    private void check(List<stmtType> all, int position, int classBeginLine, int classBeginCol, int nameBeginLine, int nameBeginCol) {
        SimpleNode node = all.get(position);
        checkNode(classBeginLine, classBeginCol, nameBeginLine, nameBeginCol, node);
    }

    private void checkNode(int classBeginLine, int classBeginCol, int nameBeginLine, int nameBeginCol, SimpleNode node) {
        assertEquals(classBeginLine, node.beginLine);
        assertEquals(classBeginCol, node.beginColumn);
        
        SimpleNode name = NodeUtils.getNameTokFromNode(node);
        assertEquals(nameBeginLine, name.beginLine);
        assertEquals(nameBeginCol, name.beginColumn);
    }
}
