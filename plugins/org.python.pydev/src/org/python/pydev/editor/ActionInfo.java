/*
 * Created on Apr 2, 2006
 */
package org.python.pydev.editor;

import org.eclipse.jface.action.IAction;

public class ActionInfo{
    public IAction action;
    public String description;
    public String binding;
    public boolean needsEnter;
    
    public ActionInfo(IAction action, String description, String binding, boolean needsEnter){
        this.action = action;
        this.description = description;
        this.binding = binding;
        this.needsEnter = needsEnter;
    }
}