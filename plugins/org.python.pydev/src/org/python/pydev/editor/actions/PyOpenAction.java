/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 12, 2004
 */
package org.python.pydev.editor.actions;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractJavaClassModule;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Location;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * Opens an editor and selects text in it.
 * 
 * Inspired by org.eclipse.jdt.ui.actions.OpenAction, but simplifies all handling in a single class.
 */
public class PyOpenAction extends Action {

    public IEditorPart editor;

    public PyOpenAction() {
    }

    public static void showInEditor(ITextEditor textEdit, Location start, Location end) {
        EditorUtils.showInEditor(textEdit, start, end);
    }

    public void run(ItemPointer p, IProject project) {
        run(p, project, null);
    }

    public void run(ItemPointer p, IProject project, IWorkbenchPartSite site) {
        editor = null;
        Object file = p.file;
        String zipFilePath = p.zipFilePath;
        Definition definition = p.definition;

        if (file instanceof File) {
            File f = (File) file;
            String filename = f.getName();
            if (PythonPathHelper.isValidSourceFile(filename) || filename.indexOf('.') == -1 || //treating files without any extension! 
                    (zipFilePath != null && PythonPathHelper.isValidSourceFile(zipFilePath))) {

                //Keep on going as we were going...

            } else if (definition instanceof JavaDefinition) {
                if (site == null) {
                    site = EditorUtils.getSite();
                }
                if (site == null) {
                    Log.log("Unable to open JavaDefinition because we have no active site.");
                }

                //note that it will only be able to find a java definition if JDT is actually available
                //so, we don't have to care about JDTNotAvailableExceptions here. 
                JavaDefinition javaDefinition = (JavaDefinition) definition;
                OpenAction openAction = new OpenAction(site);
                StructuredSelection selection = new StructuredSelection(new Object[] { javaDefinition.javaElement });
                openAction.run(selection);
            } else {
                String message;
                boolean giveError = true;

                if (definition != null && definition.module instanceof AbstractJavaClassModule) {
                    AbstractJavaClassModule module = (AbstractJavaClassModule) definition.module;
                    message = "The definition was found at: " + f.toString() + "\n" + "as the java module: "
                            + module.getName();

                } else {
                    if (FileTypesPreferencesPage.isValidDll(filename)) {
                        if (f.exists()) {
                            //It's a pyd or dll, let's check if it was a cython module to open it...
                            File parentFile = f.getParentFile();
                            File newFile = new File(parentFile, StringUtils.stripExtension(f.getName()) + "." + "pyx");

                            if (!newFile.exists()) {
                                newFile = new File(parentFile, StringUtils.stripExtension(f.getName()) + "." + "pxd");
                            }
                            if (!newFile.exists()) {
                                newFile = new File(parentFile, StringUtils.stripExtension(f.getName()) + "." + "pxi");
                            }

                            if (newFile.exists()) {
                                giveError = false;
                                file = newFile;
                            }
                        }
                    }

                    message = "The definition was found at: " + f.toString() + "\n"
                            + "(which cannot be opened because it is a compiled extension)";

                }

                if (giveError) {
                    MessageDialog.openInformation(EditorUtils.getShell(), "Compiled Extension file", message);
                    return;
                }
            }
        }

        if (zipFilePath != null) {
            //currently, only open zip file 
            editor = PyOpenEditor.doOpenEditor((File) file, zipFilePath);

        } else if (file instanceof IFile) {
            IFile f = (IFile) file;
            editor = PyOpenEditor.doOpenEditor(f);

        } else if (file instanceof IPath) {
            IPath path = (IPath) file;
            editor = PyOpenEditor.doOpenEditor(path, project);

        } else if (file instanceof File) {
            String absPath = FileUtils.getFileAbsolutePath((File) file);
            IPath path = Path.fromOSString(absPath);
            editor = PyOpenEditor.doOpenEditor(path, project);
        }

        if (editor instanceof ITextEditor && p.start.line >= 0) {
            EditorUtils.showInEditor((ITextEditor) editor, p.start, p.end);
        }
    }

    public void run(ItemPointer p) {
        run(p, null);
    }
}
