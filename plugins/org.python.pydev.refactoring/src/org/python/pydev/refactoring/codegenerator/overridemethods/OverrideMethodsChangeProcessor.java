/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.overridemethods;

import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.codegenerator.overridemethods.edit.MethodEdit;
import org.python.pydev.refactoring.codegenerator.overridemethods.request.OverrideMethodsRequest;
import org.python.pydev.refactoring.core.base.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;

public class OverrideMethodsChangeProcessor extends AbstractFileChangeProcessor<OverrideMethodsRequest> {

    public OverrideMethodsChangeProcessor(String name, RefactoringInfo info,
            IRequestProcessor<OverrideMethodsRequest> requestProcessor) {
        super(name, info, requestProcessor);
    }

    @Override
    protected void processEdit() throws MisconfigurationException {
        for (OverrideMethodsRequest req : requestProcessor.getRefactoringRequests()) {
            MethodEdit methodEdit = new MethodEdit(req);
            registerEdit(methodEdit, Messages.overrideMethodsMethods);
        }
    }
}
