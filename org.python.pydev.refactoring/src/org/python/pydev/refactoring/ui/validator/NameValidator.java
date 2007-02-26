package org.python.pydev.refactoring.ui.validator;

import java.util.Arrays;
import java.util.List;

import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;

public class NameValidator {

	// match invalid keywords (Python pocket reference)
	private final String[] keywords = { "and", "assert", "break", "class",
			"continue", "def", "del", "elif", "else", "except", "exec",
			"finally", "for", "from", "global", "if", "import", "in", "is",
			"lambda", "not", "or", "pass", "print", "raise", "return", "try",
			"while", "yield" };

	private final String nameRegExp = "[a-zA-Z_][a-zA-z0-9]*";

	private AbstractScopeNode<?> scopeNode;

	private List<String> keywordList;

	public NameValidator(AbstractScopeNode<?> scope) {
		this.scopeNode = scope;
		this.keywordList = Arrays.asList(keywords);
	}

	public void validateUniqueVariable(String name) throws Throwable {
		if (scopeNode.alreadyUsedName(name)) {
			throw new Exception("Variable name '" + name + "' is already used");
		}
	}

	public void validateLocalName(String name) throws Throwable {
		if (!name.matches(nameRegExp)) {
			throw new Exception("'" + name + "' is not a valid argument name");
		}
		validateNotKeyword(name);
	}

	private void validateNotKeyword(String name) throws Exception {
		if (this.keywordList.contains(name)) {
			throw new Exception("'" + name + "' is a reserved word");
		}
	}

	public void validateMethodName(String name) throws Throwable {
		if (!name.matches(nameRegExp)) {
			throw new Exception("'" + name + "' is not a valid function name");
		}
		validateNotKeyword(name);
	}

	public void validateUniqueFunction(String name) throws Exception {
		AbstractScopeNode<?> parentAdapter = scopeNode.getParent();

		if (parentAdapter != null) {
			for (FunctionDefAdapter function : parentAdapter.getFunctions()) {
				if (function.getName().compareTo(name) == 0) {
					throw new Exception("Function name '" + name
							+ "' is already used");
				}
			}
		}

	}

}
