/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author atotic
 * Created: Jan 2, 2004
 */
package org.python.pydev.debug.ui;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.console.IHyperlink;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.structure.Location;

/**
 * Line tracker that hyperlinks error lines: 'File "D:\mybad.py" line 3\n n Syntax error'
 *
 * see org.eclipse.ant.internal.ui.console.BuildFailedTracker
 */
public class PythonConsoleLineTracker implements IConsoleLineTracker {

    private ILinkContainer linkContainer; // console we are attached to
    private boolean onlyCreateLinksForExistingFiles = true;

    /** pattern for detecting error lines */
    static Pattern linePattern = Pattern.compile(".*(File) \\\"([^\\\"]*)\\\", line (\\d*).*");

    /**
     * Opens up a file with a given line
     */
    public class ConsoleLink implements IHyperlink {

        ItemPointer pointer;

        public ConsoleLink(ItemPointer pointer) {
            this.pointer = pointer;
        }

        public void linkEntered() {

        }

        public void linkExited() {

        }

        public void linkActivated() {
            PyOpenAction open = new PyOpenAction();
            open.run(pointer);
        }
    }

    public void init(final IConsole console) {
        this.linkContainer = new ILinkContainer() {

            public void addLink(IHyperlink link, int offset, int length) {
                console.addLink(link, offset, length);
            }

            public String getContents(int offset, int length) throws BadLocationException {
                return console.getDocument().get(offset, length);
            }
        };
    }

    public void init(ILinkContainer linkContainer) {
        this.linkContainer = linkContainer;
    }

    /**
     * Hyperlink error lines to the editor.
     * 
     * Based on org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
     */
    public void lineAppended(IRegion line) {
        int lineOffset = line.getOffset();
        int lineLength = line.getLength();
        String text;
        try {
            text = linkContainer.getContents(lineOffset, lineLength);
        } catch (BadLocationException e) {
            PydevDebugPlugin.log(IStatus.ERROR, "unexpected error", e);
            return;
        }

        Matcher m = linePattern.matcher(text);
        String fileName = null;
        String lineNumber = null;
        int fileStart = -1;
        // match
        if (m.matches()) {
            fileName = m.group(2);
            lineNumber = m.group(3);
            fileStart = m.start(1); // The beginning of the line, "File  "
        }
        // hyperlink if we found something
        if (fileName != null) {
            IHyperlink link = null;
            int num = -1;
            try {
                num = lineNumber != null ? Integer.parseInt(lineNumber) : 0;
            } catch (NumberFormatException e) {
                num = 0;
            }
            IFile[] files;
            if (SharedCorePlugin.inTestMode()) {
                files = null;
            } else {
                files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(new File(fileName).toURI());

            }
            if (files != null && files.length > 0 && files[0].exists()) {
                if (files.length == 1) {
                    link = new FileLink(files[0], null, -1, -1, num);
                }
                else {
                    // In case of a linked file, try to open the file from the same project that was just launched
                    IFile file = null;
                    IProject project = null;
                    IProcess process = DebugUITools.getCurrentProcess();
                    if (process != null) {
                        ILaunchConfiguration lc = process.getLaunch().getLaunchConfiguration();
                        try {
                            project = lc.getMappedResources()[0].getProject();
                        } catch (CoreException e) {
                            Log.log("Error accessing launched resources.", e);
                        }
                    }
                    //check for file in current & referenced projects, and select it
                    if (project != null && project.exists()) {
                        IProject[] refProjects;
                        try {
                            refProjects = project.getDescription().getReferencedProjects();
                        } catch (CoreException e) {
                            Log.log("Error accessing referenced projects.", e);
                            refProjects = new IProject[0];
                        }
                        int i = -1;
                        do {
                            IProject searchProject = (i == -1 ? project : refProjects[i]);
                            for (IFile afile : files) {
                                if (afile.getProject().equals(searchProject)) {
                                    file = afile;
                                    i = refProjects.length; //to break out of parent loop
                                    break;
                                }
                            }
                        } while (++i < refProjects.length);
                    }
                    if (file == null) {
                        file = files[0];
                    }
                    link = new FileLink(file, null, -1, -1, num);
                }
            }
            else {
                // files outside of the workspace
                File realFile = new File(fileName);
                if (!onlyCreateLinksForExistingFiles || realFile.exists()) {
                    ItemPointer p = new ItemPointer(realFile, new Location(num - 1, 0), null);
                    link = new ConsoleLink(p);
                }
            }
            if (link != null) {
                linkContainer.addLink(link, lineOffset + fileStart, lineLength - fileStart);
            }
        }
    }

    /**
     * NOOP.
     * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
     */
    public void dispose() {
    }

    public void setOnlyCreateLinksForExistingFiles(boolean b) {
        this.onlyCreateLinksForExistingFiles = b;
    }

    public void splitInLinesAndAppendToLineTracker(String string) {
        int len = string.length();
        int last = 0;
        char c;
        for (int i = 0; i < len; i++) {
            c = string.charAt(i);

            if (c == '\r') {
                this.lineAppended(new Region(last, (i - last) - 1));
                if (i < len - 1 && string.charAt(i + 1) == '\n') {
                    i++;
                }
                last = i + 1;
            }
            if (c == '\n') {
                this.lineAppended(new Region(last, (i - last) - 1));
                last = i + 1;
            }
        }
        this.lineAppended(new Region(last, len - last));
    }

}
