/*
 * Created on 18/09/2005
 */
package com.python.pydev.analysis.builder;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.python.parser.SimpleNode;
import org.python.pydev.builder.PyDevBuilder;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.parser.IParserObserver;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;

public class AnalysisParserObserver implements IParserObserver{

    public void parserChanged(SimpleNode root, IFile resource) {
        if(AnalysisPreferences.getAnalysisPreferences().getWhenAnalyze() == IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE){
            //create the module
            IFile f = (IFile) resource;
            String file = f.getRawLocation().toOSString();
            String moduleName = PythonNature.getModuleNameForResource(resource);
            AbstractModule module = AbstractModule.createModule(root, new File(file), moduleName);
            
            //visit it
            AnalysisBuilderVisitor visitor = new AnalysisBuilderVisitor();
            visitor.visitChangedResource(resource, PyDevBuilder.getDocFromResource(resource), module);
        }
    }

    public void parserError(Throwable error, IFile file) {
        //ignore errors...
    }

}
