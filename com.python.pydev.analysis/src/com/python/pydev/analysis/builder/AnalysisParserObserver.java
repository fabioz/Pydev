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
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.parser.IParserObserver;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;

public class AnalysisParserObserver implements IParserObserver{

    public void parserChanged(SimpleNode root, IAdaptable resource, IDocument doc) {
        if(resource == null){
            return;
        }
        
        IFile fileAdapter = (IFile) resource.getAdapter(IFile.class);
        if(fileAdapter == null){
            return;
        }
        

        if(AnalysisPreferences.getAnalysisPreferences().getWhenAnalyze() == IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE){
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
            visitor.visitingWillStart(new NullProgressMonitor());
            visitor.doVisitChangedResource(fileAdapter, doc, module, true, new NullProgressMonitor()); //also analyze dependencies
            visitor.visitingEnded(new NullProgressMonitor());
        }
    }

    public void parserError(Throwable error, IAdaptable file, IDocument doc) {
        //ignore errors...
    }

}
