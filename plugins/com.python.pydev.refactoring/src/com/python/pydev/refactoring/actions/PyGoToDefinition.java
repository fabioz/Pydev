/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 21, 2004
 *
 */
package com.python.pydev.refactoring.actions;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.codecompletion.PyCodeCompletionImages;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.parsing.IParserObserver;
import org.python.pydev.shared_ui.EditorUtils;

/**
 * This is a refactoring action, but it does not follow the default cycle -- so, it overrides the run
 * and always uses the same cycle... because in this case, we do not need any additional information
 * before starting the refactoring... the go to definition always only depends upon the current 
 * selected text -- and if more than 1 match is found, it asks the user to select the one that
 * is more likely the match)
 * 
 * @author Fabio Zadrozny
 */
public class PyGoToDefinition extends PyRefactorAction {

    /**
     * We do some additional checking because the default backend
     * @return true if the conditions are ok and false otherwise
     */
    protected boolean areRefactorPreconditionsOK(RefactoringRequest request) {

        // we're working with dirty editors now (through pushTemporaryModule/popTemporaryModule)
        //
        //        if (request.pyEdit.isDirty()){
        //            request.pyEdit.doSave(null);
        //        }

        return true;
    }

    /**
     * This class makes the parse when all reparses have finished.
     */
    private class FindParserObserver implements IParserObserver {

        /**
         * Lock for accessing askReparse.
         */
        private Object lock;

        /**
         * A set with all the reparses asked. When all finish, we'll do the find.
         */
        private Set<PyEdit> askReparse;

        /**
         * This is the editor which this action is listening in the reparse (will remove it from 
         * askReparse and when empty, will proceed to do the find).
         */
        private PyEdit editToReparse;

        public FindParserObserver(PyEdit editToReparse, Set<PyEdit> askReparse, Object lock) {
            this.editToReparse = editToReparse;
            this.askReparse = askReparse;
            this.lock = lock;
        }

        /**
         * As soon as the reparse is done, this method is called.
         */
        public void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc, long docModificationStamp) {
            editToReparse.getParser().removeParseListener(this); //we'll only listen for this single parse
            doFindIfLast();
        }

        /**
         * We want to work in the event of parse errors too.
         */
        public void parserError(Throwable error, IAdaptable file, IDocument doc) {
            editToReparse.getParser().removeParseListener(this); //we'll only listen for this single parse
            doFindIfLast();
        }

        /**
         * Remove the editor from askReparse and if it's the last one, do the find.
         */
        private void doFindIfLast() {
            synchronized (lock) {
                askReparse.remove(editToReparse);
                if (askReparse.size() > 0) {
                    return; //not the last one (we'll only do the find when all are reparsed.
                }
            }
            /**
             * Create an ui job to actually make the find.
             */
            UIJob job = new UIJob("Find") {

                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    try {
                        findDefinitionsAndOpen(true);
                    } catch (Throwable e) {
                        Log.log(e);
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setPriority(Job.INTERACTIVE);
            job.schedule();
        }
    }

    /**
     * Overrides the run and calls -- and the whole default refactoring cycle from the beggining, 
     * because unlike most refactoring operations, this one can work with dirty editors.
     * @return 
     */
    @Override
    public void run(IAction action) {
        workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        IEditorPart[] dirtyEditors = workbenchWindow.getActivePage().getDirtyEditors();
        Set<PyEdit> askReparse = new HashSet<PyEdit>();
        for (IEditorPart iEditorPart : dirtyEditors) {
            if (iEditorPart instanceof PyEdit) {
                PyEdit pyEdit = (PyEdit) iEditorPart;
                long astModificationTimeStamp = pyEdit.getAstModificationTimeStamp();
                IDocument doc = pyEdit.getDocument();
                if (astModificationTimeStamp != -1
                        && astModificationTimeStamp == (((IDocumentExtension4) doc).getModificationStamp())) {
                    //All OK, the ast is synched!
                } else {
                    askReparse.add(pyEdit);
                }
            }
        }

        if (askReparse.size() == 0) {
            findDefinitionsAndOpen(true);
        } else {
            //We don't have a match: ask for a reparse
            Object lock = new Object();
            for (PyEdit pyEdit : askReparse) {
                IParserObserver observer = new FindParserObserver(pyEdit, askReparse, lock);
                PyParser parser = pyEdit.getParser();
                parser.addParseListener(observer); //it will analyze when the next parse is finished
                parser.forceReparse();
            }
        }
    }

    public ItemPointer[] findDefinitionsAndOpen(boolean doOpenDefinition) {
        request = null;

        ps = new PySelection(getTextEditor());
        final PyEdit pyEdit = getPyEdit();
        RefactoringRequest refactoringRequest;
        try {
            refactoringRequest = getRefactoringRequest();
        } catch (MisconfigurationException e1) {
            Log.log(e1);
            return new ItemPointer[0];
        }

        final Shell shell = EditorUtils.getShell();
        try {

            if (areRefactorPreconditionsOK(refactoringRequest)) {
                ItemPointer[] defs = findDefinition(pyEdit);
                if (doOpenDefinition) {
                    openDefinition(defs, pyEdit, shell);
                }
                return defs;
            }
        } catch (Exception e) {
            Log.log(e);
            String msg = e.getMessage();
            if (msg == null) {
                msg = "Unable to get error msg (null)";
            }
            ErrorDialog.openError(shell, "Error", "Unable to do requested action",
                    new Status(Status.ERROR, PydevPlugin.getPluginID(), 0, msg, e));

        }
        return null;
    }

    /**
     * Opens a given definition directly or asks the user to choose one of the passed definitions
     * 
     * @param defs item pointers with the definitions available for opening.
     * @param pyEdit pyedit where the action to open the definition was started
     * @param shell the shell to be used to show dialogs 
     */
    public static void openDefinition(ItemPointer[] defs, final PyEdit pyEdit, final Shell shell) {
        if (defs == null) {
            shell.getDisplay().beep();
            return;
        }

        HashSet<ItemPointer> set = new HashSet<ItemPointer>();
        for (ItemPointer pointer : defs) {
            if (pointer.file != null) {
                set.add(pointer);
            }
        }
        final ItemPointer[] where = set.toArray(new ItemPointer[0]);

        if (where == null) {
            shell.getDisplay().beep();
            return;
        }

        if (where.length > 0) {
            if (where.length == 1) {
                ItemPointer itemPointer = where[0];
                doOpen(itemPointer, pyEdit, shell);
            } else {
                //the user has to choose which is the correct definition...
                final Display disp = shell.getDisplay();
                disp.syncExec(new Runnable() {

                    public void run() {
                        ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new ILabelProvider() {

                            public Image getImage(Object element) {
                                return PyCodeCompletionImages.getImageForType(IToken.TYPE_PACKAGE);
                            }

                            public String getText(Object element) {
                                ItemPointer pointer = (ItemPointer) element;
                                File f = (File) (pointer).file;
                                int line = pointer.start.line;
                                return f.getName() + "  (" + f.getParent() + ") - line:" + line;
                            }

                            public void addListener(ILabelProviderListener listener) {
                            }

                            public void dispose() {
                            }

                            public boolean isLabelProperty(Object element, String property) {
                                return false;
                            }

                            public void removeListener(ILabelProviderListener listener) {
                            }
                        }) {
                            @Override
                            protected Control createContents(Composite parent) {
                                Control ret = super.createContents(parent);
                                org.python.pydev.plugin.PydevPlugin
                                        .setCssId(parent, "py-go-to-definition-dialog", true);
                                return ret;
                            }

                            @Override
                            public boolean isHelpAvailable() {
                                return false;
                            }

                            @Override
                            protected void updateStatus(IStatus status) {
                                super.updateStatus(status);
                                PydevPlugin.fixSelectionStatusDialogStatusLineColor(this, this.getDialogArea()
                                        .getBackground());
                            }

                        };
                        dialog.setTitle("Found matches");
                        dialog.setTitle("Select the one you believe matches most your search.");
                        dialog.setElements(where);
                        dialog.open();
                        Object[] result = dialog.getResult();
                        if (result != null && result.length > 0) {
                            doOpen((ItemPointer) result[0], pyEdit, shell);

                        }
                    }

                });
            }
        } else {
            shell.getDisplay().beep();
        }
    }

    /**
     * @param itemPointer this is the item pointer that gives the location that should be opened
     * @param pyEdit the editor (so that we can gen the open action)
     * @param shell 
     */
    private static void doOpen(ItemPointer itemPointer, PyEdit pyEdit, Shell shell) {
        new PyOpenAction().run(itemPointer, pyEdit.getProject(), pyEdit.getSite());
    }

    /**
     * @return an array of ItemPointer with the definitions found
     * @throws MisconfigurationException 
     * @throws TooManyMatchesException 
     * @throws BadLocationException 
     */
    public ItemPointer[] findDefinition(PyEdit pyEdit)
            throws TooManyMatchesException, MisconfigurationException, BadLocationException {
        IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
        return pyRefactoring.findDefinition(getRefactoringRequest());
    }

    /**
     * As we're not using the default refactoring cycle, this method is not even called
     */
    @Override
    protected String perform(IAction action, IProgressMonitor monitor) throws Exception {
        return null;
    }

}