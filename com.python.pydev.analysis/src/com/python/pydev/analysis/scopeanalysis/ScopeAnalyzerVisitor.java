/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.scopeanalysis;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

import com.python.pydev.analysis.visitors.Found;
import com.python.pydev.analysis.visitors.ScopeItems;

/**
 * This class is used to discover the occurrences of some token having its scope as something important.
 */
public class ScopeAnalyzerVisitor extends AbstractScopeAnalyzerVisitor{

    public ScopeAnalyzerVisitor(IPythonNature nature, String moduleName, IModule current,  
            IDocument document, IProgressMonitor monitor) {
        super(nature, moduleName, current, document, monitor);
        
    }

    @Override
    protected void onAddUndefinedMessage(IToken token) {
    }

    @Override
    protected void onAddUndefinedVarInImportMessage(IToken foundTok) {
    }

    @Override
    protected void afterEndScope(boolean reportUnused, ScopeItems m) {
    }

    @Override
    protected void onLastScope(ScopeItems m) {
    }

    @Override
    public void onAddUnusedMessage(Found found) {
    }

    @Override
    public void onAddReimportMessage(Found newFound) {
    }

    @Override
    public void onAddUnresolvedImport(IToken token) {
    }

    @Override
    public void onAddDuplicatedSignature(SourceToken token, String name) {
    }

    @Override
    public void onAddNoSelf(SourceToken token, Object[] objects) {
    }
}
