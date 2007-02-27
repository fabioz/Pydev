package org.python.pydev.refactoring.codegenerator.generateproperties;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.DeleteMethodEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.GetterMethodEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.PropertyEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.SetterMethodEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.SelectionState;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.change.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.ui.UITexts;

public class GeneratePropertiesChangeProcessor extends AbstractFileChangeProcessor<GeneratePropertiesRequest> {

	public GeneratePropertiesChangeProcessor(String name, RefactoringInfo info, IRequestProcessor<GeneratePropertiesRequest> requestProvider) {
		super(name, info, requestProvider);
	}

	@Override
	protected void processEdit() {

		TextEditGroup getters = new TextEditGroup(UITexts.generatePropertiesGetter);
		TextEditGroup setters = new TextEditGroup(UITexts.generatePropertiesSetter);
		TextEditGroup deletes = new TextEditGroup(UITexts.generatePropertiesDelete);
		TextEditGroup properties = new TextEditGroup(UITexts.generatePropertiesProperty);

		for (GeneratePropertiesRequest req : requestProcessor.getRefactoringRequests()) {
			SelectionState state = req.getSelectionState();

			if (state.isGetter()) {
				GetterMethodEdit getter = new GetterMethodEdit(req);

				TextEdit edit = getter.getEdit();
				addEdit(edit);

				getters.addTextEdit(edit);
			}
			if (state.isSetter()) {
				SetterMethodEdit setter = new SetterMethodEdit(req);

				TextEdit edit = setter.getEdit();
				addEdit(edit);

				setters.addTextEdit(edit);
			}
			if (state.isDelete()) {
				DeleteMethodEdit delete = new DeleteMethodEdit(req);

				TextEdit edit = delete.getEdit();
				addEdit(edit);

				deletes.addTextEdit(edit);
			}
		}

		for (GeneratePropertiesRequest req : requestProcessor.getRefactoringRequests()) {
			PropertyEdit property = new PropertyEdit(req);
			TextEdit edit = property.getEdit();
			addEdit(edit);
			properties.addTextEdit(edit);
		}

		addGroup(getters);
		addGroup(setters);
		addGroup(deletes);
		addGroup(properties);
	}

}
