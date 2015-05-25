/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
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

package org.python.pydev.refactoring.codegenerator.generateproperties;

import java.util.List;

import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.DeleteMethodEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.GetterMethodEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.PropertyEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.edit.SetterMethodEdit;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.SelectionState;
import org.python.pydev.refactoring.core.base.AbstractFileChangeProcessor;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.edit.AbstractTextEdit;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

public class GeneratePropertiesChangeProcessor extends AbstractFileChangeProcessor<GeneratePropertiesRequest> {

    public GeneratePropertiesChangeProcessor(String name, RefactoringInfo info,
            IRequestProcessor<GeneratePropertiesRequest> requestProvider) {
        super(name, info, requestProvider);
    }

    @Override
    protected void processEdit() throws MisconfigurationException {
        List<AbstractTextEdit> getters = new LinkedListWarningOnSlowOperations<AbstractTextEdit>();
        List<AbstractTextEdit> setters = new LinkedListWarningOnSlowOperations<AbstractTextEdit>();
        List<AbstractTextEdit> deleters = new LinkedListWarningOnSlowOperations<AbstractTextEdit>();

        /* Collect all edits and assign them to the corresponding editGroups. */
        for (GeneratePropertiesRequest req : requestProcessor.getRefactoringRequests()) {
            SelectionState state = req.getSelectionState();

            if (state.isGetter()) {
                getters.add(new GetterMethodEdit(req));
            }

            if (state.isSetter()) {
                setters.add(new SetterMethodEdit(req));
            }

            if (state.isDelete()) {
                deleters.add(new DeleteMethodEdit(req));
            }
        }

        List<AbstractTextEdit> propertyEdits = new LinkedListWarningOnSlowOperations<AbstractTextEdit>();
        for (GeneratePropertiesRequest req : requestProcessor.getRefactoringRequests()) {
            propertyEdits.add(new PropertyEdit(req));
        }

        registerEdit(getters, Messages.generatePropertiesGetter);
        registerEdit(setters, Messages.generatePropertiesSetter);
        registerEdit(deleters, Messages.generatePropertiesDelete);
        registerEdit(propertyEdits, Messages.generatePropertiesProperty);
    }
}
