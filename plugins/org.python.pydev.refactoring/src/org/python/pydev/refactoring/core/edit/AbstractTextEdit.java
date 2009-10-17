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
import org.python.pydev.refactoring.ast.adapters.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.factory.PyAstFactory;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;
import org.python.pydev.refactoring.ast.visitors.rewriter.RewriterVisitor;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public abstract class AbstractTextEdit {

    private static final String WHITESPACE = " ";
    private static final String REPLACE_PATTERN = "\\r\\n|\\n|\\r";

    protected ModuleAdapter moduleAdapter;

    protected IASTNodeAdapter<? extends SimpleNode> offsetAdapter;

    protected NodeHelper nodeHelper;

    protected AdapterPrefs adapterPrefs;
    protected PyAstFactory astFactory;

    public AbstractTextEdit(IRefactoringRequest req) {
        this.moduleAdapter = req.getOffsetNode().getModule();
        this.offsetAdapter = req.getOffsetNode();
        this.nodeHelper = new NodeHelper(req.getAdapterPrefs());
        this.adapterPrefs = req.getAdapterPrefs();
        this.astFactory = new PyAstFactory(this.nodeHelper.getAdapterPrefs());
    }

    protected abstract SimpleNode getEditNode() throws MisconfigurationException;

    public abstract TextEdit getEdit() throws MisconfigurationException;

    protected String getFormattedNode() throws MisconfigurationException {
        SimpleNode node = getEditNode().createCopy();
        try{
            PyAstFactory.makeValid(node);
        }catch(Exception e){
            throw new RuntimeException(e);
        }

        String source = RewriterVisitor.createSourceFromAST(node, this.adapterPrefs);
        return getIndentedSource(node, source, getIndent());
    }

    private String getIndentedSource(SimpleNode node, String source, int indent) {
        StringBuilder indented = new StringBuilder();
        String indentation = getIndentation(indent);

        if(nodeHelper.isFunctionDef(node)){
            indented.append(this.adapterPrefs.endLineDelim);
        }

        indented.append(indentation);
        source = source.replaceAll(REPLACE_PATTERN, this.adapterPrefs.endLineDelim + indentation);
        source = source.trim();
        indented.append(source);
        indented.append(this.adapterPrefs.endLineDelim);

        if(nodeHelper.isFunctionDef(node)){
            indented.append(this.adapterPrefs.endLineDelim);
        }

        return indented.toString();
    }

    protected String getIndentation(int indent) {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < indent; i++){
            buf.append(WHITESPACE);
        }
        return buf.toString();
    }

    public abstract int getOffsetStrategy();

    public int getOffset() {
        return moduleAdapter.getOffset(offsetAdapter, getOffsetStrategy());
    }

    public int getIndent() {
        return offsetAdapter.getNodeBodyIndent();
    }

}
