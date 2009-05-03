package com.python.pydev.analysis.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Creates the selection dialog to be used to select a token.
 * 
 * @author Fabio
 */
public class GlobalsDialogFactory{

    /**
     * Creates the dialog according to the Eclipse version we have (on 3.2, the old API is used)
     */
    public static SelectionDialog create(Shell shell, List<AbstractAdditionalInterpreterInfo> additionalInfo, String selectedText){
        boolean expectedError = true;
        try{
            GlobalsTwoPanelElementSelector2 newDialog = new GlobalsTwoPanelElementSelector2(shell, true, selectedText);
            //If we were able to instance it, the error is no longer expected!
            expectedError = false;
            
            newDialog.setElements(additionalInfo);
            return newDialog;
        }catch(Throwable e){
            //That's OK: it's only available for Eclipse 3.3 onwards.
            if(expectedError){
                PydevPlugin.log(e);
            }
        }
        
        //If it got here, we were unable to create the new dialog (show the old -- compatible with 3.2)
        GlobalsTwoPaneElementSelector dialog;
        dialog = new GlobalsTwoPaneElementSelector(shell);
        dialog.setMessage("Filter");
        if(selectedText != null && selectedText.length() > 0){
            dialog.setFilter(selectedText);
        }
        
        
        List<IInfo> lst = new ArrayList<IInfo>();
        
        for(AbstractAdditionalInterpreterInfo info:additionalInfo){
            lst.addAll(info.getAllTokens());
        }
        
        dialog.setElements(lst.toArray());
        return dialog;
    }

}
