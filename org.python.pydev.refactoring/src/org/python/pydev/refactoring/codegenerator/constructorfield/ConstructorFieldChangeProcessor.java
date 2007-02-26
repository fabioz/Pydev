package org.python.pydev.refactoring.codegenerator.constructorfield;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.refactoring.codegenerator.constructorfield.edit.ConstructorMethodEdit;
import org.python.pydev.refactoring.codegenerator.constructorfield.request.ConstructorFieldRequest;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.change.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.ui.UITexts;

public class ConstructorFieldChangeProcessor extends
		AbstractFileChangeProcessor<ConstructorFieldRequest> {

	public ConstructorFieldChangeProcessor(String name, RefactoringInfo info,
			IRequestProcessor<ConstructorFieldRequest> requestProvider) {
		super(name, info, requestProvider);
	}

	@Override
	protected void processEdit() {

		for (ConstructorFieldRequest req : requestProcessor.getRefactoringRequests()) {

			TextEditGroup constructors = new TextEditGroup(UITexts.constructorFieldConstructor);
			ConstructorMethodEdit constructorEdit = new ConstructorMethodEdit(
					req);

			TextEdit edit = constructorEdit.getEdit();
			
			constructors.addTextEdit(edit);
			addEdit(edit);
			addGroup(constructors);
		}
		

	}

}
