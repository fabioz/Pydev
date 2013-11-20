/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
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

package org.python.pydev.refactoring.core.edit;

import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.NodeHelper;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.rewriter.Rewriter;
import org.python.pydev.refactoring.core.request.IExtractMethodRefactoringRequest;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public abstract class AbstractTextEdit {

    private static final String WHITESPACE = " ";
    private static final String REPLACE_PATTERN = "\\r\\n|\\n|\\r";

    protected ModuleAdapter moduleAdapter;

    protected IASTNodeAdapter<? extends SimpleNode> offsetAdapter;

    protected NodeHelper nodeHelper;

    protected AdapterPrefs adapterPrefs;
    protected PyAstFactory astFactory;
    private AbstractScopeNode<?> scopeAdapter;

    public AbstractTextEdit(IRefactoringRequest req) {
        this.moduleAdapter = req.getOffsetNode().getModule();
        this.offsetAdapter = req.getOffsetNode();
        if (req instanceof IExtractMethodRefactoringRequest) {
            this.scopeAdapter = ((IExtractMethodRefactoringRequest) req).getScopeAdapter();
        }
        this.nodeHelper = new NodeHelper(req.getAdapterPrefs());
        this.adapterPrefs = req.getAdapterPrefs();
        this.astFactory = new PyAstFactory(this.nodeHelper.getAdapterPrefs());
    }

    protected abstract SimpleNode getEditNode() throws MisconfigurationException;

    public abstract TextEdit getEdit() throws MisconfigurationException;

    protected String getFormattedNode() throws MisconfigurationException {
        SimpleNode node = getEditNode().createCopy();
        try {
            MakeAstValidForPrettyPrintingVisitor.makeValid(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String source = Rewriter.createSourceFromAST(node, this.adapterPrefs);
        return getIndentedSource(node, source, getIndent());
    }

    private String getIndentedSource(SimpleNode node, String source, String indentation) {
        StringBuilder indented = new StringBuilder();

        if (nodeHelper.isFunctionDef(node)) {
            indented.append(this.adapterPrefs.endLineDelim);
        }

        indented.append(indentation);
        source = source.replaceAll(REPLACE_PATTERN, this.adapterPrefs.endLineDelim + indentation);
        source = source.trim();
        indented.append(source);
        indented.append(this.adapterPrefs.endLineDelim);

        if (nodeHelper.isFunctionDef(node)) {
            indented.append(this.adapterPrefs.endLineDelim);
        }

        return indented.toString();
    }

    public abstract int getOffsetStrategy();

    public int getOffset() {
        return moduleAdapter.getOffset(offsetAdapter, getOffsetStrategy(), this.scopeAdapter);
    }

    public String getIndent() {
        return offsetAdapter.getNodeBodyIndent();
    }

}
