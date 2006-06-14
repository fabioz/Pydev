/*
 * Created on 18/09/2005
 */
package com.python.pydev.analysis.builder;

import java.io.File;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.parser.IParserObserver;
import org.python.pydev.parser.IParserObserver2;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;

public class AnalysisParserObserver implements IParserObserver, IParserObserver2{


	public static final String ANALYSIS_PARSER_OBSERVER_FORCE = "AnalysisParserObserver:force";

	public void parserChanged(SimpleNode root, IAdaptable resource, IDocument doc, Object... argsToReparse) {
		//don't analyze it if we're still not 'all set'
		if(!PydevPlugin.isPythonInterpreterInitialized() || !PydevPlugin.isJythonInterpreterInitialized()){
			return;
		}
		
        if(resource == null){
            return;
        }
        IFile fileAdapter = null;
        if(resource instanceof IFile){
        	fileAdapter = (IFile) resource;
        }
        
        if(fileAdapter == null){
	        fileAdapter = (IFile) resource.getAdapter(IFile.class);
	        if(fileAdapter == null){
	            return;
	        }
        }
        boolean force = false;
        if(argsToReparse != null && argsToReparse.length > 0){
        	if(argsToReparse[0] instanceof Tuple){
        		Tuple t = (Tuple) argsToReparse[0];
        		if (t.o1 instanceof String && t.o2 instanceof Boolean){
        			if (t.o1.equals(ANALYSIS_PARSER_OBSERVER_FORCE)){ //if this message is passed, it will decide whether we will force the analysis or not
        				force = (Boolean)t.o2;
        			}
        		}
        	}
        }

        if(AnalysisPreferences.getAnalysisPreferences().getWhenAnalyze() == IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE || force){
            //create the module
        	
        	if(!PythonNature.isResourceInPythonpath(fileAdapter)){
        		try {
					fileAdapter.deleteMarkers(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
				} catch (Exception e) {
					Log.log(e);
				}
            	return; // we only analyze resources that are in the pythonpath
            }

            String file = fileAdapter.getRawLocation().toOSString();
            String moduleName = PythonNature.getModuleNameForResource(fileAdapter);
            IModule module = AbstractModule.createModule(root, new File(file), moduleName);
            
            //visit it
            AnalysisBuilderVisitor visitor = new AnalysisBuilderVisitor();
            visitor.memo = new HashMap<String, Object>();
            visitor.visitingWillStart(new NullProgressMonitor(), false, null);
            visitor.doVisitChangedResource(fileAdapter, doc, module, true, new NullProgressMonitor()); //also analyze dependencies
            visitor.visitingEnded(new NullProgressMonitor());
        }
	}

    public void parserChanged(SimpleNode root, IAdaptable resource, IDocument doc) {
    }

    public void parserError(Throwable error, IAdaptable file, IDocument doc) {
        //ignore errors...
    }

	public void parserError(Throwable error, IAdaptable file, IDocument doc, Object... argsToReparse) {
		//ignore
	}

}
