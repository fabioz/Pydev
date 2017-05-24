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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.UniversalRunner;
import org.python.pydev.runners.UniversalRunner.AbstractRunner;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
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
    public void refreshCoverageInfo(IContainer container, IProgressMonitor monitor) throws CoverageException {
        int exitValue = 0;
        String stdOut = "";
        String stdErr = "";

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
                    new String[] { "combine", "--append" }, getCoverageDirLocation(), monitor);

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
            File coverageDirLocation = getCoverageDirLocation();
            File coverageXmlLocation = new File(coverageDirLocation, "coverage.xml");
            if (coverageXmlLocation.exists()) {
                coverageXmlLocation.delete();
            }
            monitor.worked(1);
            Process p = null;

            try {
                // Will create the coverage.xml file for us to process later on.
                Tuple<Process, String> tup = runner.createProcess(PythonRunnerConfig.getCoverageScript(),
                        new String[] { "--pydev-analyze" }, coverageDirLocation, monitor);
                p = tup.o1;
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

                monitor.setTaskName("Waiting for process to finish...");
                monitor.worked(1);

                while (true) {
                    try {
                        exitValue = p.exitValue();
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

                stdOut = inputStream.getAndClearContents().trim();
                stdErr = errorStream.getAndClearContents().trim();
                if (stdOut.length() > 0) {
                    Log.log(stdOut);
                }
                if (stdErr.length() > 0) {
                    Log.log(stdErr);
                }

                monitor.setTaskName("Getting coverage info...(please wait, this could take a while)");
                monitor.worked(1);

                if (!coverageXmlLocation.exists()) {
                    Log.log("Expected file: " + coverageXmlLocation + " to be written to analyze coverage info.");
                } else {
                    CoverageXmlInfo.analyze(cache, coverageXmlLocation);
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
        if (exitValue != 0) {
            FastStringBuffer buf = new FastStringBuffer("Error with coverage action (exit value: ",
                    stdErr.length() + stdOut.length() + 40);
            buf.append(exitValue).append(").");
            if (stdOut.length() > 0) {
                buf.append("\nStandard outputt:\n");
                buf.append(stdOut);
            }
            if (stdErr.length() > 0) {
                buf.append("\nError output:\n");
                buf.append(stdErr);
            }
            throw new CoverageException(buf.toString());
        }
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