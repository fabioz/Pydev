/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;

public interface IPyEditListener {

    /**
     * Anytime a PyEdit is saved, it will notify that to its listeners.
     * @param edit the PyEdit that has just been saved.
     */
    void onSave(PyEdit edit, IProgressMonitor monitor);

    /**
     * When the actions are being created in PyEdit, this method is called, so that contributors might add their own actions 
     * @param resources the resource bundle it used
     * @param edit the PyEdit
     */
    void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor);

    /**
     * This method is called whenever the edit is disposed
     * @param edit the edit that will be disposed.
     */
    void onDispose(PyEdit edit, IProgressMonitor monitor);

    /**
     * Use to notify listeners that the document that the editor was editing has just changed.
     * 
     * @param document the document being edited
     * @param edit the editor that had the document changed
     * @param monitor the monitor for the change
     */
    void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor);
}
