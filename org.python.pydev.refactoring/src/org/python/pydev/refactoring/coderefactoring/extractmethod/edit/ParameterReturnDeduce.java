package org.python.pydev.refactoring.coderefactoring.extractmethod.edit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;

public class ParameterReturnDeduce {

	private List<String> parameters;

	private Set<String> returns;

	private AbstractScopeNode<?> scopeAdapter;

	private ITextSelection selection;

	public ParameterReturnDeduce(AbstractScopeNode<?> scope,
			ITextSelection selection) {
		this.scopeAdapter = scope;
		this.selection = selection;
		this.parameters = new ArrayList<String>();
		this.returns = new HashSet<String>();
		deduce();
	}

	private void deduce() {
		List<SimpleAdapter> before = new ArrayList<SimpleAdapter>();
		List<SimpleAdapter> after = new ArrayList<SimpleAdapter>();
		List<SimpleAdapter> selected = scopeAdapter.getModule()
				.getWithinSelection(this.selection,
						scopeAdapter.getUsedVariables());

		extractBeforeAfterVariables(selected, before, after);

		deduceParameters(before, selected);
		deduceReturns(after, selected);

	}

	private void deduceParameters(List<SimpleAdapter> before,
			List<SimpleAdapter> selected) {
		for (SimpleAdapter adapter : before) {
			if (adapter.getASTNode() instanceof Name) {
				Name variable = (Name) adapter.getASTNode();
				if (isUsed(variable.id, selected)) {
					if (!parameters.contains(variable.id)) {
						parameters.add(variable.id);
					}
				}
			}

		}
	}

	private void deduceReturns(List<SimpleAdapter> after,
			List<SimpleAdapter> selected) {
		for (SimpleAdapter adapter : after) {
			if (adapter.getASTNode() instanceof Name) {
				Name variable = (Name) adapter.getASTNode();
				if (isStored(variable.id, selected)) {
					returns.add(variable.id);
				}
			}
		}
	}

	private void extractBeforeAfterVariables(
			List<SimpleAdapter> selectedVariables, List<SimpleAdapter> before,
			List<SimpleAdapter> after) {
		List<SimpleAdapter> scopeVariables = scopeAdapter.getUsedVariables();

		if (selectedVariables.size() < 1)
			return;

		SimpleAdapter firstSelectedVariable = selectedVariables.get(0);
		SimpleAdapter lastSelectedVariable = selectedVariables
				.get(selectedVariables.size() - 1);

		for (SimpleAdapter adapter : scopeVariables) {
			if (isBeforeSelectedLine(firstSelectedVariable, adapter)
					|| isBeforeOnSameLine(firstSelectedVariable, adapter)) {
				before.add(adapter);
			} else if (isAfterSelectedLine(lastSelectedVariable, adapter)
					|| isAfterOnSameLine(lastSelectedVariable, adapter)) {
				after.add(adapter);
			}
		}
	}

	private boolean isAfterOnSameLine(SimpleAdapter lastSelectedVariable,
			SimpleAdapter adapter) {
		return adapter.getNodeFirstLine() == lastSelectedVariable
				.getNodeFirstLine()
				&& (adapter.getNodeIndent() > lastSelectedVariable
						.getNodeIndent());
	}

	private boolean isAfterSelectedLine(SimpleAdapter lastSelectedVariable,
			SimpleAdapter adapter) {
		return adapter.getNodeFirstLine() > lastSelectedVariable
				.getNodeFirstLine();
	}

	private boolean isBeforeOnSameLine(SimpleAdapter firstSelectedVariable,
			SimpleAdapter adapter) {
		return adapter.getNodeFirstLine() == firstSelectedVariable
				.getNodeFirstLine()
				&& (adapter.getNodeIndent() < firstSelectedVariable
						.getNodeIndent());
	}

	private boolean isBeforeSelectedLine(SimpleAdapter firstSelectedVariable,
			SimpleAdapter adapter) {
		return adapter.getNodeFirstLine() < firstSelectedVariable
				.getNodeFirstLine();
	}

	private boolean isUsed(String var, List<SimpleAdapter> scopeVariables) {
		for (SimpleAdapter adapter : scopeVariables) {
			if (adapter.getASTNode() instanceof Name) {
				Name scopeVar = (Name) adapter.getASTNode();
				if (scopeVar.id.compareTo(var) == 0) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isStored(String var, List<SimpleAdapter> scopeVariables) {
		boolean isStored = false; 
		// must traverse all variables, because a
		// variable may be used in other context!
		for (SimpleAdapter adapter : scopeVariables) {
			if (adapter.getASTNode() instanceof Name) {
				Name scopeVar = (Name) adapter.getASTNode();
				if (scopeVar.id.compareTo(var) == 0) {
					isStored = (scopeVar.ctx == Name.Store || scopeVar.ctx == Name.AugStore);
				}
			}

			if (isStored)
				break;
		}
		return isStored;
	}

	public List<String> getParameters() {
		return this.parameters;
	}

	public List<String> getReturns() {
		List<String> methodReturns = new ArrayList<String>();
		methodReturns.addAll(this.returns);

		return methodReturns;

	}

}
