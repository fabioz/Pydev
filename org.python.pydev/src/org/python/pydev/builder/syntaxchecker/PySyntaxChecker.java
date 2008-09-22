package org.python.pydev.builder.syntaxchecker;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Whenever a given resource is changed, a syntax check is done, updating errors related to the syntax.
 * 
 * @author Fabio
 */
public class PySyntaxChecker extends PyDevBuilderVisitor{

	@Override
	public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    	PythonNature nature = getPythonNature(resource);
    	if(nature == null){
    		return;
    	}

//    	System.out.println("PySyntaxChecker: visit resource:"+resource);
    	SourceModule mod = getSourceModule(resource, document, nature);
//    	System.out.println("PySyntaxChecker: end visit resource:"+resource);
//    	System.out.println("\n\n\n\n\n");
    	Throwable parseError = mod.parseError;
    	try {
    		PyParser.deleteErrorMarkers(resource);
    	} catch (CoreException e) {
    		PydevPlugin.log(e);
    	}
    	
		if(parseError != null){
			try {
				PyParser.createParserErrorMarkers(parseError, resource, document);
			} catch (Exception e) {
				PydevPlugin.log(e);
			}
		}
		
	}

	@Override
	public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
		// Nothing needs to be done in this case
	}

}
