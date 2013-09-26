/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.OfflineActionTarget;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * This is the class that manages the actions that are binded after Ctrl+2
 */
public class OfflineActionsManager {

    Map<String, ActionInfo> onOfflineActionListeners = new HashMap<String, ActionInfo>();

    public Collection<ActionInfo> getOfflineActionDescriptions() {
        return onOfflineActionListeners.values();
    }

    public void addOfflineActionListener(String key, IAction action) {
        onOfflineActionListeners.put(key.toLowerCase(),
                new ActionInfo(action, "not described", key.toLowerCase(), true));
    }

    public void addOfflineActionListener(String key, IAction action, String description, boolean needsEnter) {
        onOfflineActionListeners.put(key.toLowerCase(), new ActionInfo(action, description, key.toLowerCase(),
                needsEnter));
    }

    public boolean activatesAutomaticallyOn(String key) {
        ActionInfo info = onOfflineActionListeners.get(key.toLowerCase());
        if (info != null) {
            if (!info.needsEnter) {
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
        List<String> parameters = null;
        if (actionInfo == null) {
            List<String> split = StringUtils.split(requestedStr, ' ');
            //We have one more shot: if we have spaces, it can be bound to the first part and have
            //parameters
            if (split.size() > 0) {
                actionInfo = onOfflineActionListeners.get(split.remove(0));
                parameters = split;
            }

            if (actionInfo == null) {
                target.statusError("No action info was found binded to:" + requestedStr);
                return false;
            } else {
                //it has parameters.
            }
        }

        IAction action = actionInfo.action;
        if (action == null) {
            target.statusError("No action was found binded to:" + requestedStr);
            return false;
        }
        if (action instanceof IOfflineActionWithParameters) {
            if (parameters == null) {
                parameters = new ArrayList<String>();
            }
            ((IOfflineActionWithParameters) action).setParameters(parameters);
        }

        try {
            action.run();
        } catch (Throwable e) {
            target.statusError("Exception raised when executing action:" + requestedStr + " - " + e.getMessage());
            Log.log(e);
            return false;
        }
        return true;
    }

    public boolean hasOfflineAction(String key) {
        return onOfflineActionListeners.get(key.toLowerCase()) != null;
    }

}
