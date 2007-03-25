package org.python.pydev.editor.codecompletion.revisited.modules;

import org.python.pydev.parser.visitors.scope.ASTEntry;

/**
 * Used for the creation of an ASTEntry that has a source module related
 */
public class ASTEntryWithSourceModule extends ASTEntry{

    private SourceModule module;

    public ASTEntryWithSourceModule(SourceModule module) {
        super(null, module.getAst());
        this.module = module;
    }
    
    public SourceModule getModule(){
        return module;
    }

}
