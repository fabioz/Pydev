/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.util.List;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractModule {

    public abstract List getWildImportedModules();
    public abstract List getTokenImportedModules();
    public abstract List getGlobalTokens();

}
