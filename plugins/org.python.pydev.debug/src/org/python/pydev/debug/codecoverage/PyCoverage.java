/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 7, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.UniversalRunner;
import org.python.pydev.runners.UniversalRunner.AbstractRunner;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;
import org.python.pydev.utils.PyFileListing;
import org.python.pydev.utils.PyFileListing.PyFileInfo;

/**
 * This class is used to make the code coverage.
 * 
 * It works in this way: when the user requests the coverage for the execution of a module, we create a python process and execute the
 * module using the code coverage module that is packed with pydev.
 * 
 * Other options are: 
 * - Erasing the results obtained; 
 * - Getting the results when requested (cached in this class).
 * 
 * @author Fabio Zadrozny
 */
public class PyCoverage {

    public CoverageCache cache = new CoverageCache();

    /**
     * This method contacts the python server so that we get the information on the files that are below the directory passed as a parameter
     * and stores the information needed on the cache.
     * 
     * @param file
     *            should be the root folder from where we want cache info.
     */
    public void refreshCoverageInfo(IContainer container, IProgressMonitor monitor) {
        cache.clear();
        if (container == null) {
            return;
        }
        try {
            if (!container.exists()) {
                throw new RuntimeException("The directory passed: " + container + " no longer exists.");
            }

            File file = container.getLocation().toFile();
            PyFileListing pyFilesBelow = new PyFileListing();

            if (file.exists()) {
                pyFilesBelow = PyFileListing.getPyFilesBelow(file, monitor, true, false);
            }

            if (pyFilesBelow.getFoundPyFileInfos().size() == 0) { //no files
                return;
            }

            //add the folders to the cache
            boolean added = false;
            for (Iterator<File> it = pyFilesBelow.getFoundFolders().iterator(); it.hasNext();) {
                File f = it.next();
                if (!added) {
                    cache.addFolder(f);
                    added = true;
                } else {
                    cache.addFolder(f, f.getParentFile());
                }
            }

            PythonNature nature = PythonNature.getPythonNature(container);
            if (nature == null) {
                throw new RuntimeException("The directory passed: " + container
                        + " does not have an associated nature.");
            }
            AbstractRunner runner = UniversalRunner.getRunner(nature);

            //First, combine the results of the many runs we may have.
            Tuple<String, String> output = runner.runScriptAndGetOutput(PythonRunnerConfig.getCoverageScript(),
                    new String[] { "combine" }, getCoverageDirLocation(), monitor);

            if (output.o1 != null && output.o1.length() > 0) {
                Log.logInfo(output.o1);
            }
            if (output.o2 != null && output.o2.length() > 0) {
                if (output.o2.startsWith("Coverage.py warning:")) {
                    Log.logInfo(output.o2);

                } else {
                    Log.log(output.o2);
                }
            }

            //we have to make a process to execute the script. it should look
            // like:
            //coverage.py -r [-m] FILE1 FILE2 ...
            //Report on the statement coverage for the given files. With the -m
            //option, show line numbers of the statements that weren't
            // executed.

            //python coverage.py -r -m files....

            monitor.setTaskName("Starting shell to get info...");
            monitor.worked(1);
            Process p = null;

            try {
                //                Tuple<Process, String> tup = runner.createProcess(
                //                        PythonRunnerConfig.getCoverageScript(), new String[]{
                //                            "-r", "-m", "--include", ".*"}, getCoverageDirLocation(), monitor);
                Tuple<Process, String> tup = runner.createProcess(PythonRunnerConfig.getCoverageScript(),
                        new String[] { "--pydev-analyze" }, getCoverageDirLocation(), monitor);
                p = tup.o1;
                try {
                    p.exitValue();
                    throw new RuntimeException("Some error happened... the process could not be created.");
                } catch (Exception e) {
                    //that's ok
                }

                String files = "";

                for (Iterator<PyFileInfo> iter = pyFilesBelow.getFoundPyFileInfos().iterator(); iter.hasNext();) {
                    String fStr = iter.next().getFile().toString();
                    files += fStr + "|";
                }
                files += "\r";
                monitor.setTaskName("Writing to shell...");

                //No need to synchronize as we'll waitFor() the process before getting the contents.
                ThreadStreamReader inputStream = new ThreadStreamReader(p.getInputStream(), false);
                inputStream.start();
                ThreadStreamReader errorStream = new ThreadStreamReader(p.getErrorStream(), false);
                errorStream.start();

                monitor.worked(1);
                OutputStream outputStream = p.getOutputStream();
                outputStream.write(files.getBytes());
                outputStream.close();

                //We'll read something in the format below:
                //Name                                                                      Stmts   Miss  Cover   Missing
                //-------------------------------------------------------------------------------------------------------
                //D:\workspaces\temp\test_workspace\pytesting1\src\mod1\__init__                0      0   100%   
                //D:\workspaces\temp\test_workspace\pytesting1\src\mod1\a                      10      3    70%   4-6
                //D:\workspaces\temp\test_workspace\pytesting1\src\mod1\hello                   3      3     0%   1-4
                //D:\workspaces\temp\test_workspace\pytesting1\src\mod1\mod2\__init__           5      5     0%   2-8
                //D:\workspaces\temp\test_workspace\pytesting1\src\mod1\mod2\hello2            33     33     0%   1-43
                //-------------------------------------------------------------------------------------------------------
                //TOTAL                                                                        57     50    12% 

                monitor.setTaskName("Waiting for process to finish...");
                monitor.worked(1);

                while (true) {
                    try {
                        p.exitValue();
                        break; //process finished
                    } catch (IllegalThreadStateException e) {
                        //not finished
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                    monitor.worked(1);
                    if (monitor.isCanceled()) {
                        try {
                            p.destroy();
                        } catch (Exception e) {
                            Log.log(e);
                        }
                        break;
                    }
                }

                String stdOut = inputStream.getAndClearContents();
                String stdErr = errorStream.getAndClearContents().trim();
                if (stdErr.length() > 0) {
                    Log.log(stdErr);
                }

                monitor.setTaskName("Getting coverage info...(please wait, this could take a while)");
                monitor.worked(1);
                FastStringBuffer tempBuf = new FastStringBuffer();
                for (String str : StringUtils.splitInLines(stdOut)) {
                    analyzeReadLine(monitor, str.trim(), tempBuf);
                }

                monitor.setTaskName("Finished");
            } catch (Exception e) {
                if (p != null) {
                    p.destroy();
                }
                Log.log(e);
            }

        } catch (Exception e1) {
            Log.log(e1);
            throw new RuntimeException(e1);
        }
    }

    /**
     * @param monitor
     * @param str
     * @param tempBuf 
     */
    private void analyzeReadLine(IProgressMonitor monitor, String str, FastStringBuffer tempBuf) {
        //The line we're interested in is something as 
        //D:\workspaces\temp\test_workspace\pytesting1\src\mod1\a   10      3    70%   4-6, 18, 19
        //with the last part (missing) optional.

        boolean added = false;
        List<String> strings = StringUtils.split(str, ' ', 5);
        String[] dottedValidSourceFiles = FileTypesPreferencesPage.getDottedValidSourceFiles();

        File f = null;
        int nTokens = strings.size();
        if (nTokens == 5 || nTokens == 4) {

            try {
                if (!strings.get(1).equalsIgnoreCase("stmts") && !strings.get(0).equalsIgnoreCase("total")) {
                    //information in the format: D:\workspaces\temp\test_workspace\pytesting1\src\mod1\a   10      3    70%   4-6, 18
                    String fileStr = strings.get(0);
                    boolean found = false;
                    for (String ext : dottedValidSourceFiles) {
                        if (fileStr.endsWith(ext)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        //Add the extension and see if it matches
                        tempBuf.clear().append(fileStr);
                        for (String ext : dottedValidSourceFiles) {
                            f = new File(tempBuf.append(ext).toString());
                            if (f.exists()) {
                                found = true;
                                break;
                            }
                            tempBuf.deleteLastChars(ext.length());
                        }
                    }

                    if (!found) {
                        return;
                    }

                    int stmts = Integer.parseInt(strings.get(1));
                    int miss = Integer.parseInt(strings.get(2));
                    if (nTokens == 4) {
                        cache.addFile(f, f.getParentFile(), stmts, miss, "");
                        added = true;
                    } else {
                        String missing = strings.get(4);
                        cache.addFile(f, f.getParentFile(), stmts, miss, missing);
                        added = true;
                    }
                    String[] strs = f.toString().replaceAll("/", " ").replaceAll("\\\\", " ").split(" ");
                    if (strs.length > 1) {
                        monitor.setTaskName("Getting coverage info..." + strs[strs.length - 1]);
                    } else {
                        monitor.setTaskName("Getting coverage info..." + f.toString());
                    }
                    monitor.worked(1);
                }
            } catch (RuntimeException e2) {
                //maybe there is something similar, but isn't quite the same, so, parse int could give us some problems...
                Log.log(IStatus.INFO, "Code-coverage: ignored line: " + str, null);
            }
        }

        //we may have gotten an error in the following format:
        //X:\coilib30\python\coilib\geom\Box3D.py exceptions.IndentationError: unindent does not match any outer indentation level (line
        // 97)
        //X:\coilib30\python\coilib\x3d\layers\cacherenderer.py exceptions.SyntaxError: invalid syntax (line 95)
        //
        //that is: file errorClass desc.
        if (added == false && f != null) {
            try {
                if (f.exists() && f.isFile()) { //this is probably an error...
                    if (!f.getName().startsWith(".coverage")) {
                        //System.out.println("Adding file:"+f);
                        cache.addFile(f, f.getParentFile(), getError(strings));
                    }
                }

            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * @param strings
     * @return string concatenating all but first elements from passed argument 
     * separated by space
     */
    private String getError(List<String> strings) {
        StringBuffer ret = new StringBuffer();
        int len = strings.size();
        for (int i = 1; i < len; i++) {
            ret.append(strings.get(i)).append(' ');
        }
        return ret.toString();
    }

    /**
     *  
     */
    public void clearInfo() {
        cache.clear();
        File dir = getCoverageDirLocation();
        try {
            //Clear the files we created when running the coverages.
            FileUtils.clearTempFilesAt(dir, ".coverage.");
        } catch (Exception e) {
            Log.log(e);
        }
        try {
            //We also need to remove the file that consolidates all the info
            new File(dir, ".coverage").delete();
        } catch (Exception e) {
            Log.log(e);
        }

    }

    private static PyCoverage pyCoverage;

    /**
     * @return Returns the pyCoverage.
     */
    public static PyCoverage getPyCoverage() {
        if (pyCoverage == null) {
            pyCoverage = new PyCoverage();
        }
        return pyCoverage;
    }

    public static File getCoverageDirLocation() {
        IPath stateLocation = PydevDebugPlugin.getDefault().getStateLocation();
        stateLocation = stateLocation.append("coverage");
        String loc = FileUtils.getFileAbsolutePath(stateLocation.toFile());
        File dir = new File(loc);
        try {
            dir.mkdirs();
        } catch (Exception e) {
            Log.log(e);
        }
        if (!dir.exists()) {
            throw new RuntimeException("The directory: " + loc + " could not be created.");
        }
        if (!dir.isDirectory()) {
            throw new RuntimeException("Expected the path: " + loc + " to be a directory.");
        }
        return dir;
    }

    /**
     * @return
     */
    public static File getCoverageFileLocation() {
        return FileUtils.getTempFileAt(getCoverageDirLocation(), ".coverage.");
    }

}