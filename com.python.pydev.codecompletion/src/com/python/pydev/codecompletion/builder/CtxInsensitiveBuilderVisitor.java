/*
 * Created on 11/09/2005
 */
package com.python.pydev.codecompletion.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;

public class CtxInsensitiveBuilderVisitor extends PyDevBuilderVisitor {

    @Override
    public boolean visitChangedResource(IResource resource, IDocument document) {
        AbstractModule sourceModule = getSourceModule(resource, document);
        return false;
    }

    @Override
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        String moduleName = getModuleName(resource);
        return false;
    }



}
