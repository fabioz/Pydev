/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.shared_ui.editor.IPyEditListener3;

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

        @Override
        @SuppressWarnings("unchecked")
        public void onInputChanged(BaseEditor baseEditor, IEditorInput oldInput, IEditorInput input,
                IProgressMonitor monitor) {
            PyEdit edit = (PyEdit) baseEditor;
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

        @Override
        public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {
        }

        @Override
        public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
        }

        @Override
        public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
        }

        @Override
        public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {
        }

    }

    public PyEditBasedCodeScannerKeywords(PyEdit pyEdit) {
        this.edit = new WeakReference<PyEdit>(pyEdit);
        pyEdit.addPyeditListener(new CythonStatusChangeNotifier(this));
    }

    @Override
    public CallbackWithListeners getOnChangeCallbackWithListeners() {
        return callbackWithListeners;
    }

    @Override
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
