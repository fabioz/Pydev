/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Copyright (c) 2013 by Syapse, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.container;

import java.io.ByteArrayInputStream;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * Abstraction that allos either formatting or organize of imports.
 * @author Fabio, Jeremy J. Carroll
 *
 */
abstract class PyContainerFormatterAction extends PyContainerAction {

    private final String verbPassed;
    private final String verbPresent;
    private final String verbPresentMsg;

    /**
     * This class is customized by implementing the {@link #createFormatter()}
     * message and providing a few strings for message in the constructor.
     * @param present     The activity in present tense (e.g. 'format' 'organize imports')
     * @param presentMsg  The activity in present tense with an object e.g. 'a file' after (e.g. 'format' 'organize imports in')
     * @param passed      The activity in passed tense (e.g. 'formatted' 'organized')
     */
    PyContainerFormatterAction(String present, String presentMsg, String passed) {
        this.verbPassed = passed;
        this.verbPresent = present;
        this.verbPresentMsg = presentMsg;
    }

    /**
     * This is used for doing the source formatting 
     * (only valid after beforeRun() and before afterRun()).
     */
    protected IFormatter formatter;
    /**
     * Set with the open files (only valid after beforeRun() and before afterRun())
     */
    protected Set<IFile> openFiles;

    abstract IFormatter createFormatter();

    /**
     * Initialize the open files and the formatter to be used.
     */
    @Override
    protected void beforeRun() {
        openFiles = PyAction.getOpenFiles();
        formatter = createFormatter();
    }

    @Override
    protected void afterRun(int formatted) {
        openFiles = null;
        formatter = null;
        MessageDialog.openInformation(null, "Files " + verbPassed, StringUtils.format("%s %s files.",
                StringUtils.getWithFirstUpper(verbPassed),
                formatted));
    }

    @Override
    protected boolean confirmRun() {
        return MessageDialog
                .openConfirm(
                        null,
                        "Confirm " + verbPresent,
                        "Are you sure that you want to recursively "
                                + verbPresentMsg
                                + " the python files from the selected folder(s)?\n"
                                + "\n"
                                + "It'll be applied to all the file-types specified in the preferences: PyDev > Editor > Code Style > File types.\n"
                                + "\n" + "This action cannot be undone.");
    }

    /**
     * Applies source code formatting to the files... 
     * Recursively pass the folders and delete the files (and sum them so that we know how many
     * files were formatted).
     * 
     * @param container the folder from where we want to remove the files
     * @return the number of files formatted
     */
    @Override
    protected int doActionOnContainer(IContainer container, IProgressMonitor monitor) {
        int formatted = 0;
        try {
            IResource[] members = container.members();

            for (final IResource c : members) {
                if (monitor.isCanceled()) {
                    break;
                }
                monitor.worked(1);
                if (c instanceof IContainer) {
                    formatted += this.doActionOnContainer((IContainer) c, monitor);

                } else if (c instanceof IFile) {
                    final String name = c.getName();
                    if (name != null) {
                        monitor.setTaskName("Formatting: " + name);
                        if (PythonPathHelper.isValidSourceFile(name)) {
                            IFile file = (IFile) c;
                            final IDocument doc = FileUtilsFileBuffer.getDocFromResource(c);

                            final boolean isOpenedFile = openFiles.contains(file);
                            try {
                                if (isOpenedFile) {
                                    RunInUiThread.async(new Runnable() {

                                        public void run() {
                                            try {
                                                formatter.formatAll(doc, null, (IFile) c, isOpenedFile, true);
                                            } catch (SyntaxErrorException e) {
                                                Log.log(IStatus.ERROR, "Could not " + verbPresentMsg + " file: " + name
                                                        + " (invalid syntax).", e);
                                            }
                                        }
                                    });
                                } else {
                                    formatter.formatAll(doc, null, (IFile) c, isOpenedFile, true);
                                }
                            } catch (SyntaxErrorException e) {
                                Log.log(IStatus.ERROR, "Could not " + verbPresentMsg + " file: " + name
                                        + " (invalid syntax).",
                                        e);
                            }
                            formatted += 1;
                            if (isOpenedFile) {
                                //This means that it's an open buffer (let the user save it when he wants).
                                continue;
                            }
                            file.setContents(new ByteArrayInputStream(doc.get().getBytes()), true, true, monitor);
                        }
                    }
                }
            }
        } catch (CoreException e) {
            Log.log(e);
        }

        return formatted;
    }

}
