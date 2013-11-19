/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.fastparser;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Region;
import org.python.pydev.shared_core.parsing.Scopes;

/**
 * @author fabioz
 *
 */
public class ScopesParserTest extends TestCase {

    public static void main(String[] args) {
        try {
            ScopesParserTest test = new ScopesParserTest();
            test.setUp();
            test.testScopes4();
            test.tearDown();
            junit.textui.TestRunner.run(ScopesParserTest.class);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testScopes() throws Exception {
        Document doc = new Document("" +
                "#comment\n" +
                "class Foo(object):\n" +
                "    def method(self, a=(10,20)):\n"
                +
                "        '''\n" +
                "    multi string\n" +
                "        '''\n");
        Scopes scopes = new ScopesParser().createScopes(doc);
        assertEquals("" +
                "[1 [2 #comment 2]\n" +
                "[4 class Foo([3 object 3]):\n"
                +
                "    [5 [8 def method([6 self, a=([7 10,20 7]) 6]):\n" +
                "        [9 [10 '''\n"
                +
                "    multi string\n" +
                "        ''' 10]\n" +
                " 4] 5] 8] 9] 1]" +
                "", scopes.debugString(doc)
                .toString());
    }

    public void testScopes2() throws Exception {
        Document doc = new Document("a().o");
        Scopes scopes = new ScopesParser().createScopes(doc);
        assertEquals(new Region(0, 5), scopes.getScopeForSelection(2, 0));
    }

    public void testScopes4() throws Exception {
        Document doc = new Document("(1\n" +
                "\n" +
                "class Bar(object):\n" +
                "    call" +
                "");
        Scopes scopes = new ScopesParser().createScopes(doc);
        assertEquals("" +
                "[1 (1\n" +
                "\n" +
                "[3 class Bar([2 object 2]):\n" +
                "    [4 call 3] 4] 1]" +
                "", scopes
                .debugString(doc).toString());
    }

    public void testScopes3() throws Exception {
        Document doc = new Document("a(.o");
        Scopes scopes = new ScopesParser().createScopes(doc);
        assertEquals(new Region(0, 4), scopes.getScopeForSelection(2, 0));
    }

    public void testScopes1() throws Exception {
        Document doc = new Document("" +
                "#comment\n" +
                "class Foo(object):\n"
                +
                "    def method(self, a=(bb,(cc,dd))):\n" +
                "        '''\n" +
                "    multi string\n" +
                "        '''\n"
                +
                "class Class2:\n" +
                "    if True:\n" +
                "        a = \\\n" +
                "xx\n" +
                "    else:\n" +
                "        pass");
        Scopes scopes = new ScopesParser().createScopes(doc);
        assertEquals("" +
                "[1 [2 #comment 2]\n" +
                "[4 class Foo([3 object 3]):\n"
                +
                "    [5 [9 def method([6 self, a=([7 bb,([8 cc,dd 8]) 7]) 6]):\n" +
                "        [10 [11 '''\n"
                +
                "    multi string\n" +
                "        ''' 4] 5] 9] 10] 11]\n" +
                "[12 class Class2:\n"
                +
                "    [13 [14 if True:\n" +
                "        [15 a = \\\n" +
                "xx 14] 15]\n" +
                "    [16 else:\n"
                +
                "        [17 pass 12] 13] 16] 17] 1]" +
                "", scopes.debugString(doc).toString());

        assertEquals(new Region(0, 8), scopes.getScopeForSelection(0, 2));
        assertEquals(new Region(19, 6), scopes.getScopeForSelection(20, 0));
    }
}
