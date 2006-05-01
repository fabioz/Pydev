/*
 * Created on May 1, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public abstract class AbstractRenameMultipleProcess extends AbstractRefactorProcess{
    /**
     * This map contains:
     * key: tuple with module name and the document representing that module
     * value: list of ast entries to be replaced
     */
    protected SortedMap<Tuple<String, IDocument>, List<ASTEntry>> occurrences;

    public AbstractRenameMultipleProcess(){
        occurrences = new TreeMap<Tuple<String,IDocument>, List<ASTEntry>>();
    }
}
