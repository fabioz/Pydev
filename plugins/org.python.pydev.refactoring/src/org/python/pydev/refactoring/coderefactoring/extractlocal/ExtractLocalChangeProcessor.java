/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
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
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.coderefactoring.extractlocal.edit.CreateLocalVariableEdit;
import org.python.pydev.refactoring.coderefactoring.extractlocal.edit.ReplaceDuplicateWithVariableEdit;
import org.python.pydev.refactoring.coderefactoring.extractlocal.edit.ReplaceWithVariableEdit;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.base.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.shared_core.structure.Tuple;

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
