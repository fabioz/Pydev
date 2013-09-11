/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;

public interface IPyEditListener {

    /**
     * Anytime a BaseEditor is saved, it will notify that to its listeners.
     * @param edit the BaseEditor that has just been saved.
     */
    void onSave(BaseEditor edit, IProgressMonitor monitor);

    /**
     * When the actions are being created in BaseEditor, this method is called, so that contributors might add their own actions 
     * @param resources the resource bundle it used
     * @param edit the BaseEditor
     */
    void onCreateActions(ListResourceBundle resources, BaseEditor edit, IProgressMonitor monitor);

    /**
     * This method is called whenever the edit is disposed
     * @param edit the edit that will be disposed.
     */
    void onDispose(BaseEditor edit, IProgressMonitor monitor);

    /**
     * Use to notify listeners that the document that the editor was editing has just changed.
     * 
     * @param document the document being edited
     * @param edit the editor that had the document changed
     * @param monitor the monitor for the change
     */
    void onSetDocument(IDocument document, BaseEditor edit, IProgressMonitor monitor);
}
