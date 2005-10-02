/*
 * Created on 11/09/2005
 */
package com.python.pydev.analysis.additionalinfo.builder;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

public class CtxInsensitiveBuilderVisitor extends PyDevBuilderVisitor {

    @Override
    protected int getPriority() {
        return PRIORITY_MIN; //this will be the last gui to be visited (it will take care of saving the information we generate)
    }
    
    Set<AbstractAdditionalInterpreterInfo> visited;
    
    @Override
    public void visitingWillStart(IProgressMonitor monitor) {
        visited = new HashSet<AbstractAdditionalInterpreterInfo>();
    }
    
    @Override
    public void visitChangedResource(IResource resource, IDocument document) {
        AbstractModule sourceModule = getSourceModule(resource, document);
        PythonNature nature = getPythonNature(resource);

        AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        
        //info.removeInfoFromModule(sourceModule.getName()); -- does not remove info from the module because this should be already
        //done once it gets here (the AnalysisBuilder, that also makes dependency info should take care of this).
        
        if (sourceModule instanceof SourceModule) {
            SourceModule m = (SourceModule) sourceModule;
            info.addSourceModuleInfo(m, nature);
        }
        visited.add(info);
    }

    @Override
    public void visitRemovedResource(IResource resource, IDocument document) {
    }

    @Override
    public void visitingEnded(IProgressMonitor monitor) {
        //persist this info (the analysis builder that generates dependency info will not have to worry about it).
        for (AbstractAdditionalInterpreterInfo info : visited) {
            info.save();
        }
    }

}
