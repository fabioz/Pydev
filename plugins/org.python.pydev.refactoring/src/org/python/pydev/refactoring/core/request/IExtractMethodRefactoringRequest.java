package org.python.pydev.refactoring.core.request;

import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;

public interface IExtractMethodRefactoringRequest {

    /**
     * @return The scope containing the current selection.
     */
    AbstractScopeNode<?> getScopeAdapter();
}
