/*
 * Created on Oct 7, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.PydevPrefs;

/**
 * This class is used to make the code coverage.
 * 
 * It works in this way: when the user requests the coverage for the execution
 * of a module, we create a python process and execute the module using the code
 * coverage module that is packed with pydev.
 * 
 * Other options are: - Erasing the results obtained; - Getting the results when
 * requested (cached in this class).
 * 
 * @author Fabio Zadrozny
 */
public class PyCoverage {

    /**
     * This method contacts the python server so that we get the information on
     * the files that are below the directory passed as a parameter
     * 
     * @param file2
     */
    public void refreshCoverageInfo(File file) {
        try {
            List pyFilesBelow = null;
            if (file.exists()) {
                pyFilesBelow = getPyFilesBelow(file);
            } else {
                pyFilesBelow = new ArrayList();
            }

            if(pyFilesBelow.size() == 0){
                return;
            }
            
            //now that we have the file information, we have to get the
            // coverage information on these files and
            //structure them so that we can get the coverage information in an
            // easy and hierarchical way.

            String profileScript = PythonRunnerConfig.getProfileScript();

            //we have to make a process to execute the script. it should look
            // like:
            //coverage.py -r [-m] FILE1 FILE2 ...
            //Report on the statement coverage for the given files. With the -m
            //option, show line numbers of the statements that weren't
            // executed.

            //python coverage.py -r -m files....

            String[] cmdLine = new String[4+ pyFilesBelow.size()];
            cmdLine[0] = PydevPrefs.getDefaultInterpreter();
            cmdLine[1] = profileScript;
            cmdLine[2] = "-r";
            cmdLine[3] = "-m";
            int i = 4;
            for (Iterator iter = pyFilesBelow.iterator(); iter.hasNext();) {
                cmdLine[i] = iter.next().toString();
                i++;
            }

            Process p=null;
            
            try {
                
                p = execute(cmdLine);
                //we have the process...
                int bufsize = 64; // small bufsize so that we can see the progress
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()), bufsize);
                BufferedReader eIn = new BufferedReader(new InputStreamReader(p.getErrorStream()), bufsize);


                p.getOutputStream().close();
                String str = "";
                while ((str = eIn.readLine()) != null) {
                    System.out.println("STDERR: " + str); //ignore this...
                }
                eIn.close();
                while ((str = in.readLine()) != null) {
                    System.out.println("STDOUT: " + str);//get the data...
                    StringTokenizer tokenizer = new StringTokenizer(str);
                    if(tokenizer.countTokens() ==5){

                        String []strings = new String[5];
                        int k = 0;
                        while(tokenizer.hasMoreElements()){
                            strings[k] = tokenizer.nextToken();
                            k++;
                        }
                        
                        if(strings[1].equals("Stmts") == false){
                            //information in the format: D:\dev_programs\test\test1.py      11      0     0%   1,2,4-23
                            System.out.println("VALID: " + str);//get the data...
                            
                        }
                    }
                }
                in.close();
                System.out.println("waiting");
                p.waitFor();
            } catch (Exception e) {
                if(p!=null){
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
     * @param cmdLine
     * @return
     * @throws IOException
     */
    private Process execute(String[] cmdLine) throws IOException {
        Process p;
        Properties properties = System.getProperties();
        Set set = properties.keySet();
        
        String []envp = new String [set.size()];
        int j = 0;
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            Object element = (Object) iter.next();
            envp[j] = element+"="+properties.getProperty(element.toString()).toString();
            j++;
        }
        envp = PyCoverage.setCoverageFileEnviromentVariable(envp);
        
        p = Runtime.getRuntime().exec(cmdLine, envp);
        return p;
    }



    /**
     * 
     * @param file
     */
    private List getPyFilesBelow(File file) {
        List filesToReturn = new ArrayList();

        if (file.exists() == true) {

            if (file.isDirectory()) {
                File[] files = file.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        return pathname.isDirectory() || pathname.toString().endsWith(".py");
                    }

                });
                for (int i = 0; i < files.length; i++) {
                    filesToReturn.addAll(getPyFilesBelow(files[i]));
                }
            } else if (file.isFile()) {
                filesToReturn.add(file);
            }
        }
        return filesToReturn;
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
        try {
            File pySrcPath = PydevDebugPlugin.getPySrcPath();
            return pySrcPath.getAbsolutePath() + "/.coverage";
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param envp
     * @return
     */
    public static String[] setCoverageFileEnviromentVariable(String[] envp) {
        boolean added = false;

        for (int i = 0; i < envp.length; i++) {

            if (envp[i].startsWith("COVERAGE_FILE")) {
                envp[i] = "COVERAGE_FILE=" + getCoverageFileLocation();
                added = true;
            }

        }
        if (!added) {
            List list = new ArrayList(Arrays.asList(envp));
            list.add("COVERAGE_FILE=" + getCoverageFileLocation());
            envp = (String[]) list.toArray(new String[0]);
        }
        return envp;
    }

    /**
     * 
     */
    public void clearInfo() {
        try {
            String profileScript;
            profileScript = PythonRunnerConfig.getProfileScript();
	        String[] cmdLine = new String[3];
	        cmdLine[0] = PydevPrefs.getDefaultInterpreter();
	        cmdLine[1] = profileScript;
	        cmdLine[2] = "-e";
	        Process p = execute(cmdLine);
	        p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
    }

}