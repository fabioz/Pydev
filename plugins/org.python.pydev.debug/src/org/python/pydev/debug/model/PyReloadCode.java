/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.io.File;
import java.util.List;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.debug.model.remote.ReloadCodeCommand;
import org.python.pydev.debug.ui.DebugPrefsPage;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;

public class PyReloadCode implements IPyEditListener {

    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {
        if (!DebugPrefsPage.getReloadModuleOnChange()) {
            return;
        }
        PyEdit edit = (PyEdit) baseEditor;
        File file = edit.getEditorFile();
        if (file != null) {

            IDebugTarget[] debugTargets = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
            if (debugTargets.length > 0) {
                ICallback<Boolean, IDebugTarget> callbackThatFilters = new ICallback<Boolean, IDebugTarget>() {

                    @Override
                    public Boolean call(IDebugTarget arg) {
                        return arg instanceof AbstractDebugTarget;
                    }
                };
                List<IDebugTarget> filter = ArrayUtils.filter(debugTargets, callbackThatFilters);
                if (filter.size() > 0) {
                    try {
                        IPythonNature pythonNature = edit.getPythonNature();
                        if (pythonNature != null) {
                            String moduleName = pythonNature.resolveModule(file);
                            if (moduleName != null) {
                                for (IDebugTarget iDebugTarget : filter) {
                                    AbstractDebugTarget target = (AbstractDebugTarget) iDebugTarget;
                                    target.postCommand(new ReloadCodeCommand(target, moduleName));
                                }
                            }
                        }
                    } catch (MisconfigurationException e) {
                        Log.log(e);
                    }
                }
            }
        }
    }

    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

}
