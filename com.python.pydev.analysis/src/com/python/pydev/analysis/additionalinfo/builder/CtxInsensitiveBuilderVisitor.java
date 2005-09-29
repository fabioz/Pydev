/*
 * Created on 11/09/2005
 */
package com.python.pydev.analysis.additionalinfo.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

public class CtxInsensitiveBuilderVisitor extends PyDevBuilderVisitor {

    List<AbstractAdditionalInterpreterInfo> visited;
    
    @Override
    public void visitingWillStart() {
        visited = new ArrayList<AbstractAdditionalInterpreterInfo>();
    }
    
    @Override
    public boolean visitChangedResource(IResource resource, IDocument document) {
        AbstractModule sourceModule = getSourceModule(resource, document);
        PythonNature nature = getPythonNature(resource);

        AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        info.removeInfoFromModule(sourceModule.getName());
        if (sourceModule instanceof SourceModule) {
            SourceModule m = (SourceModule) sourceModule;
            info.addSourceModuleInfo(m, nature);
        }
        return false;
    }

    @Override
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        String moduleName = getModuleName(resource);
        PythonNature nature = getPythonNature(resource);
        
        AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        info.removeInfoFromModule(moduleName);
        return false;
    }

    @Override
    public void visitingEnded() {
        for (AbstractAdditionalInterpreterInfo info : visited) {
            info.save();
        }
    }

}
