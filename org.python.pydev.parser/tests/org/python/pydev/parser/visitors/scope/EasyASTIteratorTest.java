/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import java.util.Iterator;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.parser.PyParser;

/**
 * @author Fabio
 */
public class EasyASTIteratorTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(EasyASTIteratorTest.class);
//        EasyASTIteratorTest test = new EasyASTIteratorTest();
//        try {
//            test.setUp();
//            test.testMultiline2();
//            test.tearDown();
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * @throws Exception
     * 
     */
    public void testClassesMethods() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
		"class C:\n" +
		"    def met1(self):pass\n" +
		"\n" +
		"if True:\n" +
		"    print 't'\n" +
		"\n" +
		"class D:\n" +
		"    pass\n" +
		"class E:\n" +
		"    '''t1\n" +
		"    t2\n" +
		"    '''\n" +
		"c = C()\n" +
		"";
        
        Object[] objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str), false, null));
        SimpleNode root = (SimpleNode) objects[0];
        root.accept(visitor);
        Iterator iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 2);
        check((ASTEntry) iterator.next(), "met1", 5, 2, 2);
        check((ASTEntry) iterator.next(), "D", 1, 7, 8);
        check((ASTEntry) iterator.next(), "E", 1, 9, 12);
        assertFalse(iterator.hasNext());
    }

    /**
     * @throws Exception
     * 
     */
    public void testMultiline() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
		"class C:        \n"+ 
		"    def d(self):\n"+ 
		"        c = \\\n"+ 
		"'''             \n"+  
		"a               \n"+     
		"b               \n"+      
		"c               \n"+     
		"'''             \n";      
        
        Object[] objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str), false, null));
        SimpleNode root = (SimpleNode) objects[0];
        root.accept(visitor);
        Iterator iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 8);
        check((ASTEntry) iterator.next(), "d", 5, 2, 8);
        assertFalse(iterator.hasNext());
    }

    /**
     * @throws Exception
     * 
     */
    public void testMultiline2() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
		"class C:          \n"+           
		"    def d(self):  \n"+                   
		"        c = '''   \n"+                  
		"                  \n"+   
		"c                 \n"+    
		"'''               \n"+      
		"                  \n"+     
		"class E:          \n"+
		"    '''t1         \n"+    
		"    t2            \n"+  
		"    '''           \n";         

        Object[] objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str), false, null));
        SimpleNode root = (SimpleNode) objects[0];
        root.accept(visitor);
        Iterator iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        check((ASTEntry) iterator.next(), "d", 5, 2, 6);
        check((ASTEntry) iterator.next(), "E", 1, 8, 11);
        assertFalse(iterator.hasNext());
    }

    /**
     * @throws Exception
     * 
     */
    public void testImports() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
		"import test.lib\n" +
		"from test.lib import test\n" +
		"from test.lib import *\n" +
		"";
        
        Object[] objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str), false, null));
        SimpleNode root = (SimpleNode) objects[0];
        root.accept(visitor);
        Iterator iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "import test.lib", 8, 1, 1);
        check((ASTEntry) iterator.next(), "from test.lib import test", 6, 2, 2);
        check((ASTEntry) iterator.next(), "from test.lib import *", 6, 3, 3);
        assertFalse(iterator.hasNext());
    }
    
    /**
     * @throws Exception
     * 
     */
    public void testAttributes() throws Exception {
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        String str = "" +
        		"class C:\n" +
        		"    def met1(self):\n" +
        		"        self.attr1=1\n" +
        		"        self.attr2=2\n" +
        		"\n" +
        		"    classAttr = 10\n" +
        		"pass";
        
        Object[] objects = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(str), false, null));
        SimpleNode root = (SimpleNode) objects[0];
        root.accept(visitor);
        Iterator iterator = visitor.getIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        check((ASTEntry) iterator.next(), "met1", 5, 2, 4);
        check((ASTEntry) iterator.next(), "attr1", 9, 3, 3);
        check((ASTEntry) iterator.next(), "attr2", 9, 4, 4);
        check((ASTEntry) iterator.next(), "classAttr", 5, 6, 6);
        assertFalse(iterator.hasNext());
        
        iterator = visitor.getClassesIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        assertFalse(iterator.hasNext());
        
        iterator = visitor.getClassesAndMethodsIterator();
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        check((ASTEntry) iterator.next(), "met1", 5, 2, 4);
        assertFalse(iterator.hasNext());
        
        iterator = visitor.getIterator(ClassDef.class);
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        assertFalse(iterator.hasNext());
        
        iterator = visitor.getIterator(new Class[]{ClassDef.class, FunctionDef.class});
        check((ASTEntry) iterator.next(), "C", 1, 1, 6);
        check((ASTEntry) iterator.next(), "met1", 5, 2, 4);
        assertFalse(iterator.hasNext());
    }
    
    private void check(ASTEntry entry, String name, int col, int begLine, int endLine) {
        assertEquals(name, entry.getName());
        assertEquals(col, entry.node.beginColumn);
        assertEquals(begLine, entry.node.beginLine);
        assertEquals(endLine, entry.endLine);
    }
    
    

}
