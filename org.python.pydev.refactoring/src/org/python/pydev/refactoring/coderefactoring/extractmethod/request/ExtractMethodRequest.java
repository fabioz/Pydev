/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractmethod.request;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;

public class ExtractMethodRequest implements IRefactoringRequest {

	private AbstractScopeNode<?> scopeAdapter;

	private int offsetStrategy;

	private String methodName;

	private ModuleAdapter parsedSelection;

	private List<String> parameters;

	private List<String> returnVariables;

	private Map<String, String> renamedVariables;

	private ITextSelection selection;

    private String endLineDelim;

	public ExtractMethodRequest(String methodName, ITextSelection selection, AbstractScopeNode<?> scopeAdapter,
			ModuleAdapter parsedSelection, List<String> callParameters, List<String> returnVariables, Map<String, String> renamedVariables,
			int offsetStrategy, String endLineDelim) {
		this.methodName = methodName;
		this.selection = selection;
		this.scopeAdapter = scopeAdapter;
		this.parsedSelection = parsedSelection;
		this.offsetStrategy = offsetStrategy;

		this.parameters = callParameters;
		this.returnVariables = returnVariables;
		this.renamedVariables = renamedVariables;
        this.endLineDelim = endLineDelim;
	}

	public ITextSelection getSelection() {
		return selection;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public Map<String, String> getRenamedVariables() {
		return renamedVariables;
	}

	public List<String> getReturnVariables() {
		return returnVariables;
	}

	public int getOffsetStrategy() {
		return offsetStrategy;
	}

	public AbstractScopeNode<?> getScopeAdapter() {
		return scopeAdapter;
	}

	public IASTNodeAdapter<? extends SimpleNode> getOffsetNode() {
		IASTNodeAdapter<? extends SimpleNode> offsetNode = scopeAdapter;
		while (offsetNode instanceof FunctionDefAdapter)
			offsetNode = offsetNode.getParent();

		return offsetNode;
	}

	public String getMethodName() {
		return methodName;
	}

	public ModuleAdapter getParsedSelection() {
		return parsedSelection;
	}

    public String getNewLineDelim() {
        return endLineDelim;
    }

}
