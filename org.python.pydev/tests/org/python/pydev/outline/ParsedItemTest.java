package org.python.pydev.outline;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.parser.visitors.scope.OutlineCreatorVisitor;

public class ParsedItemTest extends PyParserTestBase {
    
    public static void main(String[] args) {
        try {
            ParsedItemTest analyzer2 = new ParsedItemTest();
            analyzer2.setUp();
            analyzer2.testParsedItemCreation3();
            analyzer2.tearDown();
            System.out.println("finished");
            
            junit.textui.TestRunner.run(ParsedItemTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testParsedItemCreation() throws Exception {
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "" +
                "class Foo(object):\n" +
                "    def m1(self):\n" +
                "        pass\n" +
                "        #--- comm method\n" +
                "    #--- comm class\n" +
                "#--- comm module\n" +
                "";
        
        SimpleNode node = parseLegalDocStr(str);
        
        OutlineCreatorVisitor visitor = OutlineCreatorVisitor.create(node);
        ParsedItem item = new ParsedItem(visitor.getAll().toArray(new ASTEntryWithChildren[0]), null);
        
        //module level: Foo and 1 comment
        assertEquals(2, item.getAstChildrenEntries().length);
        assertNull(item.getAstChildrenEntries()[1].children); //comment has no children
        
        //class level: m1 and 1 comment
        ASTEntryWithChildren classEntry = item.getAstChildrenEntries()[0];
        assertEquals(2, classEntry.children.size()); 
        assertNull(classEntry.children.get(1).children); //comment has no children
        
        // method level: 1 comment
        ASTEntryWithChildren functionEntry = classEntry.children.get(0);
        assertEquals(1, functionEntry.children.size()); 
        
    }
    
    public void testParsedItemCreation3() throws Exception {
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "" +
        "class Test:\n" +
        "    def __init__(self):\n" +
        "        self.foo, self.bar = 1, 2\n" +
        "";
        
        SimpleNode node = parseLegalDocStr(str);
        
        OutlineCreatorVisitor visitor = OutlineCreatorVisitor.create(node);
        ParsedItem item = new ParsedItem(visitor.getAll().toArray(new ASTEntryWithChildren[0]), null);
        
        //module level: Test
        assertEquals(1, item.getAstChildrenEntries().length);
        
        //class level: __init__
        ASTEntryWithChildren classEntry = item.getAstChildrenEntries()[0];
        assertEquals(1, classEntry.children.size()); 
        
        // method level: 2 attributes
        ASTEntryWithChildren functionEntry = classEntry.children.get(0);
        assertEquals(2, functionEntry.children.size()); 
        
    }
    
    public void testParsedItemCreation2() throws Exception {
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "" +
        "class Foo(object):\n" +
        "    pass\n" +
        "if __name__ == '__main__':\n" +
        "    var = 10\n" +
        "\n" +
        "";
        
        SimpleNode node = parseLegalDocStr(str);
        
        OutlineCreatorVisitor visitor = OutlineCreatorVisitor.create(node);
        ParsedItem item = new ParsedItem(visitor.getAll().toArray(new ASTEntryWithChildren[0]), null);
        
        //module level: only Foo
        assertEquals(1, item.getAstChildrenEntries().length);
        assertNull(item.getAstChildrenEntries()[0].children); //no children
    }
    
    /**
     * Check if the creation of a new structure will maintain the old items intact (as much as possible).
     * 
     * @throws Exception
     */
    public void testNewChildrenStructure() throws Exception {
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5);
        String str = "" +
        "class Foo(object):\n" +
        "    def m1(self):\n" +
        "        pass\n" +
        "\n" +
        "";
        
        String str2 = "" +
        "class Foo(object):\n" +
        "    def m1(self):\n" +
        "        pass\n" +
        "    def m2(self):\n" + //one more member
        "        pass\n" +
        "\n" +
        "";
        
        SimpleNode node = parseLegalDocStr(str);
        SimpleNode node2 = parseLegalDocStr(str2);
        
        OutlineCreatorVisitor visitor = OutlineCreatorVisitor.create(node);
        ParsedItem item = new ParsedItem(visitor.getAll().toArray(new ASTEntryWithChildren[0]), null);
        
        OutlineCreatorVisitor visitor2 = OutlineCreatorVisitor.create(node2);
        ParsedItem item2 = new ParsedItem(visitor2.getAll().toArray(new ASTEntryWithChildren[0]), null);

        item.updateTo(item2);
    }
}
