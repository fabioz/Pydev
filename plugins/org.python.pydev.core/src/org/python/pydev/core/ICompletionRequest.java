/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.io.File;

public interface ICompletionRequest {

    /**
     * @return the python nature to which this completion is being request.d
     */
    IPythonNature getNature();

    /**
     * @return the editor file for the editor that's requesting this completion.
     * It can be null if the completion is not being called from an editor.
     */
    File getEditorFile();

}