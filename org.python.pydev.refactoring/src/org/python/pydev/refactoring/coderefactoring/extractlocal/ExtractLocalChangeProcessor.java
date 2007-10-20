/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal;

import org.python.pydev.refactoring.coderefactoring.extractlocal.edit.CreateLocalVariableEdit;
import org.python.pydev.refactoring.coderefactoring.extractlocal.edit.ReplaceWithVariableEdit;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.change.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;

public class ExtractLocalChangeProcessor extends AbstractFileChangeProcessor<ExtractLocalRequest> {
	public ExtractLocalChangeProcessor(String name, RefactoringInfo info, IRequestProcessor<ExtractLocalRequest> requestProcessor) {
		super(name, info, requestProcessor);
	}

	@Override
	protected void processEdit() {
		for (ExtractLocalRequest req : requestProcessor.getRefactoringRequests()) {
			processExtraction(req);
		}
	}

	private void processExtraction(ExtractLocalRequest req) {
		CreateLocalVariableEdit createLocalVariableEdit = new CreateLocalVariableEdit(req);
		ReplaceWithVariableEdit replaceWithVariableEdit = new ReplaceWithVariableEdit(req);
		
		registerEdit(createLocalVariableEdit, Messages.extractLocalCreateLocalVariable);
		registerEdit(replaceWithVariableEdit, Messages.extractLocalReplaceWithVariable);
	}
}
