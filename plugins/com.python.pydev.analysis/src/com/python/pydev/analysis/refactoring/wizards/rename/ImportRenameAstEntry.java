package com.python.pydev.analysis.refactoring.wizards.rename;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public abstract class ImportRenameAstEntry extends ASTEntry implements IRefactorCustomEntry {

    public ImportRenameAstEntry(ASTEntry parent, SimpleNode node) {
        super(parent, node);
    }

}
