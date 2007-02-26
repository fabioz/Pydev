package org.python.pydev.refactoring.codegenerator.overridemethods;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.refactoring.codegenerator.overridemethods.edit.MethodEdit;
import org.python.pydev.refactoring.codegenerator.overridemethods.request.OverrideMethodsRequest;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.change.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.ui.UITexts;

public class OverrideMethodsChangeProcessor extends
		AbstractFileChangeProcessor<OverrideMethodsRequest> {

	public OverrideMethodsChangeProcessor(String name, RefactoringInfo info,
			IRequestProcessor<OverrideMethodsRequest> requestProcessor) {
		super(name, info, requestProcessor);
	}

	@Override
	protected void processEdit() {
		for (OverrideMethodsRequest req : requestProcessor
				.getRefactoringRequests()) {

			TextEditGroup methods = new TextEditGroup(
					UITexts.overrideMethodsMethods);
			MethodEdit methodEdit = new MethodEdit(req);

			TextEdit edit = methodEdit.getEdit();
			addEdit(edit);

			methods.addTextEdit(edit);

			addGroup(methods);
		}

	}

}
