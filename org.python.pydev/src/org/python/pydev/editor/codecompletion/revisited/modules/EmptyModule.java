/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;

import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

/**
 * @author Fabio Zadrozny
 */
public class EmptyModule extends AbstractModule {

    public File f;

    /**
     * @param f
     */
    public EmptyModule(String name, File f) {
        super(name);
        this.f = f;
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getWildImportedModules()
     */
    public IToken[] getWildImportedModules() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getTokenImportedModules()
     */
    public IToken[] getTokenImportedModules() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens()
     */
    public IToken[] getGlobalTokens() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getDocString()
     */
    public String getDocString() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens(java.lang.String)
     */
    public IToken[] getGlobalTokens(CompletionState state, ASTManager manager) {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#findDefinition(java.lang.String, int, int)
     */
    public Definition[] findDefinition(String token, int line, int col, IASTManager manager) throws Exception {
        throw new RuntimeException("Not intended to be called");
    }

}
