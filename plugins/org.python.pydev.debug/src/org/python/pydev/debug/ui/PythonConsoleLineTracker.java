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
import java.util.HashMap;
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
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Location;
import org.python.pydev.shared_core.structure.Tuple;
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
    private boolean updatedProjectAndWorkingDir = false;

    private void updateProjectAndWorkingDir() {
        if (updatedProjectAndWorkingDir) {
            return;
        }
        IProcess process = DebugUITools.getCurrentProcess();
        if (process != null) {
            ILaunch launch = process.getLaunch();
            if (launch != null) {
                updatedProjectAndWorkingDir = true;
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

    /**
     * @return may be null.
     */
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
                if (length <= 0) {
                    // Log.log("Trying to create link with invalid len: " + length);
                    return;
                }
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
                    final PythonNature nature = PythonNature.getPythonNature(project);
                    if (nature != null) {
                        this.workingDirectory = PythonRunnerConfig.getWorkingDirectory(launchConfiguration,
                                nature);
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    // private long totalTime = 0;

    /**
     * Hyperlink error lines to the editor.
     *
     * Based on org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
     */
    @Override
    public void lineAppended(IRegion line) {
        // long initialTime = System.currentTimeMillis();
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

            Matcher m = regularPythonlinePattern.matcher(text);
            if (m.matches()) {
                try {
                    regularPythonMatcher(lineOffset, lineLength, m);
                } catch (Exception e) {
                    Log.log(e);
                }
                return;
            }

            // Ok, did not find with the regular match, let's try with a different approach.
            forEachStringToCheck(text, FileTypesPreferences.getDottedValidSourceFiles(),
                    (matchStartCol, matchEndCol, textFound, lineNumberAtText) -> {
                        checkMapFilenameToHyperlink(lineOffset, matchStartCol, matchEndCol, textFound,
                                lineNumberAtText);
                    });

        } catch (Exception e) {
            Log.log(e);

        }
        // long diff = System.currentTimeMillis() - initialTime;
        // totalTime += diff;
        // System.out.println("TotalTime: " + totalTime / 1000.0 + "s ");
    }

    public static interface OnStringToCheck {
        public void onString(int matchStartCol, int matchEndCol, String text, int lineNumberAtText);
    }

    public static void forEachStringToCheck(final String text, final String[] dottedFileExtensions,
            final OnStringToCheck onString) {
        final int length = text.length();

        final FastStringBuffer tempBuf = new FastStringBuffer();

        boolean foundDot = false;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                tempBuf.append(c);

            } else if (c == '\\' || c == '/') {
                tempBuf.append(c);

            } else if (c == '.') {
                foundDot = true;
                tempBuf.append(c);

            } else {
                if (c == ':') {
                    // Colon can be a drive only for windows.
                    if (PlatformUtils.isWindowsPlatform()) {
                        if (tempBuf.length() == 1) {
                            if (Character.isLetter(tempBuf.charAt(0))) {
                                tempBuf.append(c);
                                continue;
                            }
                        }
                    }
                }
                // Ok, found separator.
                if (!tempBuf.isEmpty()) {
                    if (foundDot) {
                        i = analyzeText(text, dottedFileExtensions, onString, length, tempBuf, i, c);
                    }
                    tempBuf.clear();
                }
                foundDot = false;
            }
        }

        if (foundDot && !tempBuf.isEmpty()) {
            char c = '\0';
            int i = length;
            analyzeText(text, dottedFileExtensions, onString, length, tempBuf, i, c);
        }
    }

    private static int analyzeText(String text, String[] dottedFileExtensions, OnStringToCheck onString, int length,
            FastStringBuffer tempBuf, int i, char c) {
        String string = tempBuf.toString();
        for (String fileExtension : dottedFileExtensions) {
            if (string.endsWith(fileExtension)) {
                int lineNumberAtText = 0;
                int foundColumn = i - string.length();
                if (c == ':') {
                    Tuple<Integer, String> extractedLineNumber = extractLineNumber(i + 1, text, length,
                            tempBuf.clear());
                    if (extractedLineNumber != null) {
                        i = extractedLineNumber.o1; // new offset.
                        lineNumberAtText = Integer.parseInt(extractedLineNumber.o2);
                    }
                }

                onString.onString(foundColumn, i, string, lineNumberAtText);
            }
        }
        return i;
    }

    private static Tuple<Integer, String> extractLineNumber(int i, String text, int length, FastStringBuffer tempBuf) {
        char c;
        for (; i < length; i++) {
            c = text.charAt(i);
            if (Character.isDigit(c)) {
                tempBuf.append(c);
            } else {
                break;
            }
        }
        if (tempBuf.length() > 0) {
            return new Tuple<Integer, String>(i, tempBuf.toString());
        }
        return null;
    }

    private final Map<Object, Boolean> cacheFileExists = new HashMap<Object, Boolean>();
    private final static long CACHE_TIMEOUT_IN_MILLIS = 1500;
    private long lastTimeCacheFileExistsCalled = 0;

    private boolean internalFileExists(Object file, ICallback<Boolean, Object> existsCallback) {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastTimeCacheFileExistsCalled > CACHE_TIMEOUT_IN_MILLIS) {
            lastTimeCacheFileExistsCalled = currentTimeMillis;
            cacheFileExists.clear();
        }
        Boolean saved = cacheFileExists.get(file);
        if (saved != null) {
            // System.out.println("Cached exists: " + saved + " - " + file);
            return saved;
        }

        boolean exists = existsCallback.call(file);
        cacheFileExists.put(file, exists);
        // System.out.println("Check: " + exists + " - " + file);
        return exists;

    }

    protected boolean fileExists(File file) {
        return internalFileExists(file, (f) -> {
            return file.exists();
        });
    }

    protected boolean fileExists(IFile file) {
        return internalFileExists(file, (f) -> {
            return file.exists();
        });
    }

    private boolean checkMapFilenameToHyperlink(final int lineOffset, final int matchStartCol, final int matchEndCol,
            final String filename,
            final int lineNumberInt) {

        File realFile = new File(filename);
        if (fileExists(realFile)) {
            // Simple case (absolute file match).
            createLinkToFile(lineOffset, matchStartCol, matchEndCol, lineNumberInt, realFile);
            return true;
        }

        // Not a direct match, let's try some heuristics to get a match based on the working dir.
        final IPath path = Path.fromOSString(filename);
        if (path.isAbsolute()) {
            return false; // Not much we can't do if the path is already absolute.
        }

        IProject project = getProject();
        try {
            if (project != null) {
                if (createLinkFromContainerAndPath(lineOffset, matchStartCol, matchEndCol, lineNumberInt, path,
                        project)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException e1) {
            // ignore
        } catch (Exception e1) {
            Log.log(e1);
        }

        IPath workingDir = getWorkingDirectory();
        if (workingDir != null) {
            if (!workingDir.equals(project.getLocation())) {
                IPath full = workingDir.append(path);
                File file = full.toFile();
                if (fileExists(file)) {
                    createLinkToFile(lineOffset, matchStartCol, matchEndCol, lineNumberInt, file);
                    return true;
                }
            }
        }

        PythonNature nature = PythonNature.getPythonNature(project);
        if (nature != null) {
            try {
                Set<IResource> projectSourcePathFolderSet = nature.getPythonPathNature()
                        .getProjectSourcePathFolderSet();
                for (IResource iResource : projectSourcePathFolderSet) {
                    if (iResource instanceof IContainer) {
                        IContainer iContainer = (IContainer) iResource;
                        if (iContainer.equals(project)) {
                            continue; // We already checked this.
                        }
                        if (workingDir != null && iContainer.getLocation().equals(workingDir)) {
                            continue; // We already checked this.
                        }
                        if (createLinkFromContainerAndPath(lineOffset, matchStartCol, matchEndCol, lineNumberInt, path,
                                iContainer)) {
                            return true;
                        }
                    }
                }
            } catch (CoreException e) {
            }
        }

        return false;
    }

    private boolean createLinkToFile(final int lineOffset, final int matchStartCol, final int matchEndCol,
            final int lineNumberInt, File realFile) {
        ItemPointer p = new ItemPointer(realFile, new Location(lineNumberInt - 1, 0), null);
        ConsoleLink link = new ConsoleLink(p);
        linkContainer.addLink(link, lineOffset + matchStartCol, matchEndCol - matchStartCol);
        return true;
    }

    private boolean createLinkFromContainerAndPath(final int lineOffset, final int matchStartCol, final int matchEndCol,
            final int lineNumberInt, final IPath path, IContainer iContainer) {
        try {
            IFile file2 = iContainer.getFile(path);
            if (fileExists(file2)) {
                FileLink link = new FileLink(file2, null, -1, -1, lineNumberInt);
                linkContainer.addLink(link, lineOffset + matchStartCol, matchEndCol - matchStartCol);
                return true;
            }
        } catch (IllegalArgumentException e) {
            // ignore
        } catch (Exception e) {
            Log.log(e);
        }
        return false;
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
        if (fileName != null) {
            IFile file = null;

            if (!SharedCorePlugin.inTestMode()) {
                try {
                    file = FindWorkspaceFiles.getFileForLocation(Path.fromOSString(fileName), getProject());
                } catch (Exception e) {
                    // Related project could be closed (go forward with external file).
                    Log.log(e);
                }
            }

            if (file != null && fileExists(file)) {
                IHyperlink link = new FileLink(file, null, -1, -1, lineNumberInt);
                linkContainer.addLink(link, lineOffset + startCol, endCol - startCol);
                return true;

            } else {
                // File outside of the workspace.
                File realFile = new File(fileName);
                if (!onlyCreateLinksForExistingFiles || fileExists(realFile)) {
                    ItemPointer p = new ItemPointer(realFile, new Location(lineNumberInt - 1, 0), null);
                    IHyperlink link = new ConsoleLink(p);
                    linkContainer.addLink(link, lineOffset + startCol, endCol - startCol);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * NOOP.
     * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
     */
    @Override
    public void dispose() {
        cacheFileExists.clear();
    }

    public void setOnlyCreateLinksForExistingFiles(boolean b) {
        this.onlyCreateLinksForExistingFiles = b;
    }

    public void splitInLinesAndAppendToLineTracker(final String string) {
        final int len = string.length();
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
        int lastLen = (len - last);
        if (lastLen > 0) {
            this.lineAppended(new Region(last, lastLen));
        }
    }

}
