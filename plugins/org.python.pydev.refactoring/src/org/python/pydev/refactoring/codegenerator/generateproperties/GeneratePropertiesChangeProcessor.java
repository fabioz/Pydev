/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.generateproperties;

import java.util.LinkedList;
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

public class GeneratePropertiesChangeProcessor extends AbstractFileChangeProcessor<GeneratePropertiesRequest> {

    public GeneratePropertiesChangeProcessor(String name, RefactoringInfo info,
            IRequestProcessor<GeneratePropertiesRequest> requestProvider) {
        super(name, info, requestProvider);
    }

    @Override
    protected void processEdit() throws MisconfigurationException {
        List<AbstractTextEdit> getters = new LinkedList<AbstractTextEdit>();
        List<AbstractTextEdit> setters = new LinkedList<AbstractTextEdit>();
        List<AbstractTextEdit> deleters = new LinkedList<AbstractTextEdit>();

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

        List<AbstractTextEdit> propertyEdits = new LinkedList<AbstractTextEdit>();
        for (GeneratePropertiesRequest req : requestProcessor.getRefactoringRequests()) {
            propertyEdits.add(new PropertyEdit(req));
        }

        registerEdit(getters, Messages.generatePropertiesGetter);
        registerEdit(setters, Messages.generatePropertiesSetter);
        registerEdit(deleters, Messages.generatePropertiesDelete);
        registerEdit(propertyEdits, Messages.generatePropertiesProperty);
    }
}
