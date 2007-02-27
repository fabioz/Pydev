package org.python.pydev.refactoring.coderefactoring.extractmethod;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ExtractCallEdit;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ExtractMethodEdit;
import org.python.pydev.refactoring.coderefactoring.extractmethod.request.ExtractMethodRequest;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.change.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.ui.UITexts;

public class ExtractMethodChangeProcessor extends AbstractFileChangeProcessor<ExtractMethodRequest> {

	public ExtractMethodChangeProcessor(String name, RefactoringInfo info, IRequestProcessor<ExtractMethodRequest> requestProcessor) {
		super(name, info, requestProcessor);
	}

	@Override
	protected void processEdit() {

		for (ExtractMethodRequest req : requestProcessor.getRefactoringRequests()) {
			processExtraction(req);
		}
	}

	private void processExtraction(ExtractMethodRequest req) {
		TextEditGroup extractMethod = new TextEditGroup(UITexts.extractMethodChangeName);
		TextEditGroup substitute = new TextEditGroup(UITexts.extractMethodReplaceWithCall);

		ExtractMethodEdit extractMethodEdit = new ExtractMethodEdit(req);
		ExtractCallEdit extractCallEdit = new ExtractCallEdit(req);

		TextEdit method = extractMethodEdit.getEdit();
		TextEdit call = extractCallEdit.getEdit();

		addEdit(method);
		addEdit(call);

		updateGroup(extractMethod, substitute, method, call);
	}

	private void updateGroup(TextEditGroup extractMethod, TextEditGroup substitute, TextEdit method, TextEdit call) {
		extractMethod.addTextEdit(method);
		substitute.addTextEdit(call);

		addGroup(extractMethod);
		addGroup(substitute);
	}

}
