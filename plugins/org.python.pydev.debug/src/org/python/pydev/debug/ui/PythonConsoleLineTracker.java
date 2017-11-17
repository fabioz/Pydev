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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
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
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.plugin.nature.PythonNature;
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
    private IProject project;

    private IProject getProject() {
        IProject project = this.project;
        if (project == null) {
            IProcess process = DebugUITools.getCurrentProcess();
            if (process != null) {
                ILaunchConfiguration lc = process.getLaunch().getLaunchConfiguration();
                try {
                    project = lc.getMappedResources()[0].getProject();
                } catch (NullPointerException e) {
                    //Ignore if we don't have lc or mapped resources.
                } catch (CoreException e) {
                    Log.log("Error accessing launched resources.", e);
                }
            }
        }
        return project;
    }

    private IPath workingDirectory;

    /** pattern for detecting error lines */
    static Pattern regularPythonlinePattern = Pattern.compile(".*(File) \\\"([^\\\"]*)\\\", line (\\d*).*");

    /**
     * Opens up a file with a given line
     */
    public class ConsoleLink implements IHyperlink {

        ItemPointer pointer;

        public ConsoleLink(ItemPointer pointer) {
            this.pointer = pointer;
        }

        @Override
        public void linkEntered() {

        }

        @Override
        public void linkExited() {

        }

        @Override
        public void linkActivated() {
            PyOpenAction open = new PyOpenAction();
            open.run(pointer);
        }
    }

    @Override
    public void init(final IConsole console) {
        IProcess process = console.getProcess();
        if (process != null) {
            ILaunch launch = process.getLaunch();
            if (launch != null) {
                initLaunchConfiguration(launch.getLaunchConfiguration());
            }
        }
        this.linkContainer = new ILinkContainer() {

            @Override
            public void addLink(IHyperlink link, int offset, int length) {
                console.addLink(link, offset, length);
            }

            @Override
            public String getContents(int offset, int length) throws BadLocationException {
                return console.getDocument().get(offset, length);
            }
        };
    }

    public void init(ILaunchConfiguration launchConfiguration, ILinkContainer linkContainer) {
        this.linkContainer = linkContainer;
        initLaunchConfiguration(launchConfiguration);
    }

    private void initLaunchConfiguration(ILaunchConfiguration launchConfiguration) {
        if (launchConfiguration != null) {
            IResource[] mappedResources;
            try {
                mappedResources = launchConfiguration.getMappedResources();
                if (mappedResources != null && mappedResources.length > 0) {
                    this.project = mappedResources[0].getProject();
                    this.workingDirectory = PythonRunnerConfig.getWorkingDirectory(launchConfiguration,
                            PythonNature.getPythonNature(project));
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * Hyperlink error lines to the editor.
     *
     * Based on org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
     */
    @Override
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

        Matcher m = regularPythonlinePattern.matcher(text);
        // match
        if (m.matches()) {
            regularPythonMatcher(lineOffset, lineLength, m);
        }
    }

    private void regularPythonMatcher(int lineOffset, int lineLength, Matcher m) {
        String fileName = null;
        String lineNumber = null;
        int fileStart = -1;
        fileName = m.group(2);
        lineNumber = m.group(3);
        fileStart = m.start(1); // The beginning of the line, "File  "
        // hyperlink if we found something
        if (fileName != null) {
            IHyperlink link = null;
            int num = -1;
            try {
                num = lineNumber != null ? Integer.parseInt(lineNumber) : 0;
            } catch (NumberFormatException e) {
                num = 0;
            }
            IFile file;
            if (SharedCorePlugin.inTestMode()) {
                file = null;
            } else {
                IProject project = getProject();
                file = new PySourceLocatorBase().getFileForLocation(Path.fromOSString(fileName), project);

            }
            if (file != null && file.exists()) {
                link = new FileLink(file, null, -1, -1, num);
            } else {
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
    @Override
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
