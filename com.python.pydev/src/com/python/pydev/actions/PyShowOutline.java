/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.actions;
import org.eclipse.jface.action.IAction;
import org.python.parser.SimpleNode;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.Location;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.ui.dialogs.TreeSelectionDialog;

public class PyShowOutline extends PyAction{

    public void run(IAction action) {
        PyEdit pyEdit = getPyEdit();
        SimpleNode ast = pyEdit.getAST();
        TreeSelectionDialog dialog = new TreeSelectionDialog(getShell(), new ShowOutlineLabelProvider(), new ShowOutlineTreeContentProvider());
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
