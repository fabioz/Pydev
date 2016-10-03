/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.ui.dialogs;

import java.util.HashMap;

import org.eclipse.jface.text.Document;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.ui.SWTTest;

import com.python.pydev.actions.PyOutlineSelectionDialog;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

public class PyOutlineSelectionDialogTest extends SWTTest {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyOutlineSelectionDialogTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    public void testIt() throws Exception {
        if (display != null) {
            String barDoc = "\n" +
                    "class Bar:\n" +
                    "    def barMethod(self):\n" +
                    "        pass\n" +
                    "\n" +
                    "";

            String testDoc = "GLOBAL_ATTR = 1\n" +
                    "GLOBAL2.IGNORE_THIS = 2\n" +
                    "" +
                    "class Test(Bar):\n"
                    +
                    "    test_attr = 1\n" +
                    "    test_attr.ignore = 2\n" +
                    "    test_attr2.ignore_this = 3\n" +
                    ""
                    +
                    "    class Test2:\n" +
                    "        def mmm(self):\n" +
                    "            self.attr1 = 10";

            IGrammarVersionProvider grammarVersionProvider = new IGrammarVersionProvider() {

                @Override
                public int getGrammarVersion() throws MisconfigurationException {
                    return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
                }

                @Override
                public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions()
                        throws MisconfigurationException {
                    return null;
                }
            };

            SourceModule moduleTest = AbstractModule.createModuleFromDoc("test", null, new Document(
                    testDoc), null, true, grammarVersionProvider);
            SourceModule moduleBar = AbstractModule.createModuleFromDoc("bar", null,
                    new Document(barDoc), null, true, grammarVersionProvider);

            Module astTest = (Module) moduleTest.getAst();
            Module astBar = (Module) moduleBar.getAst();

            HierarchyNodeModel testModel = new HierarchyNodeModel("test", (ClassDef) astTest.body[2]);
            HierarchyNodeModel barModel = new HierarchyNodeModel("bar", (ClassDef) astBar.body[0]);
            testModel.parents.add(barModel);

            HashMap<SimpleNode, HierarchyNodeModel> nodeToModel = new HashMap<SimpleNode, HierarchyNodeModel>();
            nodeToModel.put(astTest.body[2], testModel);

            PyOutlineSelectionDialog dialog = new PyOutlineSelectionDialog(new Shell(display), astTest, nodeToModel);

            dialog.open();
            //goToManual(display);
        }
    }

}
