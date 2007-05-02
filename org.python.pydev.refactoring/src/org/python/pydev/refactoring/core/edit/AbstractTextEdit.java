package org.python.pydev.refactoring.core.edit;

import org.eclipse.text.edits.TextEdit;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public abstract class AbstractTextEdit {

	private final String WHITESPACE = " ";

	protected ModuleAdapter moduleAdapter;

	protected IASTNodeAdapter offsetAdapter;

	protected NodeHelper nodeHelper;

    protected String newLineDelim;

	private AbstractTextEdit(ModuleAdapter moduleAdapter, IASTNodeAdapter offsetAdapter, String newLineDelim) {
		this.moduleAdapter = moduleAdapter;
		this.offsetAdapter = offsetAdapter;
		this.nodeHelper = new NodeHelper(newLineDelim);
        this.newLineDelim = newLineDelim;
	}

	public AbstractTextEdit(IRefactoringRequest req) {
		this(req.getOffsetNode().getModule(), req.getOffsetNode(), req.getNewLineDelim());
	}

	protected abstract SimpleNode getEditNode();

	public abstract TextEdit getEdit();

	protected String getFormatedNode() {
		SimpleNode node = getEditNode();
		String source = VisitorFactory.createSourceFromAST(node, newLineDelim);
		return getIndentedSource(node, source, getIndent());
	}

	private String getIndentedSource(SimpleNode node, String source, int indent) {
		StringBuilder indented = new StringBuilder();
		String indentation = getIndentation(indent);
		indented.append(newLineDelim + indentation);
		source = source.replaceAll(REPLACE_PATTERN(), newLineDelim + indentation);
		source = source.trim();
		indented.append(source);
		indented.append(newLineDelim);
		if (nodeHelper.isFunctionDef(node))
			indented.append(newLineDelim);

		return indented.toString();
	}

	private String REPLACE_PATTERN() {
		return "\\r\\n|\\n|\\r";
	}

	protected String getIndentation(int indent) {
		StringBuffer buf = new StringBuffer();
		while (indent > 1) {
			buf.append(WHITESPACE);
			indent--;
		}
		return buf.toString();
	}

	protected String getCapitalString(String name) {
		StringBuilder sb = new StringBuilder(name);
		sb.replace(0, 1, name.substring(0, 1).toUpperCase());
		return sb.toString();
	}

	public abstract int getOffsetStrategy();

	public int getOffset() {
		return moduleAdapter.getOffset(offsetAdapter, getOffsetStrategy());
	}

	public int getIndent() {
		return offsetAdapter.getNodeBodyIndent();
	}

}
