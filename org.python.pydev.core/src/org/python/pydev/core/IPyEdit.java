/*
 * Created on 12/06/2005
 */
package org.python.pydev.core;

import org.eclipse.ui.IEditorInput;


/**
 * @author Fabio
 */
public interface IPyEdit {

    /**
     * @return
     */
    IPythonNature getPythonNature();

    /**
     * @return
     */
    IEditorInput getEditorInput();

}
