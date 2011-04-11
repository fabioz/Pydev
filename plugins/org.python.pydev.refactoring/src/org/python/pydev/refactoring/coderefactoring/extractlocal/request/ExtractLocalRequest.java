/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal.request;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class ExtractLocalRequest implements IRefactoringRequest {

    public final RefactoringInfo info;
    public final ITextSelection selection;
    public final exprType expression;
    public final String variableName;

    public ExtractLocalRequest(RefactoringInfo info, ITextSelection selection, exprType expression, String variableName) {
        this.info = info;
        this.selection = selection;
        this.expression = expression;
        this.variableName = variableName;
    }

    public int getOffsetStrategy() {
        return 0;
    }

    public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
        return info.getScopeAdapter();
    }

    public AdapterPrefs getAdapterPrefs() {
        return info.getAdapterPrefs();
    }
}
