/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;

import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

/**
 * @author Fabio Zadrozny
 */
public class EmptyModule extends AbstractModule {

    private static final long serialVersionUID = 1L;
    public File f;

    @Override
    public File getFile() {
        return f;
    }

    /**
     * @param f
     */
    public EmptyModule(String name, File f) {
        super(name);
        this.f = f;
    }

    @Override
    public IPythonNature getNature() {
        return null;
    }

    @Override
    public boolean hasFutureImportAbsoluteImportDeclared() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getWildImportedModules()
     */
    @Override
    public IToken[] getWildImportedModules() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getTokenImportedModules()
     */
    @Override
    public IToken[] getTokenImportedModules() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens()
     */
    @Override
    public IToken[] getGlobalTokens() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getDocString()
     */
    @Override
    public String getDocString() {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens(java.lang.String)
     */
    @Override
    public IToken[] getGlobalTokens(ICompletionState state, ICodeCompletionASTManager manager) {
        throw new RuntimeException("Not intended to be called");
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#findDefinition(java.lang.String, int, int)
     */
    @Override
    public Definition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature)
            throws Exception {
        throw new RuntimeException("Not intended to be called");
    }

    @Override
    public boolean isInDirectGlobalTokens(String tok, ICompletionCache completionCache) {
        throw new RuntimeException("Not intended to be called");
    }

    public boolean isInDirectImportTokens(String tok) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EmptyModule)) {
            return false;
        }
        EmptyModule m = (EmptyModule) obj;

        if (name == null || m.name == null) {
            if (name != m.name) {
                return false;
            }
            //both null at this point
        } else if (!name.equals(m.name)) {
            return false;
        }

        if (f == null || m.f == null) {
            if (f != m.f) {
                return false;
            }
            //both null at this point
        } else if (!f.equals(m.f)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 71;
        if (f != null) {
            hash += f.hashCode();
        }
        if (name != null) {
            hash += name.hashCode();
        }
        return hash;
    }

}
