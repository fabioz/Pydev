/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.builder.pylint;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.runners.SimplePythonRunner;

/**
 * @author Fabio
 *
 */
public class Pep8Visitor extends PyDevBuilderVisitor{

    @Override
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        IProject project = resource.getProject();
        PythonNature nature = PythonNature.getPythonNature(project);
        try {

            File fileToExec = new File("w:/pep8/pep8.py");
            HashMap<String, Object> locals = new HashMap<String, Object>();
            locals.put("__name__", "__main__");
            
            //It's important that the interpreter is created in the Thread and not outside the thread (otherwise
            //it may be that the output ends up being shared, which is not what we want.)
            IPythonInterpreter interpreter = JythonPlugin.newPythonInterpreter(false, false);

            interpreter.setErr(System.err);
            interpreter.setOut(System.out);
            Throwable e = JythonPlugin.exec(
                    locals, 
                    interpreter, 
                    fileToExec, 
//                    new File[]{fileToExec.getParentFile()} //pythonpath
                    new File[]{} //pythonpath
                    );
            if(e != null){
                Log.log(e);
            }

            
//            SimpleJythonRunner runner = new SimpleJythonRunner();
//            
//            Tuple<String, String> outTup = runner.runAndGetOutputWithJar(
//                    "D:/bin/Python265/Lib/site-packages/pep8-0.6.1-py2.6.egg/pep8.py", "D:/bin/jython-2.1/jython.jar", 
//                    new String[]{resource.getLocation().toOSString()}, resource.getParent().getLocation().toFile(), project, monitor);
//            
//            System.out.println(outTup);
        } catch (Exception e) {
            Log.log(e);
        }

    }

    @Override
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    }

}
