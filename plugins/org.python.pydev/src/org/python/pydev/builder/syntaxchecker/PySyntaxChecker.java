/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.builder.syntaxchecker;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback0;

/**
 * Whenever a given resource is changed, a syntax check is done, updating errors related to the syntax.
 * 
 * @author Fabio
 */
public class PySyntaxChecker extends PyDevBuilderVisitor {

    @Override
    public void visitChangedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        PythonNature nature = getPythonNature(resource);
        if (nature == null) {
            return;
        }

        if (PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()) {
            if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                Log.toLogFile(this, "PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()");
            }
            return; //not analyzed with this builder... always from parser changes.
        }

        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
            Log.toLogFile(this, "Checking!");
        }

        IDocument doc = document.call();
        if (doc == null) {
            return;
        }
        SourceModule mod;
        try {
            mod = getSourceModule(resource, doc, nature);
        } catch (MisconfigurationException e1) {
            Log.log(e1);
            return;
        }
        Throwable parseError = mod.parseError;

        try {
            PyParser.deleteErrorMarkers(resource);
        } catch (CoreException e) {
            Log.log(e);
        }

        if (parseError != null) {
            PyParser.createParserErrorMarkers(parseError, resource, doc);
        }

    }

    @Override
    public void visitRemovedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        // Nothing needs to be done in this case
    }

}
