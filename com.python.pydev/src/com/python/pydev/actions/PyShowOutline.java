/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.actions;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.python.parser.SimpleNode;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;

public class PyShowOutline extends PyAction{

    public void run(IAction action) {
        System.out.println("here");
        PyEdit pyEdit = getPyEdit();
        SimpleNode ast = pyEdit.getAST();
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new ShowOutlineLabelProvider(), new ShowOutlineTreeContentProvider());
        dialog.setInput(ast);
        int i = dialog.open();
        System.out.println(i);
    }

}
