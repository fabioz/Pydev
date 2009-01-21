/*
 * Created on Oct 7, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.REF;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.utils.PyFileListing;
import org.python.pydev.utils.PyFileListing.PyFileInfo;

/**
 * This class is used to make the code coverage.
 * 
 * It works in this way: when the user requests the coverage for the execution of a module, we create a python process and execute the
 * module using the code coverage module that is packed with pydev.
 * 
 * Other options are: - Erasing the results obtained; - Getting the results when requested (cached in this class).
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
    public void refreshCoverageInfo(File file, IProgressMonitor monitor) {
        cache.clear();
        if (file == null) {
            return;
        }
        try {
            if (!file.isDirectory()) {
                throw new RuntimeException("We can only get information on a dir.");
            }

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

            //now that we have the file information, we have to get the
            // coverage information on these files and
            //structure them so that we can get the coverage information in an
            // easy and hierarchical way.

            String profileScript = PythonRunnerConfig.getCoverageScript();

            //we have to make a process to execute the script. it should look
            // like:
            //coverage.py -r [-m] FILE1 FILE2 ...
            //Report on the statement coverage for the given files. With the -m
            //option, show line numbers of the statements that weren't
            // executed.

            //python coverage.py -r -m files....

            String[] cmdLine = new String[4];
            cmdLine[0] = PydevPlugin.getPythonInterpreterManager().getDefaultInterpreter();
            cmdLine[1] = profileScript;
            cmdLine[2] = getCoverageFileLocation();
            cmdLine[3] = "-waitfor";

            monitor.setTaskName("Starting shell to get info...");
            monitor.worked(1);
            Process p = null;

            try {

                p = execute(cmdLine);
                try {
                    p.exitValue();
                    throw new RuntimeException("Some error happened... the process could not be created.");
                } catch (Exception e) {
                    //that's ok
                }
                //we have the process...
                int bufsize = 32; // small bufsize so that we can see the progress
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()), bufsize);

                String files = "";

                for (Iterator<PyFileInfo> iter = pyFilesBelow.getFoundPyFileInfos().iterator(); iter.hasNext();) {
                    String fStr = iter.next().getFile().toString();
                    files += fStr + "|";
                }
                files += "\r";
                monitor.setTaskName("Writing to shell...");

                monitor.worked(1);
                p.getOutputStream().write(files.getBytes());

                p.getOutputStream().close();
                String str = "";
                monitor.setTaskName("Getting coverage info...(please wait, this could take a while)");
                monitor.worked(1);
                while ((str = in.readLine()) != null) {
                    //System.out.println("Analyzing line:"+str);
                    analyzeReadLine(monitor, str);
                }
                in.close();
                monitor.setTaskName("Waiting for process to finish...");
                monitor.worked(1);
                p.waitFor();
                monitor.setTaskName("Finished");
            } catch (Exception e) {
                if (p != null) {
                    p.destroy();
                }
                e.printStackTrace();
            }

        } catch (Exception e1) {
            e1.printStackTrace();
            throw new RuntimeException(e1);
        }
    }

    /**
     * @param monitor
     * @param str
     */
    private void analyzeReadLine(IProgressMonitor monitor, String str) {
//        System.out.println("read line "+ str);
        boolean added = false;
        StringTokenizer tokenizer = new StringTokenizer(str, "@");
        int nTokens = tokenizer.countTokens();
        String[] strings = new String[nTokens];

        int k = 0;
        while (tokenizer.hasMoreElements()) {
            strings[k] = tokenizer.nextToken().trim();
            k++;
        }
        if (nTokens == 5 || nTokens == 4) {

            try {
                if (strings[1].equals("Stmts") == false && strings[0].equals("TOTAL") == false) {
                    //information in the format: D:\dev_programs\test\test1.py @ 11 @ 0 @ 0% @ 1,2,4-23
                    File f = new File(strings[0]);
                    if (nTokens == 4) {
                        cache.addFile(f, f.getParentFile(), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]), "");
                        added = true;
                    } else {
                        cache.addFile(f, f.getParentFile(), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]), strings[4]);
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
                e2.printStackTrace();
            }
        }

        //we may have gotten an error in the following format:
        //X:\coilib30\python\coilib\geom\Box3D.py exceptions.IndentationError: unindent does not match any outer indentation level (line
        // 97)
        //X:\coilib30\python\coilib\x3d\layers\cacherenderer.py exceptions.SyntaxError: invalid syntax (line 95)
        //
        //that is: file errorClass desc.
        if (added == false) {
            try {
                File f = new File(strings[0]);
                if(f.exists() && f.isFile()){ //this is probably an error...
                    if(!f.getName().equals(".coverage")){
                        //System.out.println("Adding file:"+f);
                        cache.addFile(f, f.getParentFile(), getError(strings));
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param strings
     * @return string concatenating all but first elements from passed argument 
     * separated by space
     */
    private String getError(String[] strings) {
        StringBuffer ret = new StringBuffer();
        for (int i = 1; i < strings.length; i++) {
            ret.append(strings[i]).append(' ');
        }
        return ret.toString();
    }

    /**
     *  
     */
    public void clearInfo() {
        try {
            String profileScript;
            profileScript = PythonRunnerConfig.getCoverageScript();
            String[] cmdLine = new String[4];
            cmdLine[0] = PydevPlugin.getPythonInterpreterManager().getDefaultInterpreter();
            cmdLine[1] = profileScript;
            cmdLine[2] = getCoverageFileLocation();
            cmdLine[3] = "-e";
            Process p = execute(cmdLine);
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    /**
     * @param cmdLine
     * @return
     * @throws IOException
     */
    private Process execute(String[] cmdLine) throws IOException {
        return SimpleRunner.createProcess(cmdLine, null);
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

    public static String getCoverageFileLocation() {
        IPath stateLocation = PydevDebugPlugin.getDefault().getStateLocation();
        stateLocation = stateLocation.append(".coverage");
        return REF.getFileAbsolutePath(stateLocation.toFile());
    }

}