/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import org.python.pydev.parser.visitors.scope.ASTEntry;

/**
 * Used for the creation of an ASTEntry that has a source module related
 */
public class ASTEntryWithSourceModule extends ASTEntry {

    private SourceModule module;

    public ASTEntryWithSourceModule(SourceModule module) {
        super(null, module.getAst());
        this.module = module;
    }

    public SourceModule getModule() {
        return module;
    }

}
