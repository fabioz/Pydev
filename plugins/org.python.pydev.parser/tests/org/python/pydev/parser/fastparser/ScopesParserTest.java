/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.fastparser;

import org.eclipse.jface.text.Document;
import org.python.pydev.parser.fastparser.ScopesParser.Scopes;

import junit.framework.TestCase;

/**
 * @author fabioz
 *
 */
public class ScopesParserTest extends TestCase {

    public static void main(String[] args) {
        try {
            ScopesParserTest test = new ScopesParserTest();
            test.setUp();
            test.testScopes1();
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
        		"    def method(self, a=(10,20)):\n" +
        		"        '''\n" +
        		"    multi string\n" +
        		"        '''\n");
        Scopes scopes = ScopesParser.createScopes(doc);
        assertEquals(scopes.debugString(doc).toString(), "" +
        		"[1 [2 #comment 2]\n" +
        		"[4 class Foo([3 object 3]):\n" +
        		"    [7 def method([5 self, a=([6 10,20 6]) 5]):\n" +
        		"        [8 '''\n" +
        		"    multi string\n" +
        		"        ''' 8]\n" +
        		" 4] 7] 1]" +
        		"");
    }
    
    public void testScopes1() throws Exception {
        Document doc = new Document("" +
                "#comment\n" +
                "class Foo(object):\n" +
                "    def method(self, a=(bb,(cc,dd))):\n" +
                "        '''\n" +
                "    multi string\n" +
                "        '''\n" +
                "class Class2:\n" +
                "    if True:\n" +
                "        a = \\\n" +
                "xx\n" +
                "    else:\n" +
                "        pass");
        Scopes scopes = ScopesParser.createScopes(doc);
        assertEquals(scopes.debugString(doc).toString(), "" +
        		"[1 [2 #comment 2]\n" +
        		"[4 class Foo([3 object 3]):\n" +
        		"    [8 def method([5 self, a=([6 bb,([7 cc,dd 7]) 6]) 5]):\n" +
        		"        [9 '''\n" +
        		"    multi string\n" +
        		"        ''' 4] 8] 9]\n" +
        		"[10 class Class2:\n" +
        		"    [11 if True:\n" +
        		"        a = \\\n" +
        		"xx 11]\n" +
        		"    [12 else:\n" +
        		"        pass 10] 12] 1]" +
        		"");
    }
}
