/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.io.Serializable;
import java.util.List;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractModule implements Serializable{

    public abstract List getWildImportedModules();
    public abstract List getTokenImportedModules();
    public abstract List getGlobalTokens();

}
