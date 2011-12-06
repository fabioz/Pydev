/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.builder.pylint;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;

/**
 * @author Fabio
 *
 */
public class Pep8Visitor extends PyDevBuilderVisitor{

    @Override
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        try {

            //It's important that the interpreter is created in the Thread and not outside the thread (otherwise
            //it may be that the output ends up being shared, which is not what we want.)
            IPythonInterpreter interpreter = JythonPlugin.newPythonInterpreter(true, true);
            String file = StringUtils.replaceAllSlashes(resource.getLocation().toOSString());
            interpreter.exec(StringUtils.format(
            		"import sys\n" +
            		"sys.argv = ['', '%s']\n" +
            		"add_to_pythonpath = '%s'\n" +
            		"if add_to_pythonpath not in sys.path:\n" +
            		"    sys.path.append(add_to_pythonpath)\n" +
            		"\n" +
            		"import pep8\n" +
            		"options, args = pep8.process_options(['', '-r', '%s'])\n" +
            		"pep8.options = options\n" +
            		"pep8.input_file('%s')\n" +
            		"", 
            		StringUtils.replaceAllSlashes("W:/pep8"),
            		file,
            		file,
            		file
            ));

        } catch (Exception e) {
            Log.log(e);
        }

    }

    @Override
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    }

}
