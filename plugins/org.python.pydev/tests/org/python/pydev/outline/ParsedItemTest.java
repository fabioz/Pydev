/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.outline;

import java.util.List;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.parser.visitors.scope.OutlineCreatorVisitor;

public class ParsedItemTest extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            ParsedItemTest analyzer2 = new ParsedItemTest();
            analyzer2.setUp();
            analyzer2.testParsedItemCreation4();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(ParsedItemTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testParsedItemCreation() throws Exception {
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5);
        String str = "" +
                "class Foo(object):\n" +
                "    def m1(self):\n" +
                "        pass\n"
                +
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

    public void testParsedItemCreation4() throws Exception {
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5);
        String str = "" +
                "if 0:\n" +
                "    #--- foo ---\n" +
                "    pass\n" +
                "else:\n" +
                "    #--- bar ---\n"
                +
                "    pass";

        SimpleNode node = parseLegalDocStr(str);

        OutlineCreatorVisitor visitor = OutlineCreatorVisitor.create(node);
        ParsedItem item = new ParsedItem(visitor.getAll().toArray(new ASTEntryWithChildren[0]), null);

        //module level: 2 comments
        assertEquals(2, item.getAstChildrenEntries().length);
        assertNull(item.getAstChildrenEntries()[0].children); //comment has no children
        assertNull(item.getAstChildrenEntries()[1].children); //comment has no children

    }

    public void testParsedItemCreation3() throws Exception {
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5);
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
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5);
        String str = "" +
                "class Foo(object):\n" +
                "    pass\n" +
                "if __name__ == '__main__':\n" +
                "    var = 10\n"
                +
                "\n" +
                "";

        SimpleNode node = parseLegalDocStr(str);

        OutlineCreatorVisitor visitor = OutlineCreatorVisitor.create(node);
        ParsedItem item = new ParsedItem(visitor.getAll().toArray(new ASTEntryWithChildren[0]), null);

        //module level: Foo and main
        assertEquals(2, item.getAstChildrenEntries().length);
        assertNull(item.getAstChildrenEntries()[0].children); //no children
    }

    /**
     * Check if the creation of a new structure will maintain the old items intact (as much as possible).
     *
     * @throws Exception
     */
    public void testNewChildrenStructure() throws Exception {
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5);
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

    public void testCommentsSkippedOnTryExcept() throws Exception {
        setDefaultVersion(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5);
        String str = "" +
                "if ( False ):\n" +
                "    pass\n" +
                "#--- comment4 is shown in outline, as expected\n" +
                "\n" +
                "try:\n" +
                "    pass\n" +
                "except NameError:\n" +
                "    pass\n" +
                "\n" +
                "#--- comment5 is not shown in outline !!! Why is that???\n" +
                "\n" +
                "pass\n" +
                "#--- comment6 is shown in outline, as expected" +
                "";

        SimpleNode node = parseLegalDocStr(str);
        OutlineCreatorVisitor visitor = OutlineCreatorVisitor.create(node);
        List<ASTEntry> all = visitor.getAll();
        assertEquals(3, all.size());
    }
}
