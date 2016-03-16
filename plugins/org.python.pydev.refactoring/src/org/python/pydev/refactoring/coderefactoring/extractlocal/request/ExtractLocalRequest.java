/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal.request;

import java.util.List;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;
import org.python.pydev.shared_core.structure.Tuple;

public class ExtractLocalRequest implements IRefactoringRequest {

    public final RefactoringInfo info;
    public final ITextSelection selection;
    public final exprType expression;
    public final String variableName;
    public final List<Tuple<ITextSelection, SimpleNode>> duplicates;
    public final boolean replaceDuplicates;

    public ExtractLocalRequest(RefactoringInfo info, ITextSelection selection, exprType expression,
            String variableName, List<Tuple<ITextSelection, SimpleNode>> duplicates, boolean replaceDuplicates) {
        this.info = info;
        this.selection = selection;
        this.expression = expression;
        this.variableName = variableName;
        this.duplicates = duplicates;
        this.replaceDuplicates = replaceDuplicates;
    }

    public int getOffsetStrategy() {
        return 0;
    }

    @Override
    public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
        return info.getScopeAdapter();
    }

    @Override
    public AdapterPrefs getAdapterPrefs() {
        return info.getAdapterPrefs();
    }
}
