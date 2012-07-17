/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.lang.ref.WeakReference;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.core.callbacks.CallbackWithListeners;

/**
 * Provides the keywords to be used for an editor. Could be extended to add more heuristics.
 * 
 * @author fabioz
 *
 */
@SuppressWarnings("rawtypes")
public class PyEditBasedCodeScannerKeywords implements ICodeScannerKeywords {

    private WeakReference<PyEdit> edit;
    private final CallbackWithListeners callbackWithListeners = new CallbackWithListeners();

    /**
     * Helper to notify when the input changes from a cython file to a non-cython file (and vice-versa).
     * 
     * @author fabioz
     */
    private static class CythonStatusChangeNotifier implements IPyEditListener, IPyEditListener3 {

        private WeakReference<CallbackWithListeners> ref;
        private boolean currentIsCythonFile;

        public CythonStatusChangeNotifier(PyEditBasedCodeScannerKeywords pyEditBasedCodeScannerKeywords) {
            this.ref = new WeakReference<CallbackWithListeners>(pyEditBasedCodeScannerKeywords.callbackWithListeners);
            PyEdit pyEdit = pyEditBasedCodeScannerKeywords.edit.get();
            if (pyEdit == null) {
                currentIsCythonFile = false;
            } else {
                currentIsCythonFile = pyEdit.isCythonFile();
            }
        }

        @SuppressWarnings("unchecked")
        public void onInputChanged(PyEdit edit, IEditorInput oldInput, IEditorInput input, IProgressMonitor monitor) {
            boolean cythonFile = edit.isCythonFile();
            if (cythonFile != currentIsCythonFile) {
                currentIsCythonFile = cythonFile;
                CallbackWithListeners callbackWithListeners = this.ref.get();
                if (callbackWithListeners != null) {
                    callbackWithListeners.call(null);
                } else {
                    edit.removePyeditListener(this);
                }
            }
        }

        public void onSave(PyEdit edit, IProgressMonitor monitor) {
        }

        public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
        }

        public void onDispose(PyEdit edit, IProgressMonitor monitor) {
        }

        public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
        }

    }

    public PyEditBasedCodeScannerKeywords(PyEdit pyEdit) {
        this.edit = new WeakReference<PyEdit>(pyEdit);
        pyEdit.addPyeditListener(new CythonStatusChangeNotifier(this));
    }

    public CallbackWithListeners getOnChangeCallbackWithListeners() {
        return callbackWithListeners;
    }

    public String[] getKeywords() {
        PyEdit pyEdit = edit.get();
        if (pyEdit == null) {
            return PyCodeScanner.DEFAULT_KEYWORDS;
        }
        if (pyEdit.isCythonFile()) {
            return PyCodeScanner.CYTHON_KEYWORDS;

        } else {
            return PyCodeScanner.DEFAULT_KEYWORDS;

        }
    }

}
