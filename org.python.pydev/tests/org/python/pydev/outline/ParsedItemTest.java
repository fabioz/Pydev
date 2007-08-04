package org.python.pydev.outline;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.parser.visitors.scope.OutlineCreatorVisitor;

public class ParsedItemTest extends PyParserTestBase {

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
        assertEquals(2, item.astChildrenEntries.length);
        assertNull(item.astChildrenEntries[1].children); //comment has no children
        
        //class level: m1 and 1 comment
        ASTEntryWithChildren classEntry = item.astChildrenEntries[0];
        assertEquals(2, classEntry.children.size()); 
        assertNull(classEntry.children.get(1).children); //comment has no children
        
        // method level: 1 comment
        ASTEntryWithChildren functionEntry = classEntry.children.get(0);
        assertEquals(1, functionEntry.children.size()); 
        
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
        assertEquals(1, item.astChildrenEntries.length);
        assertNull(item.astChildrenEntries[0].children); //no children
        
    }
}
