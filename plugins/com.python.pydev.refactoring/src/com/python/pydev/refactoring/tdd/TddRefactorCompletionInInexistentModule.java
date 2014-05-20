/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * This is the proposal that goes outside. It only creates the proposal that'll actually do something later, as
 * creating that proposal may be slower.
 */
public final class TddRefactorCompletionInInexistentModule extends AbstractTddRefactorCompletion {

    private File module;
    private List<String> parametersAfterCall;
    private AbstractPyCreateAction pyCreateAction;
    private PySelection ps;

    public TddRefactorCompletionInInexistentModule(String replacementString, Image image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, PyEdit edit,
            File module, List<String> parametersAfterCall, AbstractPyCreateAction pyCreateAction, PySelection ps) {

        super(edit, replacementString, 0, 0, 0, image, displayString, contextInformation, additionalProposalInfo,
                priority);
        this.module = module;
        this.parametersAfterCall = parametersAfterCall;
        this.pyCreateAction = pyCreateAction;
        this.ps = ps;
    }

    @Override
    public void apply(IDocument document) {
        Log.log("This apply should not be called as it implements ICompletionProposalExtension2.");
    }

    @Override
    public boolean isAutoInsertable() {
        return false;
    }

    @Override
    public Point getSelection(IDocument document) {
        return null;
    }

    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        //Now, we need to go on and create the module
        List<File> parents = new ArrayList<File>();
        File f = module.getParentFile();
        while (f != null && !f.exists()) {
            parents.add(f);
            f = f.getParentFile();
        }
        IProject project = null;
        if (edit != null) {
            project = edit.getProject();
        }
        IContainer container = new PySourceLocatorBase().getContainerForLocation(Path.fromOSString(f
                .getAbsolutePath()), project);
        if (container == null) {
            return;
        }
        Collections.reverse(parents);
        int size = parents.size();
        for (int i = 0; i < size; i++) {
            File parent = parents.get(i);
            //create folder with __init__.
            IFolder folder = container.getFolder(new Path(parent.getName()));
            if (!folder.exists()) {
                try {
                    folder.create(true, true, null);
                } catch (CoreException e) {
                    Log.log(e);
                }
            }

            container = folder;
            IFile file = container.getFile(new Path("__init__"
                    + FileTypesPreferencesPage.getDefaultDottedPythonExtension()));
            if (!file.exists()) {
                try {
                    file.create(new ByteArrayInputStream(new byte[0]), true, null);
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
        }

        //Now that the package structure is created, create the actual module.
        IFile file = container.getFile(new Path(module.getName()));
        if (!file.exists()) {
            try {
                file.create(new ByteArrayInputStream(new byte[0]), true, null);
            } catch (CoreException e) {
                Log.log(e);
            }
        }

        //Upon creation, opens the new editor and creates the class.
        PyOpenAction openAction = new PyOpenAction();
        openAction.run(new ItemPointer(file));

        PyEdit pyEdit = (PyEdit) openAction.editor;
        TddRefactorCompletion completion = new TddRefactorCompletion(fReplacementString, fImage, fDisplayString,
                fContextInformation, fAdditionalProposalInfo, 0, pyEdit, PyCreateClass.LOCATION_STRATEGY_END,
                parametersAfterCall, pyCreateAction, ps);
        completion.apply(pyEdit.getEditorSourceViewer(), '\n', 0, 0);

        //As the change was done in another module, let's ask for a new code analysis for the current editor,
        //as the new contents should fix the marker which we used for the fix.
        forceReparseInBaseEditorAnd(pyEdit);
    }

    public void selected(ITextViewer viewer, boolean smartToggle) {
    }

    public void unselected(ITextViewer viewer) {
    }

    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        return false;
    }
}