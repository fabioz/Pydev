/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pylint;

import java.io.File;
import java.util.StringTokenizer;

import org.eclipse.core.internal.resources.Marker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.SimplePythonRunner;

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
    
    public boolean visitResource(IResource resource, IDocument document) {
        
        if(PyLintPrefPage.usePyLint() == false){
            try {
                resource.deleteMarkers(PYLINT_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
            } catch (CoreException e3) {
                PydevPlugin.log(e3);
            }
            return true;
        }
        
        IProject project = resource.getProject();
        if (project != null && resource instanceof IFile) {

            IFile file = (IFile) resource;
            IPath location = PydevPlugin.getLocation(file.getFullPath());
            
            File script;
            try {
                script = PydevPlugin.getScriptWithinPySrc("ThirdParty/logilab/pylint/lint.py");
	            File arg = new File(location.toOSString());

	            String lintargs = " --include-ids=y ";
	            lintargs += PyLintPrefPage.getPylintArgs().replaceAll("\r","").replaceAll("\n"," ");
	            lintargs += " ";
	            
	            String output = SimplePythonRunner.runAndGetOutput(script.getAbsolutePath(), lintargs+arg.getAbsolutePath(), script.getParentFile());

	            StringTokenizer tokenizer = new StringTokenizer(output, "\r\n");
	            
	            boolean useW = PyLintPrefPage.useWarnings();
	            boolean useE = PyLintPrefPage.useErrors();
	            boolean useF = PyLintPrefPage.useFatal();
	            boolean useC = PyLintPrefPage.useCodingStandard();
	            boolean useR = PyLintPrefPage.useRefactorTips();
	            
	            try {
	                resource.deleteMarkers(PYLINT_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
	            } catch (CoreException e3) {
	                PydevPlugin.log(e3);
	            }
	            
	            while(tokenizer.hasMoreTokens()){
	                String tok = tokenizer.nextToken();
	                
	                try {
                        String type = null;
                        int priority = 0;
                        
                        //W0611:  3: Unused import finalize
                        //F0001:  0: Unable to load module test.test2 (list index out of range)
                        //C0321: 25:fdfd: More than one statement on a single line
                        if(tok.startsWith("C")&& useC && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_WARNING;
                        }
                        else if(tok.startsWith("R")  && useR && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_WARNING;
                        }
                        else if(tok.startsWith("W")  && useW && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_WARNING;
                        }
                        else if(tok.startsWith("E") && useE && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_ERROR;
                        }
                        else if(tok.startsWith("F") && useF && tok.indexOf(":") != -1){
                            type = PYLINT_PROBLEM_MARKER;
                            priority = Marker.SEVERITY_ERROR;
                        }
                        
                        String initial = tok;
                        try {
                            if(type != null){
                                String id = tok.substring(0, tok.indexOf(":")).trim();
                                
                                tok = tok.substring(tok.indexOf(":")+1);
                                int line = Integer.parseInt(tok.substring(0, tok.indexOf(":")).trim() );
                                
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
                                
                                tok = tok.substring(tok.indexOf(":")+1);
                                createMarker(resource, "ID:"+id+" "+tok , line,  type, priority);
                            }
                        } catch (RuntimeException e2) {
                            e2.printStackTrace();
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
	            }
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
        }
        return true;
    }
    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        return false;
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
