/*
 * Created on 18/09/2005
 */
package com.python.pydev.analysis.builder;

import java.io.File;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.parser.ChangedParserInfoForObservers;
import org.python.pydev.core.parser.ErrorParserInfoForObservers;
import org.python.pydev.core.parser.IParserObserver;
import org.python.pydev.core.parser.IParserObserver3;
import org.python.pydev.core.parser.ISimpleNode;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;

/**
 * Observes changes to the parser and when OK, it'll ask for the analysis of the module reparsed.
 * 
 * @author Fabio
 */
public class AnalysisParserObserver implements IParserObserver, IParserObserver3{


    public static final String ANALYSIS_PARSER_OBSERVER_FORCE = "AnalysisParserObserver:force";

    @SuppressWarnings("unchecked")
    public void parserChanged(ChangedParserInfoForObservers info) {
        SimpleNode root = (SimpleNode) info.root;
        if(info.file == null){
            return;
        }
        IFile fileAdapter = null;
        if(info.file instanceof IFile){
            fileAdapter = (IFile) info.file;
        }
        
        if(fileAdapter == null){
            fileAdapter = (IFile) info.file.getAdapter(IFile.class);
            if(fileAdapter == null){
                return;
            }
        }
        boolean force = false;
        if(info.argsToReparse != null && info.argsToReparse.length > 0){
            if(info.argsToReparse[0] instanceof Tuple){
                Tuple t = (Tuple) info.argsToReparse[0];
                if (t.o1 instanceof String && t.o2 instanceof Boolean){
                    if (t.o1.equals(ANALYSIS_PARSER_OBSERVER_FORCE)){ 
                        //if this message is passed, it will decide whether we will force the analysis or not
                        force = (Boolean)t.o2;
                    }
                }
            }
        }

        int whenAnalyze = AnalysisPreferences.getAnalysisPreferences().getWhenAnalyze();
        if(whenAnalyze == IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE || force){
            
            //create the module
            IPythonNature nature = PythonNature.getPythonNature(fileAdapter);
            if(nature == null){
                return;
            }
            
            //don't analyze it if we're still not 'all set'
            if(!nature.isOkToUse()){
                return;
            }
            
            if(!nature.startRequests()){
                return;
            }
            try{
                if(!nature.isResourceInPythonpath(fileAdapter)){
                    AnalysisRunner.deleteMarkers(fileAdapter);
                    return; // we only analyze resources that are in the pythonpath
                }
    
                String file = fileAdapter.getRawLocation().toOSString();
                String moduleName = nature.resolveModule(fileAdapter);
                IModule module = AbstractModule.createModule(root, new File(file), moduleName);
                
                //visit it
                AnalysisBuilderVisitor visitor = new AnalysisBuilderVisitor();
                visitor.memo = new HashMap<String, Object>();
                visitor.memo.put(PyDevBuilderVisitor.IS_FULL_BUILD, false);
                visitor.memo.put(PyDevBuilderVisitor.DOCUMENT_TIME, info.documentTime);
                visitor.visitingWillStart(new NullProgressMonitor(), false, null);
                visitor.doVisitChangedResource(nature, fileAdapter, info.doc, null, module, new NullProgressMonitor(), force, 
                        AnalysisBuilderRunnable.ANALYSIS_CAUSE_PARSER, info.documentTime); 
                
                visitor.visitingEnded(new NullProgressMonitor());
            }catch(MisconfigurationException e){
                Log.log(e); //Not much we can do about it.
            }finally{
                nature.endRequests();
            }
        }
    }

    
    public void parserChanged(ISimpleNode root, IAdaptable resource, IDocument doc) {
        throw new RuntimeException("As it uses IParserObserver2, this interface should not be asked for.");
    }

    
    public void parserError(Throwable error, IAdaptable file, IDocument doc) {
        //ignore errors...
    }

    
    public void parserError(ErrorParserInfoForObservers info) {
        //ignore
    }

}
