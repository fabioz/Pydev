/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.coderefactoring.inlinelocal.request;

import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class InlineLocalRequest implements IRefactoringRequest {

    public final RefactoringInfo info;

    public final Assign assignment;
    public final List<Name> variables;

    public InlineLocalRequest(RefactoringInfo info, Assign assignment, List<Name> variables) {
        this.info = info;
        this.assignment = assignment;
        this.variables = variables;
    }

    public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
        return info.getScopeAdapter();
    }

    public AdapterPrefs getAdapterPrefs() {
        return info.getAdapterPrefs();
    }

}
