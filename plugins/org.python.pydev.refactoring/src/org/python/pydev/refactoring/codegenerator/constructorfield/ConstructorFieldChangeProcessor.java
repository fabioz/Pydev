/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.constructorfield;

import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.codegenerator.constructorfield.edit.ConstructorMethodEdit;
import org.python.pydev.refactoring.codegenerator.constructorfield.request.ConstructorFieldRequest;
import org.python.pydev.refactoring.core.base.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;

public class ConstructorFieldChangeProcessor extends AbstractFileChangeProcessor<ConstructorFieldRequest> {

    public ConstructorFieldChangeProcessor(String name, RefactoringInfo info,
            IRequestProcessor<ConstructorFieldRequest> requestProvider) {
        super(name, info, requestProvider);
    }

    @Override
    protected void processEdit() throws MisconfigurationException {
        for (ConstructorFieldRequest req : requestProcessor.getRefactoringRequests()) {
            ConstructorMethodEdit constructorEdit = new ConstructorMethodEdit(req);

            registerEdit(constructorEdit, Messages.constructorFieldConstructor);
        }
    }
}
