/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.SWT;
import org.python.pydev.core.IInfo;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.codecompletion.proposals.CtxInsensitiveImportComplProposal;
import org.python.pydev.editor.codecompletion.proposals.DefaultCompletionProposalFactory;

import junit.framework.TestCase;

public class CtxInsensitiveImportComplProposalTest extends TestCase {

    public static void main(String[] args) {
        try {
            CtxInsensitiveImportComplProposalTest analyzer2 = new CtxInsensitiveImportComplProposalTest();
            analyzer2.setUp();
            analyzer2.testApplyLocal9();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(CtxInsensitiveImportComplProposalTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CompletionProposalFactory.set(new DefaultCompletionProposalFactory());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        CompletionProposalFactory.set(null);
    }

    public void testCompletionDontAddFromImport() throws Exception {
        Document doc = new Document("from XXX import YYY\n");

        String replacementString = "YYY";
        int replacementOffset = doc.getLength();
        int cursorPosition = doc.getLength();
        int replacementLength = 0;
        String realImportRep = "from XXX import YYY";
        int priority = 0;
        String additionalProposalInfo = null;
        IContextInformation contextInformation = null;
        String displayString = "Import YYY (from XXX)";

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal(replacementString, replacementOffset, replacementLength,
                        cursorPosition,
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE, displayString, contextInformation, additionalProposalInfo,
                        priority, realImportRep, null);

        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());

        assertEquals("from XXX import YYY\nYYY", doc.get());
    }

    public void testCompletionGroupFromImport() throws Exception {
        Document doc = new Document("from XXX import YYY\n");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("from XXX import YYY, BBB\nBBB", doc.get());
    }

    public void testCompletionGroupFromImport2() throws Exception {
        Document doc = new Document("from XXX import (YYY, ZZZ)\n");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("from XXX import (YYY, ZZZ, BBB)\nBBB", doc.get());
    }

    public void testCompletionGroupFromImport3() throws Exception {
        //yeap, lot's of spaces there (more than 80 to test it)
        Document doc = new Document(
                "from XXX import (YYY,                                                                     ZZZ)\n");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals(
                "from XXX import (YYY,                                                                     ZZZ,\n\tBBB)\nBBB",
                doc.get());
    }

    public void testCompletionGroupFromImport4() throws Exception {
        //yeap, lot's of spaces there (more than 80 to test it)
        Document doc = new Document(
                "from XXX import YYY,                                                                     ZZZ\n");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals(
                "from XXX import YYY,                                                                     ZZZ,\\\n\tBBB\nBBB",
                doc.get());
    }

    public void testCompletionGroupFromImport5() throws Exception {
        //yeap, lot's of spaces there (more than 80 to test it)
        Document doc = new Document(
                "from XXX import WWW,                                                                     RRR # comment\n"
                        + "from XXX import YYY,                                                                     ZZZ\n");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        //        System.out.println(">>"+doc.get()+"<<");
        assertEquals(
                "from XXX import WWW,                                                                     RRR,\\\n"
                        + "\tBBB # comment\n"
                        + "from XXX import YYY,                                                                     ZZZ\nBBB",
                doc.get());
    }

    public void testCompletionFutureComesFirst() throws Exception {
        Document doc = new Document("from a import b\n");

        String replacementString = "with_statement";
        int replacementOffset = doc.getLength();
        int cursorPosition = doc.getLength();
        int replacementLength = 0;
        String realImportRep = "from __future__ import with_statement";
        int priority = 0;
        String additionalProposalInfo = null;
        IContextInformation contextInformation = null;
        String displayString = "Import with_statement (from __future__)";

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal(replacementString, replacementOffset, replacementLength,
                        cursorPosition,
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE, displayString, contextInformation, additionalProposalInfo,
                        priority, realImportRep, null);

        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());

        assertEquals("from __future__ import with_statement\nfrom a import b\nwith", doc.get());
    }

    public void testApplyNonLocal() throws Exception {
        Document doc = new Document("from XXX import YYY\n" +
                "def m1():\n" +
                "    ");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("" +
                "from XXX import YYY, BBB\n" +
                "def m1():\n" +
                "    BBB" +
                "", doc.get());
    }

    public void testApplyLocal() throws Exception {
        Document doc = new Document("from XXX import YYY\n" +
                "def m1():\n" +
                "    ");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.setAddLocalImport(true);
        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("" +
                "from XXX import YYY\n" +
                "def m1():\n" +
                "    from XXX import BBB\n" +
                "    BBB" +
                "",
                doc.get());
    }

    public void testApplyLocal2() throws Exception {
        Document doc = new Document("def m1():\n" +
                "\tdef another():\n" +
                "\t\t" +
                "");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.setAddLocalImport(true);
        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("" +
                "def m1():\n" +
                "\tdef another():\n" +
                "\t\tfrom XXX import BBB\n" +
                "\t\tBBB" +
                "",
                doc.get());
    }

    public void testApplyLocal3() throws Exception {
        Document doc = new Document("def m1():\n" +
                "\tdef another():\n" +
                "\t\tcall(\n" +
                "\t\t\tra, " +
                "");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.setAddLocalImport(true);
        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("" +
                "def m1():\n" +
                "\tdef another():\n" +
                "\t\tfrom XXX import BBB\n" +
                "\t\tcall(\n"
                +
                "\t\t\tra, BBB" +
                "", doc.get());
    }

    public void testApplyLocal4() throws Exception {
        Document doc = new Document("\n");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("BBB", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import BBB (from XXX)", null, null, 0, "from XXX import BBB", null);

        prop.setAddLocalImport(true);
        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("" +
                "from XXX import BBB\n" +
                "\nBBB" +
                "", doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal5() throws Exception {
        Document doc = new Document("class Bar(");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("" +
                "import sys\n" +
                "class Bar(sys" +
                "", doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal6() throws Exception {
        Document doc = new Document("\n\nclass Bar(");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "\t";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("" +
                "import sys\n" +
                "\n" +
                "\nclass Bar(sys" +
                "",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal7() throws Exception {
        Document doc = new Document("\n\nclass Bar():\n\n    s");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "\t";
        prop.addLocalImportsOnTopOfFunc = false;
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("\n\nclass Bar():\n\n    import sys\n    ssys", doc.get().replace("\r\n", "\n")
                .replace('\r', '\n'));
    }

    public void testApplyLocal8() throws Exception {
        Document doc = new Document("\n\nclass Bar():\n\n    s\n    s");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "\t";
        prop.addLocalImportsOnTopOfFunc = false;
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("\n\nclass Bar():\n\n    s\n    import sys\n    ssys",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal9() throws Exception {
        String initial = "" +
                "if True:\n" +
                "    pass\n" +
                "\n" +
                "eggs = D" +
                "";
        Document doc = new Document(initial);

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("Decimal", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import Decimal", null, null, 0, "import Decimal", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.apply(doc, '\n', SWT.SHIFT, doc.getLength());
        assertEquals("" +
                "import Decimal\n" +
                "if True:\n" +
                "    pass\n" +
                "\n" +
                "eggs = DDecimal" +
                "",
                doc.get()
                        .replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal10() throws Exception {
        String initial = "" +
                "def m1():\n" +
                "    if True:\n" +
                "        pass\n" +
                "    \n" +
                "    eggs = D" +
                "";
        Document doc = new Document(initial);

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("Decimal", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import Decimal", null, null, 0, "import Decimal", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.addLocalImportsOnTopOfFunc = false;
        prop.apply(doc, '\n', SWT.SHIFT, doc.getLength());
        assertEquals("" +
                "def m1():\n" +
                "    if True:\n" +
                "        pass\n" +
                "    \n" +
                "    import Decimal\n"
                +
                "    eggs = DDecimal" +
                "", doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal11() throws Exception {
        Document doc = new Document(
                "def test(\n" +
                        "        rara\n" +
                        "    ):\n" +
                        "    " +
                        "");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals("def test(\n" +
                "        rara\n" +
                "    ):\n" +
                "    import sys\n" +
                "    sys" +
                "",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal12() throws Exception {
        Document doc = new Document(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "    " +
                        "");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "    import sys\n" +
                        "    sys" +
                        "",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal13() throws Exception {
        Document doc = new Document(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    " +
                        "");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import sys\n" +
                        "    sys" +
                        "",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal14() throws Exception {
        Document doc = new Document(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "    ");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "    import sys\n" +
                        "    sys" +
                        "",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal15() throws Exception {
        Document doc = new Document(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "\n" +
                        "    from x import foo\n" +
                        "    ");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "\n" +
                        "    from x import foo\n" +
                        "    import sys\n" +
                        "    sys" +
                        "",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal16() throws Exception {
        Document doc = new Document(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "\n" +
                        "    from x import foo\n" +
                        "\n" +
                        "    ");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "\n" +
                        "    from x import foo\n" +
                        "    import sys\n" +
                        "\n" +
                        "    sys" +
                        "",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal17() throws Exception {
        Document doc = new Document(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "\n" +
                        "    from x import(\n"
                        + "    foo)\n" +
                        "\n" +
                        "    ");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "\n" +
                        "    from x import(\n"
                        + "    foo)\n" +
                        "    import sys\n" +
                        "\n" +
                        "    sys" +
                        "",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }

    public void testApplyLocal18() throws Exception {
        Document doc = new Document(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "\n" +
                        "    from x import \\\n"
                        + "    foo\n" +
                        "\n" +
                        "    ");

        CtxInsensitiveImportComplProposal prop = (CtxInsensitiveImportComplProposal) CompletionProposalFactory.get()
                .createCtxInsensitiveImportComplProposal("sys", doc.getLength(), 0, doc.getLength(),
                        IInfo.ATTRIBUTE_WITH_IMPORT_TYPE,
                        "Import sys", null, null, 0, "import sys", null);

        prop.setAddLocalImport(true);
        prop.indentString = "    ";
        prop.apply(doc, '\n', 0, doc.getLength());
        assertEquals(
                "def test(\n" +
                        "    rara\n" +
                        "):\n" +
                        "\n" +
                        "\n" +
                        "    '''testing\n" +
                        "'''\n" +
                        "    import bar\n" +
                        "\n" +
                        "    from x import \\\n"
                        + "    foo\n" +
                        "    import sys\n" +
                        "\n" +
                        "    sys" +
                        "",
                doc.get().replace("\r\n", "\n").replace('\r', '\n'));
    }
}
