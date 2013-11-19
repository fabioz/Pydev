/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;

public class PyAddBlockCommentTest extends TestCase {

    public void testBlock() throws Exception {
        Document doc = null;
        FormatStd std = new FormatStd();

        doc = new Document("cc");
        new PyAddBlockComment(std, 10, true, true, true).perform(new PySelection(doc, 0, 0, 0));
        PySelectionTest.checkStrEquals("" +
                "#---------\r\n" +
                "# cc\r\n" +
                "#---------", doc.get());

        doc = new Document("\t cc");
        new PyAddBlockComment(std, 10, true, true, true).perform(new PySelection(doc, 0, 0, 0));
        PySelectionTest.checkStrEquals("" +
                "\t #----\r\n" +
                "\t # cc\r\n" +
                "\t #----", doc.get());

        doc = new Document("class Foo(object):");
        new PyAddBlockComment(std, 10, true, true, true).perform(new PySelection(doc, 0, 0, 0));
        PySelectionTest.checkStrEquals("" +
                "#---------\r\n" +
                "# Foo\r\n" +
                "#---------\r\n" +
                "class Foo(object):",
                doc.get());

        doc = new Document("class Information( UserDict.UserDict, IInformation ):");
        new PyAddBlockComment(std, 10, true, true, true).perform(new PySelection(doc, 0, 0, 0));
        PySelectionTest.checkStrEquals("" +
                "#---------\r\n" +
                "# Information\r\n" +
                "#---------\r\n"
                +
                "class Information( UserDict.UserDict, IInformation ):", doc.get());

        doc = new Document("def Information( (UserDict, IInformation) ):");
        new PyAddBlockComment(std, 10, true, true, true).perform(new PySelection(doc, 0, 0, 0));
        PySelectionTest.checkStrEquals("" +
                "#---------\r\n" +
                "# Information\r\n" +
                "#---------\r\n"
                +
                "def Information( (UserDict, IInformation) ):", doc.get());

        //without class behavior
        doc = new Document("class Foo(object):");
        new PyAddBlockComment(std, 10, true, false, true).perform(new PySelection(doc, 0, 0, 0));
        PySelectionTest.checkStrEquals("" +
                "#---------\r\n" +
                "# class Foo(object):\r\n" +
                "#---------" +
                "",
                doc.get());

        //aligned class
        doc = new Document("    class Foo(object):");
        new PyAddBlockComment(std, 10, true, true, true).perform(new PySelection(doc, 0, 0, 0));
        PySelectionTest.checkStrEquals("" +
                "    #-----\r\n" +
                "    # Foo\r\n" +
                "    #-----\r\n"
                +
                "    class Foo(object):" +
                "", doc.get());

    }

}
