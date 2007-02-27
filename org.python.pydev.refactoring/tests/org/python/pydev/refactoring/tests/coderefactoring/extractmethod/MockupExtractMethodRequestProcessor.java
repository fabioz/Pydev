package org.python.pydev.refactoring.tests.coderefactoring.extractmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ParameterReturnDeduce;
import org.python.pydev.refactoring.coderefactoring.extractmethod.request.ExtractMethodRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public class MockupExtractMethodRequestProcessor implements IRequestProcessor<ExtractMethodRequest> {

	private AbstractScopeNode<?> scopeAdapter;

	private int offsetStrategy;

	private ModuleAdapter parsedSelection;

	private ParameterReturnDeduce deducer;

	private Map<String, String> renameMap;

	private ITextSelection selection;

	public MockupExtractMethodRequestProcessor(AbstractScopeNode<?> scopeAdapter, ITextSelection selection, ModuleAdapter parsedSelection,
			ParameterReturnDeduce deducer, Map<String, String> renameMap, int offsetStrategy) {

		this.scopeAdapter = scopeAdapter;
		this.selection = selection;
		this.parsedSelection = parsedSelection;
		this.offsetStrategy = offsetStrategy;
		this.deducer = deducer;
		this.renameMap = renameMap;
	}

	public List<ExtractMethodRequest> getRefactoringRequests() {
		List<ExtractMethodRequest> requests = new ArrayList<ExtractMethodRequest>();
		ExtractMethodRequest req = new ExtractMethodRequest("pepticMethod", this.selection, this.scopeAdapter, this.parsedSelection,
				deducer.getParameters(), deducer.getReturns(), this.renameMap, this.offsetStrategy);
		requests.add(req);

		return requests;

	}
}
