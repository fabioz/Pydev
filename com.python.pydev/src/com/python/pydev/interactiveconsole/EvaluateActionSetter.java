/*
 * Created on Mar 7, 2006
 */
package com.python.pydev.interactiveconsole;

import java.util.ListResourceBundle;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;

public class EvaluateActionSetter implements IPyEditListener {
    
    private static final String EVALUATE_ACTION_ID = "org.python.pydev.interactiveconsole.EvaluateActionSetter";

    public void onSave(PyEdit edit) {
        //ignore
    }

    public void onCreateActions(ListResourceBundle resources, final PyEdit edit) {
        edit.setAction(EVALUATE_ACTION_ID, new Action() {  

            public int getAccelerator() {
                return SWT.CTRL|'\r';
            }

            public String getText() {
                return "Evaluate Python Code in Console";
            }
            
            public  void run(){
                PySelection selection = new PySelection(edit);
                System.out.println("Selected code:"+selection.getTextSelection().getText());
            }

        });

        edit.setActionActivationCode(EVALUATE_ACTION_ID, '\r', -1, SWT.CTRL);
    }

}
