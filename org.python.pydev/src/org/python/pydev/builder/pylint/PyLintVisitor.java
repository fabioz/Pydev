/*
 * License: Common Public License v1.0
 * Created on Oct 25, 2004
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pylint;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.builder.PydevMarkerUtils;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.SimplePythonRunner;

/**
 * 
 * Check lint.py for options.
 * 
 * @author Fabio Zadrozny
 */
public class PyLintVisitor extends PyDevBuilderVisitor {

    /* (non-Javadoc)
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitResource(org.eclipse.core.resources.IResource)
     */
    public static final String PYLINT_PROBLEM_MARKER = "org.python.pydev.pylintproblemmarker";

    public static List pyLintThreads = new ArrayList();
    
    /**
     * This class runs as a thread to get the markers, and only stops the IDE when the markers are being added.
     * 
     * @author Fabio Zadrozny
     */
    public static class PyLintThread extends Thread{
        
        IResource resource; 
        IDocument document; 
        IPath location;

        List markers = new ArrayList();
        
        public PyLintThread(IResource resource, IDocument document, IPath location){
            this.resource = resource;
            this.document = document;
            this.location = location;
        }
        
        /**
         * @return
         */
        private boolean canPassPyLint() {
            if(pyLintThreads.size() < 4){
                pyLintThreads.add(this);
                return true;
            }
            return false;
        }

        /**
         * @see java.lang.Thread#run()
         */
        public void run() {
            try {
                if(canPassPyLint()){
	                passPyLint(resource);
	                
	                new Job("Adding markers"){
	                
	                    protected IStatus run(IProgressMonitor monitor) {
	                        try {
	                            resource.deleteMarkers(PYLINT_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
	                        } catch (CoreException e3) {
	                            PydevPlugin.log(e3);
	                        }
	
	                        for (Iterator iter = markers.iterator(); iter.hasNext();) {
	                            Object[] el = (Object[]) iter.next();
	                            
	                            String tok   = (String) el[0];
	                            String type  = (String) el[1];
	                            int priority = ((Integer)el[2]).intValue();
	                            String id    = (String) el[3];
	                            int line     = ((Integer)el[4]).intValue();
	        		            PydevMarkerUtils.createMarker(resource, "ID:"+id+" "+tok , line,  type, priority);
	                        }
	
	                        return PydevPlugin.makeStatus(Status.OK, "", null);
	                    }
	                }.schedule();
                }
                
            } catch (final CoreException e) {
                new Job("Error reporting"){
                    protected IStatus run(IProgressMonitor monitor) {
                        PydevPlugin.log(e);
                        return PydevPlugin.makeStatus(Status.OK, "", null);
                    }
                }.schedule();
            }finally{
                try {
                    pyLintThreads.remove(this);
                } catch (Exception e) {
                    PydevPlugin.log(e);
                }
            }
        }


        /**
         * @param tok
         * @param type
         * @param priority
         * @param id
         * @param line
         */
        private void addToMarkers(String tok, String type, int priority, String id, int line) {
            markers.add(new Object[]{tok, type, new Integer(priority), id, new Integer(line)} );
        }
        
        /**
         * @param resource
         * @param document
         * @param location
         * @throws CoreException
         */
        private void passPyLint(IResource resource) throws CoreException {
            File script = new File(PyLintPrefPage.getPyLintLocation());
            File arg = new File(location.toOSString());

            ArrayList<String> list = new ArrayList<String>();
            list.add("--include-ids=y");
            
            //user args
            String userArgs = PyLintPrefPage.getPylintArgs().replaceAll("\r","").replaceAll("\n"," ");
            StringTokenizer tokenizer2 = new StringTokenizer(userArgs);
            while(tokenizer2.hasMoreTokens()){
                list.add(tokenizer2.nextToken());
            }
            list.add(REF.getFileAbsolutePath(arg));
            
            
            IProject project = resource.getProject();
            
            String output = new SimplePythonRunner().runAndGetOutput(REF.getFileAbsolutePath(script), list.toArray(new String[0]), script.getParentFile(), project);

            StringTokenizer tokenizer = new StringTokenizer(output, "\r\n");
            
            boolean useW = PyLintPrefPage.useWarnings();
            boolean useE = PyLintPrefPage.useErrors();
            boolean useF = PyLintPrefPage.useFatal();
            boolean useC = PyLintPrefPage.useCodingStandard();
            boolean useR = PyLintPrefPage.useRefactorTips();
            
            //System.out.println(output);
            if(output.indexOf("Traceback (most recent call last):") != -1){
                PydevPlugin.log(new RuntimeException("PyLint ERROR: \n"+output));
                return;
            }
            while(tokenizer.hasMoreTokens()){
                String tok = tokenizer.nextToken();
                
                try {
                    String type = null;
                    int priority = 0;
                    
                    //W0611:  3: Unused import finalize
                    //F0001:  0: Unable to load module test.test2 (list index out of range)
                    //C0321: 25:fdfd: More than one statement on a single line
                    int indexOfDoublePoints = tok.indexOf(":");
                    if(indexOfDoublePoints != -1){
                        
	                    if(tok.startsWith("C")&& useC){
	                        type = PYLINT_PROBLEM_MARKER;
	                        priority = IMarker.SEVERITY_WARNING;
	                    }
	                    else if(tok.startsWith("R")  && useR ){
	                        type = PYLINT_PROBLEM_MARKER;
	                        priority = IMarker.SEVERITY_WARNING;
	                    }
	                    else if(tok.startsWith("W")  && useW ){
	                        type = PYLINT_PROBLEM_MARKER;
	                        priority = IMarker.SEVERITY_WARNING;
	                    }
	                    else if(tok.startsWith("E") && useE ){
	                        type = PYLINT_PROBLEM_MARKER;
	                        priority = IMarker.SEVERITY_ERROR;
	                    }
	                    else if(tok.startsWith("F") && useF ){
	                        type = PYLINT_PROBLEM_MARKER;
	                        priority = IMarker.SEVERITY_ERROR;
	                    }else{
	                        continue;
	                    }
	                    
                    }else{
                        continue;
                    }
                    
                    try {
                        if(type != null){
                            String id = tok.substring(0, tok.indexOf(":")).trim();
                            
                            int i = tok.indexOf(":");
                            if(i == -1)
                                continue;
                            
                            tok = tok.substring(i+1);

                            i = tok.indexOf(":");
                            if(i == -1)
                                continue;
                            
                            int line = Integer.parseInt(tok.substring(0, i).trim() );
                            
                            IRegion region = null;
                            try {
                                region = document.getLineInformation(line - 1);
                            } catch (Exception e) {
                                region = document.getLineInformation(line);
                            }
                            String lineContents = document.get(region.getOffset(), region.getLength());
                            
                            int pos = -1;
                            if( ( pos = lineContents.indexOf("IGNORE:") ) != -1){
                                String lintW = lineContents.substring(pos+"IGNORE:".length());
                                if (lintW.startsWith(id)){
                                    continue;
                                }
                            }
                            
                            i = tok.indexOf(":");
                            if(i == -1)
                                continue;

                            tok = tok.substring(i+1);
                            addToMarkers(tok, type, priority, id, line);
                        }
                    } catch (RuntimeException e2) {
                        PydevPlugin.log(e2);
                    }
                } catch (Exception e1) {
                    PydevPlugin.log(e1);
                }
            }
        }


    }
    
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        
        if(PyLintPrefPage.usePyLint() == false){
            try {
                resource.deleteMarkers(PYLINT_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
            } catch (CoreException e3) {
                PydevPlugin.log(e3);
            }
            return;
        }
        
        IProject project = resource.getProject();
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        try {
            //pylint can only be used for jython projects
            if (!pythonNature.isPython()) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        if (project != null && resource instanceof IFile) {

            IFile file = (IFile) resource;
            IPath location = PydevPlugin.getLocation(file.getFullPath(), project);
            if(location != null){
                PyLintThread thread = new PyLintThread(resource, document, location);
                thread.start();
            }
        }
    }
    
    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#maxResourcesToVisit()
     */
    public int maxResourcesToVisit() {
        int i = PyLintPrefPage.getMaxPyLintDelta();
        if (i < 0){
            i = 0;
        }
        return i;
    }
}
