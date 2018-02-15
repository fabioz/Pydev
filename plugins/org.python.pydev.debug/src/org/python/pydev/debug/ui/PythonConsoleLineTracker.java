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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
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
import org.python.pydev.ast.item_pointer.ItemPointer;
import org.python.pydev.ast.location.FindWorkspaceFiles;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.FileTypesPreferences;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.structure.Location;
import org.python.pydev.shared_core.utils.PlatformUtils;

/**
 * Line tracker that hyperlinks error lines: 'File "D:\mybad.py" line 3\n n Syntax error'
 *
 * see org.eclipse.ant.internal.ui.console.BuildFailedTracker
 */
public class PythonConsoleLineTracker implements IConsoleLineTracker {

    private ILinkContainer linkContainer; // console we are attached to
    private boolean onlyCreateLinksForExistingFiles = true;
    private IProject project;

    private void updateProjectAndWorkingDir() {
        IProcess process = DebugUITools.getCurrentProcess();
        if (process != null) {
            ILaunch launch = process.getLaunch();
            if (launch != null) {
                ILaunchConfiguration lc = launch.getLaunchConfiguration();
                initLaunchConfiguration(lc);
            }
        }
    }

    private IProject getProject() {
        IProject project = this.project;
        if (project == null) {
            updateProjectAndWorkingDir();
        }
        return project;
    }

    private IPath workingDirectory;

    public IPath getWorkingDirectory() {
        if (workingDirectory == null) {
            updateProjectAndWorkingDir();
        }
        return workingDirectory;
    }

    /** pattern for detecting error lines */
    static Pattern regularPythonlinePattern = Pattern.compile(".*(File) \\\"([^\\\"]*)\\\", line (\\d*).*");
    static Pattern insideQuotesMatcher1 = Pattern.compile(".*\\\"(.*)?\\\".*");
    static Pattern insideQuotesMatcher2 = Pattern.compile(".*\\'(.*)?\\'.*");

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
        try {
            int lineOffset = line.getOffset();
            int lineLength = line.getLength();
            String text;
            try {
                text = linkContainer.getContents(lineOffset, lineLength);
            } catch (BadLocationException e) {
                PydevDebugPlugin.log(IStatus.ERROR, "unexpected error", e);
                return;
            }
            if (text.contains("..\\..\\..\\etk\\coilib50\\source\\python\\coilib50")) {
                System.out.println("here");
            }

            Matcher m = regularPythonlinePattern.matcher(text);
            if (m.matches()) {
                regularPythonMatcher(lineOffset, lineLength, m);
                return;
            }

            if (quotesPattern(lineOffset, text, insideQuotesMatcher1)) {
                return;
            }

            if (quotesPattern(lineOffset, text, insideQuotesMatcher2)) {
                return;
            }

            // Ok, we did not have a direct match, let's try a different approach...
            String[] dottedValidSourceFiles = FileTypesPreferences.getDottedValidSourceFiles();
            for (String dottedExt : dottedValidSourceFiles) {
                Pattern pattern = getRegexpForExtension(dottedExt);
                m = pattern.matcher(text);
                if (m.matches()) {
                    int lineNumberInt = 0;
                    String filename = m.group(1);
                    int endCol = m.end(1);
                    if (text.length() > endCol) {
                        if (text.charAt(endCol) == ':') {
                            int j = 1;
                            while (endCol + j < text.length()) {
                                char c = text.charAt(endCol + j);
                                if (Character.isDigit(c)) {
                                    j++;
                                } else {
                                    break;
                                }
                            }
                            if (j > 1) {
                                String string = text.substring(endCol + 1, endCol + j);
                                lineNumberInt = Integer.parseInt(string);
                                endCol += j;
                            }
                        }
                    }
                    if (checkMapFilenameToHyperlink(lineOffset, 0, endCol, filename, lineNumberInt)) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Log.log(e);

        }
    }

    private boolean quotesPattern(int lineOffset, String text, Pattern quotesRegex) {
        Matcher m;
        m = quotesRegex.matcher(text);
        if (m.matches()) {
            String filename = m.group(1);
            int endCol = m.end(1);

            int lineNumberInt = 0;
            int matchStartCol = m.start(1);
            int colonI = filename.lastIndexOf(':');
            if (colonI != -1) {
                if (!(PlatformUtils.isWindowsPlatform() && colonI == 1)) {
                    String lineNumber = filename.substring(colonI + 1).trim();
                    if (lineNumber.length() > 0) {
                        try {
                            lineNumberInt = Integer.parseInt(lineNumber);
                            filename = filename.substring(0, colonI);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            }

            if (checkMapFilenameToHyperlink(lineOffset, matchStartCol, endCol, filename, lineNumberInt)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkMapFilenameToHyperlink(int lineOffset, int matchStartCol, int endCol, String filename,
            int lineNumberInt) {
        final String initialFilename = filename;
        if (new File(filename).exists()) {
            if (createHyperlink(lineOffset, matchStartCol, endCol, filename, lineNumberInt)) {
                return true;
            }
            return false;
        }

        if (PlatformUtils.isWindowsPlatform()) {
            // On windows an absolute file would start with the drive letter and a colon, so, check for that.
            int colonI = filename.lastIndexOf(':');
            if (colonI > 0) { // at 0 it'd not be ok (because we still need the drive letter).
                filename = filename.substring(colonI - 1);
                matchStartCol = matchStartCol + colonI - 1;
                if (new File(filename).exists()) {
                    if (createHyperlink(lineOffset, matchStartCol, endCol, filename, lineNumberInt)) {
                        return true;
                    }
                    return false;
                }
            }
        }
        // Not a direct match, let's try some heuristics to get a match based on the working dir.
        final IPath path = Path.fromOSString(filename);

        IProject project = getProject();
        try {
            if (project != null) {
                IFile file = project.getFile(path);
                if (file.exists()) {
                    FileLink link = new FileLink(file, null, -1, -1, lineNumberInt);
                    linkContainer.addLink(link, lineOffset + matchStartCol, endCol - matchStartCol);
                    return true;
                }
            }
        } catch (IllegalArgumentException e1) {
            // ignore
        } catch (Exception e1) {
            Log.log(e1);
        }

        PythonNature nature = PythonNature.getPythonNature(project);
        if (nature != null) {
            try {
                Set<IResource> projectSourcePathFolderSet = nature.getPythonPathNature()
                        .getProjectSourcePathFolderSet();
                for (IResource iResource : projectSourcePathFolderSet) {
                    if (iResource instanceof IContainer) {
                        IContainer iContainer = (IContainer) iResource;
                        try {
                            IFile file2 = iContainer.getFile(path);
                            if (file2.exists()) {
                                FileLink link = new FileLink(file2, null, -1, -1, lineNumberInt);
                                linkContainer.addLink(link, lineOffset + matchStartCol, endCol - matchStartCol);
                            }
                            IPath pathInDisk = iContainer.getLocation().append(path);
                            if (createHyperlink(lineOffset, matchStartCol, endCol, pathInDisk.toOSString(),
                                    lineNumberInt)) {
                                return true;
                            }
                        } catch (IllegalArgumentException e) {
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }
                }
            } catch (CoreException e) {
            }
        }

        try {
            List<IPath> lst = new ArrayList<>();
            lst.add(getWorkingDirectory());
            if (getProject() != null) {
                lst.add(getProject().getLocation());
            }

            for (IPath workingDirectory : lst) {
                if (workingDirectory != null) {
                    IPath pathCopy = (IPath) path.clone();
                    while (pathCopy.segmentCount() > 0) {
                        IPath appended = workingDirectory.append(pathCopy);
                        File checkFile = appended.toFile();
                        if (checkFile.exists()) {
                            if (createHyperlink(lineOffset,
                                    matchStartCol + (initialFilename.length() - pathCopy.toString().length()),
                                    endCol, checkFile.getAbsolutePath(), lineNumberInt)) {
                                return true;
                            }
                        }
                        pathCopy = pathCopy.removeFirstSegments(1);
                    }
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }

        return false;
    }

    private Map<String, Pattern> compiledPatterns = new HashMap<>();

    private Pattern getRegexpForExtension(String dottedExt) {
        Pattern pattern = compiledPatterns.get(dottedExt);
        if (pattern != null) {
            return pattern;
        }
        pattern = Pattern.compile("(.*" + Pattern.quote(dottedExt) + ")\\b.*");
        compiledPatterns.put(dottedExt, pattern);
        return pattern;
    }

    private boolean regularPythonMatcher(int lineOffset, int lineLength, Matcher m) {
        String lineNumber = null;
        int col = -1;
        final String fileName = m.group(2);
        lineNumber = m.group(3);
        col = m.start(1); // The beginning of the line, "File  "
        int lineNumberInt = 0;
        try {
            lineNumberInt = lineNumber != null ? Integer.parseInt(lineNumber) : 0;
        } catch (NumberFormatException e) {
        }

        return createHyperlink(lineOffset, col, lineLength, fileName, lineNumberInt);
    }

    /**
     * @return true if the hyperlink was created and false otherwise.
     */
    private boolean createHyperlink(int lineOffset, int startCol, int endCol, final String fileName,
            int lineNumberInt) {
        // hyperlink if we found something
        if (fileName != null) {
            IHyperlink link = null;
            IFile file;
            if (SharedCorePlugin.inTestMode()) {
                file = null;
            } else {
                file = getFileForLocation(fileName);
            }
            if (file != null && file.exists()) {
                link = new FileLink(file, null, -1, -1, lineNumberInt);
            } else {
                // files outside of the workspace
                File realFile = new File(fileName);
                if (!onlyCreateLinksForExistingFiles || realFile.exists()) {
                    ItemPointer p = new ItemPointer(realFile, new Location(lineNumberInt - 1, 0), null);
                    link = new ConsoleLink(p);
                }
            }
            if (link != null) {
                linkContainer.addLink(link, lineOffset + startCol, endCol - startCol);
                return true;
            }
        }
        return false;
    }

    private IFile getFileForLocation(String fileName) {
        IFile file;
        IProject project = getProject();
        file = FindWorkspaceFiles.getFileForLocation(Path.fromOSString(fileName), project);
        return file;
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
        int lastLen = (len - last) - 1;
        if (lastLen > 0) {
            this.lineAppended(new Region(last, lastLen));
        }
    }

}
