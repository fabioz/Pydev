/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.actions;
import org.eclipse.jface.action.IAction;
import org.python.pydev.core.uiutils.DialogMemento;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.Location;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.ui.dialogs.TreeSelectionDialog;


public class PyShowOutline extends PyAction{

    public void run(IAction action) {
        final DialogMemento memento = new DialogMemento(getShell(),"com.python.pydev.actions.PyShowOutline");
        
        PyEdit pyEdit = getPyEdit();
        SimpleNode ast = pyEdit.getAST();
        
        TreeSelectionDialog dialog = new PyOutlineSelectionDialog(
                getShell(), new ShowOutlineLabelProvider(), new ShowOutlineTreeContentProvider(), memento);

        dialog.setInput(ast);
        dialog.open();
        Object[] result = dialog.getResult();
        if(result != null && result.length > 0){
            ASTEntry entry = (ASTEntry) result[0];
            Location location = new Location(
                    NodeUtils.getNameLineDefinition(entry.node)-1,
                    NodeUtils.getNameColDefinition(entry.node)-1);
            new PyOpenAction().showInEditor(pyEdit, location, location);
        }
    }

}
