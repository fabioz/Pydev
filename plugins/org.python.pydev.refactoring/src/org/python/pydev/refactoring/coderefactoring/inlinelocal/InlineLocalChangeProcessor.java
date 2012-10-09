/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.coderefactoring.inlinelocal;

import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.refactoring.coderefactoring.inlinelocal.edit.RemoveAssignment;
import org.python.pydev.refactoring.coderefactoring.inlinelocal.edit.ReplaceWithExpression;
import org.python.pydev.refactoring.coderefactoring.inlinelocal.request.InlineLocalRequest;
import org.python.pydev.refactoring.core.base.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;

public class InlineLocalChangeProcessor extends AbstractFileChangeProcessor<InlineLocalRequest> {
    public InlineLocalChangeProcessor(String name, RefactoringInfo info,
            IRequestProcessor<InlineLocalRequest> requestProcessor) {
        super(name, info, requestProcessor);
    }

    @Override
    protected void processEdit() throws MisconfigurationException {
        for (InlineLocalRequest req : requestProcessor.getRefactoringRequests()) {
            processExtraction(req);
        }
    }

    private void processExtraction(InlineLocalRequest req) throws MisconfigurationException {
        RemoveAssignment removeEdit = new RemoveAssignment(req);
        registerEdit(removeEdit, Messages.inlineLocalRemoveAssignment);

        /* Replace all variables with the assigned expression */
        for (Name variable : req.variables) {
            /* Ignore the assignment */
            if (variable.parent == req.assignment) {
                continue;
            }

            ReplaceWithExpression replaceWithExpressionEdit = new ReplaceWithExpression(req, variable);
            registerEdit(replaceWithExpressionEdit, Messages.inlineLocalReplaceWithExpression);
        }
    }
}
