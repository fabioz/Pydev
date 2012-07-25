/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal;

import java.util.List;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.coderefactoring.extractlocal.edit.CreateLocalVariableEdit;
import org.python.pydev.refactoring.coderefactoring.extractlocal.edit.ReplaceDuplicateWithVariableEdit;
import org.python.pydev.refactoring.coderefactoring.extractlocal.edit.ReplaceWithVariableEdit;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.base.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;

public class ExtractLocalChangeProcessor extends AbstractFileChangeProcessor<ExtractLocalRequest> {
    public ExtractLocalChangeProcessor(String name, RefactoringInfo info,
            IRequestProcessor<ExtractLocalRequest> requestProcessor) {
        super(name, info, requestProcessor);
    }

    @Override
    protected void processEdit() throws MisconfigurationException {
        for (ExtractLocalRequest req : requestProcessor.getRefactoringRequests()) {
            processExtraction(req);
        }
    }

    private void processExtraction(ExtractLocalRequest req) throws MisconfigurationException {
        CreateLocalVariableEdit createLocalVariableEdit = new CreateLocalVariableEdit(req);
        ReplaceWithVariableEdit replaceWithVariableEdit = new ReplaceWithVariableEdit(req);

        registerEdit(createLocalVariableEdit, Messages.extractLocalCreateLocalVariable);
        registerEdit(replaceWithVariableEdit, Messages.extractLocalReplaceWithVariable);

        if (req.replaceDuplicates) {
            List<Tuple<ITextSelection, SimpleNode>> duplicates = req.duplicates;
            for (Tuple<ITextSelection, SimpleNode> dup : duplicates) {
                try {
                    ReplaceDuplicateWithVariableEdit v = new ReplaceDuplicateWithVariableEdit(req, dup);
                    registerEdit(v, "Replace duplicate.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
