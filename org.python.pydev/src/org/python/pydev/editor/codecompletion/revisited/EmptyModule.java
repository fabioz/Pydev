/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

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
     * @see org.python.pydev.editor.codecompletion.revisited.AbstractModule#getWildImportedModules()
     */
    public IToken[] getWildImportedModules() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.AbstractModule#getTokenImportedModules()
     */
    public IToken[] getTokenImportedModules() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.AbstractModule#getGlobalTokens()
     */
    public IToken[] getGlobalTokens() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.AbstractModule#getDocString()
     */
    public String getDocString() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.AbstractModule#getGlobalTokens(java.lang.String)
     */
    public IToken[] getGlobalTokens(String token) {
        throw new RuntimeException("Not intended to be called");
    }

}
