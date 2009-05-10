package org.python.pydev.ui.dialogs;

import java.util.Map;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractMapOfStringsInputDialog extends AbstractKeyValueDialog{

    private Map<String, String> map;


    public AbstractMapOfStringsInputDialog(Shell shell, String dialogTitle, String dialogMessage, Map<String, String> map) {
        super(shell, dialogTitle, dialogMessage);       
        this.map = map;
    }
    
    
    protected String getInitialMessage(){
        return "Please, inform the name and related value for the variable";
    }
    

    protected String getValueLabelText(){
        return "Value: ";
    }


    protected String getKeyLabelText(){
        return "Name: ";
    }
    
    
    /**
     * @return a listened that should clear or set the error message after any change.
     */
    protected Listener createChangesValidator(){
        return new Listener() {
            public void handleEvent(Event event) {
                
                String errorMessage = null;
                
                String key = keyField.getText().trim();
                if (key.equals("")){
                    errorMessage = "The variable name must be specified";
                }
                
                String value = valueField.getText().trim();
                if (errorMessage == null && value.equals("")){
                    errorMessage = "The value must be specified";
                }
                if(errorMessage == null){
                    if(map.containsKey(key)){
                        errorMessage = "The key: "+key+" is already specified.";
                    }
                }
                setErrorMessage(errorMessage);
            }
        };
    }   


}
