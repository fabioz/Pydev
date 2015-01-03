/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.curr_exception;

import java.io.File;
import java.lang.ref.WeakReference;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;

public class EditIgnoredCaughtExceptions extends Action {

    private WeakReference<CurrentExceptionView> currentExceptionView;

    public EditIgnoredCaughtExceptions(CurrentExceptionView currentExceptionView) {
        this.currentExceptionView = new WeakReference<CurrentExceptionView>(currentExceptionView);
        this.setImageDescriptor(SharedUiPlugin.getImageCache().getDescriptor(UIConstants.HISTORY));
        this.setToolTipText("Edit currently ignored caught exceptions.");
    }

    @Override
    public void run() {
        IPath ignoreThrownExceptionsPath = PyExceptionBreakPointManager.getInstance().ignoreCaughtExceptionsWhenThrownFrom
                .getIgnoreThrownExceptionsPath();
        File file = ignoreThrownExceptionsPath.toFile();
        IEditorPart openFile = EditorUtils.openFile(file);

        if (openFile instanceof ITextEditor) {
            final ITextEditor textEditor = (ITextEditor) openFile;
            IDocumentProvider documentProvider = textEditor.getDocumentProvider();
            final IEditorInput input = openFile.getEditorInput();
            if (documentProvider instanceof IStorageDocumentProvider) {
                IStorageDocumentProvider storageDocumentProvider = (IStorageDocumentProvider) documentProvider;

                // Make sure the file is seen as UTF-8.
                storageDocumentProvider.setEncoding(input, "utf-8");
                textEditor.doRevertToSaved();
            }
            if (textEditor instanceof ISaveablePart) {
                IPropertyListener listener = new IPropertyListener() {

                    @Override
                    public void propertyChanged(Object source, int propId) {
                        if (propId == IWorkbenchPartConstants.PROP_DIRTY) {
                            if (source == textEditor) {
                                if (textEditor.getEditorInput() == input) {
                                    if (!textEditor.isDirty()) {
                                        PyExceptionBreakPointManager.getInstance().ignoreCaughtExceptionsWhenThrownFrom
                                                .updateIgnoreThrownExceptions();
                                    }
                                }
                            }
                        }
                    }
                };
                textEditor.addPropertyListener(listener);

            }
        }

        //        Code to provide a dialog to edit it (decided on opening the file instead).
        //        Collection<IgnoredExceptionInfo> ignoreThrownExceptionsForEdition = PyExceptionBreakPointManager.getInstance()
        //                .getIgnoreThrownExceptionsForEdition();
        //        HashMap<String, String> map = new HashMap<>();
        //        for (IgnoredExceptionInfo ignoredExceptionInfo : ignoreThrownExceptionsForEdition) {
        //            map.put(ignoredExceptionInfo.filename + ": " + ignoredExceptionInfo.line, ignoredExceptionInfo.contents);
        //        }
        //
        //        EditIgnoredCaughtExceptionsDialog dialog = new EditIgnoredCaughtExceptionsDialog(EditorUtils.getShell(), map);
        //        int open = dialog.open();
        //        if (open == dialog.OK) {
        //            Map<String, String> result = dialog.getResult();
        //
        //        } else {
        //            System.out.println("Cancel");
        //        }
    }
}
