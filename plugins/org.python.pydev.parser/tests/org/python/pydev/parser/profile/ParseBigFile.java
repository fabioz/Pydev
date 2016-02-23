/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.profile;

import java.io.File;

import org.python.pydev.core.TestDependent;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;
import org.python.pydev.shared_core.io.FileUtils;

public class ParseBigFile extends PyParserTestBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ParseBigFile.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Initial times with 5 iterations:
     * 
     * Took: 1.625 secs
     * Took: 0.797 secs
     * Took: 0.828 secs
     * Took: 0.766 secs
     * Took: 0.765 secs
     * 
     * After using the FastCharStream:
     * 
     * Took: 0.453 secs
     * Took: 0.14 secs
     * Took: 0.14 secs
     * Took: 0.141 secs
     * Took: 0.14 secs
     * 
     * (impressive hum?)
     * 
     * -- note that this is directly proportional to the size of the string, so, while in small streams
     * there will be no noticeable change, in longer files the changes will be dramatical. E.g. A file
     * with 3MB of code would take about 3 minutes with the previous approach and would take 2 seconds with
     * the new approach.
     * 
     * @throws Exception
     */
    public void testBigFileParsing() throws Exception {
        String loc = TestDependent.TEST_PYDEV_PARSER_PLUGIN_LOC + "/tests/pysrc/data_string.py";
        String s = FileUtils.getFileContents(new File(loc));
        for (int i = 0; i < 5; i++) {
            @SuppressWarnings("unused")
            long curr = System.currentTimeMillis();
            SimpleNode node = parseLegalDocStr(s);

            //uncomment line below to see the time for parsing
            //System.out.println(StringUtils.format("Took: %s secs", (System.currentTimeMillis()-curr)/1000.0));
            SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(node);

            ASTEntry entry = visitor.getAsList(Str.class).get(0);
            String s0 = ((Str) entry.node).s;
            assertEquals(42, entry.node.beginLine);
            assertEquals(8, entry.node.beginColumn);
            assertTrue("Expecting big string. Received" + s0, s0.length() > 100);

        }
    }
}
