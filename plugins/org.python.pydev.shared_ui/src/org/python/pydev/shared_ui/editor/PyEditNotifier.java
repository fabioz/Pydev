/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor;

import java.lang.ref.WeakReference;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.shared_core.log.Log;

/**
 * Helper to give notifications for the listeners of the editor.
 * 
 * @author Fabio
 */
public class PyEditNotifier {

    private WeakReference<BaseEditor> pyEdit;

    public static interface INotifierRunnable {
        public void run(IProgressMonitor monitor);
    }

    public PyEditNotifier(BaseEditor edit) {
        this.pyEdit = new WeakReference<BaseEditor>(edit);
    }

    /**
     * Notifies listeners that the actions have just been created in the editor.
     */
    public void notifyOnCreateActions(final ListResourceBundle resources) {
        final BaseEditor edit = pyEdit.get();
        if (edit == null) {
            return;
        }
        INotifierRunnable runnable = new INotifierRunnable() {
            public void run(final IProgressMonitor monitor) {
                for (IPyEditListener listener : edit.getAllListeners()) {
                    try {
                        if (!monitor.isCanceled()) {
                            listener.onCreateActions(resources, edit, monitor);
                        }
                    } catch (Exception e) {
                        //must not fail
                        Log.log(e);
                    }
                }
            }
        };
        runIt(runnable);
    }

    /**
     * Notifies listeners that the editor has just been saved
     */
    public void notifyOnSave() {
        final BaseEditor edit = pyEdit.get();
        if (edit == null) {
            return;
        }
        INotifierRunnable runnable = new INotifierRunnable() {
            public void run(IProgressMonitor monitor) {
                for (IPyEditListener listener : edit.getAllListeners()) {
                    try {
                        if (!monitor.isCanceled()) {
                            listener.onSave(edit, monitor);
                        }
                    } catch (Throwable e) {
                        //must not fail
                        Log.log(e);
                    }
                }
            }
        };
        runIt(runnable);

    }

    /**
     * Helper function to run the notifications of the editor in a job.
     * 
     * @param runnable the runnable to be run.
     */
    private void runIt(final INotifierRunnable runnable) {
        Job job = new Job("PyEditNotifier") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                runnable.run(monitor);
                return Status.OK_STATUS;
            }

        };
        job.setPriority(Job.SHORT);
        job.setSystem(true);
        job.schedule();
    }

    /**
     * Notifies listeners that the editor has just been disposed
     */
    public void notifyOnDispose() {
        final BaseEditor edit = pyEdit.get();
        if (edit == null) {
            return;
        }

        INotifierRunnable runnable = new INotifierRunnable() {
            public void run(IProgressMonitor monitor) {
                for (IPyEditListener listener : edit.getAllListeners(false)) {
                    try {
                        if (!monitor.isCanceled()) {
                            listener.onDispose(edit, monitor);
                        }
                    } catch (Throwable e) {
                        //no need to worry... as we're disposing, in shutdown, we may not have access to some classes anymore
                    }
                }
            }
        };
        runIt(runnable);
    }

    /**
     * @param document the document just set
     */
    public void notifyOnSetDocument(final IDocument document) {
        final BaseEditor edit = pyEdit.get();
        if (edit == null) {
            return;
        }
        INotifierRunnable runnable = new INotifierRunnable() {
            public void run(IProgressMonitor monitor) {
                for (IPyEditListener listener : edit.getAllListeners()) {
                    try {
                        if (!monitor.isCanceled()) {
                            listener.onSetDocument(document, edit, monitor);
                        }
                    } catch (Exception e) {
                        //must not fail
                        Log.log(e);
                    }
                }
            }
        };
        runIt(runnable);
    }

    /**
     * Notifies the available listeners that the input has changed for the editor.
     * 
     * @param oldInput the old input of the editor
     * @param input the new input of the editor
     */
    public void notifyInputChanged(final IEditorInput oldInput, final IEditorInput input) {
        final BaseEditor edit = pyEdit.get();
        if (edit == null) {
            return;
        }
        INotifierRunnable runnable = new INotifierRunnable() {
            public void run(IProgressMonitor monitor) {
                for (IPyEditListener listener : edit.getAllListeners()) {
                    if (listener instanceof IPyEditListener3) {
                        IPyEditListener3 pyEditListener3 = (IPyEditListener3) listener;
                        try {
                            if (!monitor.isCanceled()) {
                                pyEditListener3.onInputChanged(edit, oldInput, input, monitor);
                            }
                        } catch (Exception e) {
                            //must not fail
                            Log.log(e);
                        }
                    }
                }
            }
        };
        runIt(runnable);
    }

    public void notifyEditorCreated() {
        //Note that it's not done on a Job as in the other cases!
        final BaseEditor edit = pyEdit.get();
        if (edit == null) {
            return;
        }
        for (IPyEditListener listener : edit.getAllListeners(false)) {
            if (listener instanceof IPyEditListener4) {
                IPyEditListener4 pyEditListener4 = (IPyEditListener4) listener;
                try {
                    pyEditListener4.onEditorCreated(edit);
                } catch (Exception e) {
                    //must not fail
                    Log.log(e);
                }
            }
        }
    }

}
