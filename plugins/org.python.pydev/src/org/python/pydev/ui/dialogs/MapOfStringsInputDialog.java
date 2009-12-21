package org.python.pydev.ui.dialogs;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;


/**
 * @author fabioz
 */
public class MapOfStringsInputDialog extends AbstractMapOfStringsInputDialog {

    public MapOfStringsInputDialog(Shell shell, String dialogTitle, String dialogMessage, Map<String, String> map) {
        super(shell, dialogTitle, dialogMessage, map);
    }

    @Override
    protected String handleBrowseButton(){
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);

        String file = dialog.open();
        return file;
    }

    
    
    
}
