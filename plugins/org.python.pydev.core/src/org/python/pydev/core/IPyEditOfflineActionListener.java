package org.python.pydev.core;

import org.eclipse.jface.action.IAction;

public interface IPyEditOfflineActionListener {

    public void addOfflineActionListener(String key, IAction action, String description, boolean needsEnter);

}
