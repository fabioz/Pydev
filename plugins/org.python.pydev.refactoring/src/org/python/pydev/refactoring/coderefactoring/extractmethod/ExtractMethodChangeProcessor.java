/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractmethod;

import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ExtractCallEdit;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ExtractMethodEdit;
import org.python.pydev.refactoring.coderefactoring.extractmethod.request.ExtractMethodRequest;
import org.python.pydev.refactoring.core.base.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;

public class ExtractMethodChangeProcessor extends AbstractFileChangeProcessor<ExtractMethodRequest> {
    public ExtractMethodChangeProcessor(String name, RefactoringInfo info, IRequestProcessor<ExtractMethodRequest> requestProcessor) {
        super(name, info, requestProcessor);
    }

    @Override
    protected void processEdit() throws MisconfigurationException {
        for(ExtractMethodRequest req:requestProcessor.getRefactoringRequests()){
            processExtraction(req);
        }
    }

    private void processExtraction(ExtractMethodRequest req) throws MisconfigurationException {
        ExtractMethodEdit extractMethodEdit = new ExtractMethodEdit(req);
        ExtractCallEdit extractCallEdit = new ExtractCallEdit(req);

        registerEdit(extractMethodEdit, Messages.extractMethodChangeName);
        registerEdit(extractCallEdit, Messages.extractMethodReplaceWithCall);
    }
}
