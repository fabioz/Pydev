/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.ui.dialogs;

import org.eclipse.jface.text.Document;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ui.SWTTest;
import org.python.pydev.ui.dialogs.TreeSelectionDialog;

import com.python.pydev.actions.ShowOutlineLabelProvider;
import com.python.pydev.actions.ShowOutlineTreeContentProvider;

public class TreeSelectionDialogTest extends SWTTest {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TreeSelectionDialogTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected Display createDisplay() {
        return Display.getCurrent();
    }

    public void testIt() throws Exception{
        if(display != null){
            String doc = 
            "GLOBAL_ATTR = 1\n" +
            "GLOBAL2.IGNORE_THIS = 2\n" +
            "" +
            "class Test:\n" +
            "    test_attr = 1\n" +
            "    test_attr.ignore = 2\n" +
            "    test_attr2.ignore_this = 3\n" +
            "" +
            "    class Test2:\n" +
            "        def mmm(self):\n" +
            "            self.attr1 = 10";
                    
            SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("test", null, new Document(doc), null, 0);
            TreeSelectionDialog dialog = new TreeSelectionDialog(shell, new ShowOutlineLabelProvider(), new ShowOutlineTreeContentProvider());
            dialog.setInput(module.getAst());
//            dialog.open();
        }
    }

}
