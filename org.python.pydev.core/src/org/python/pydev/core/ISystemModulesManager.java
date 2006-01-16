/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.util.Collection;

public interface ISystemModulesManager {

    public abstract void regenerateForcedBuilltins(Collection<String> forcedLibs);

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getBuiltins()
     */
    public abstract String[] getBuiltins();

    /**
     * @param forcedLibs
     */
    public abstract void setBuiltins(Collection<String> forcedLibs);

}