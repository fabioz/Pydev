/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 29, 2006
 */
package com.python.pydev.refactoring.markoccurrences;

import java.lang.ref.WeakReference;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener2;
import org.python.pydev.editor.PyEdit;

/**
 * This class dispatches the request (and gives it info if it should keep going with it) for marking occurrences.
 * 
 * Note: We should only let it make it if the editor has the focus (otherwise we will just be wasting cicles).
 * 
 * @author Fabio
 */
public class MarkOccurrencesDispatcher implements IPyEditListener, IDocumentListener, IPyEditListener2 {

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
    }

    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    public void documentChanged(DocumentEvent event) {
    }

    public void handleCursorPositionChanged(PyEdit edit, PySelection ps) {
        MarkOccurrencesJob.scheduleRequest(new WeakReference<PyEdit>(edit), ps);
    }
}
