/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;


public interface ISystemModulesManager extends IModulesManager {

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getBuiltins()
     */
    public abstract String[] getBuiltins();


}