package org.python.pydev.editor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.actions.OfflineActionTarget;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This is the class that manages the actions that are binded after Ctrl+2
 */
public class OfflineActionsManager {
    
    Map<String, ActionInfo> onOfflineActionListeners = new HashMap<String, ActionInfo>();
    
    public Collection<ActionInfo> getOfflineActionDescriptions(){
        return onOfflineActionListeners.values();
    }
    
    public void addOfflineActionListener(String key, IAction action) {
        onOfflineActionListeners.put(key.toLowerCase(), new ActionInfo(action, "not described", key.toLowerCase(), true));
    }
    
    public void addOfflineActionListener(String key, IAction action, String description, boolean needsEnter) {
        onOfflineActionListeners.put(key.toLowerCase(), new ActionInfo(action, description, key.toLowerCase(), needsEnter));
    }
    
    public boolean activatesAutomaticallyOn(String key){
        ActionInfo info = onOfflineActionListeners.get(key.toLowerCase());
        if(info != null){
            if(!info.needsEnter){
                return true;
            }
        }
        return false;
    }
    /**
     * @return if an action was binded and was successfully executed
     */
    public boolean onOfflineAction(String requestedStr, OfflineActionTarget target) {
        ActionInfo actionInfo = onOfflineActionListeners.get(requestedStr.toLowerCase());
        if(actionInfo == null){
            target.statusError("No action was found binded to:"+requestedStr);
            return false;
            
        }
            
        IAction action = actionInfo.action;
        if(action == null){
            target.statusError("No action was found binded to:"+requestedStr);
            return false;
        }
        
        try {
            action.run();
        } catch (Throwable e) {
            target.statusError("Exception raised when executing action:"+requestedStr+" - "+e.getMessage());
            PydevPlugin.log(e);
            return false;
        }
        return true;
    }

	public boolean hasOfflineAction(String key) {
		return onOfflineActionListeners.get(key.toLowerCase()) != null;
	}

}
