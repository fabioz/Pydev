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

package org.python.pydev.refactoring.core.base;

import java.util.List;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.core.edit.AbstractTextEdit;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;
import org.python.pydev.shared_ui.utils.SynchronizedTextFileChange;

public abstract class AbstractFileChangeProcessor<T extends IRefactoringRequest> implements IChangeProcessor {

    private TextChange change;
    private MultiTextEdit multiEdit;
    private String name;
    private RefactoringInfo info;

    protected final IRequestProcessor<T> requestProcessor;

    public AbstractFileChangeProcessor(String name, RefactoringInfo info, IRequestProcessor<T> requestProcessor) {
        this.name = name;
        this.info = info;
        this.requestProcessor = requestProcessor;
    }

    protected abstract void processEdit() throws MisconfigurationException;

    public Change createChange() throws MisconfigurationException {
        if (info.getSourceFile() != null) {
            change = new SynchronizedTextFileChange(name, info.getSourceFile());
            change.setTextType("py");
        } else {
            // Not insisting on a source file makes testing easier.
            change = PyDocumentChange.create(name, info.getDocument());

        }
        multiEdit = new MultiTextEdit();
        change.setEdit(this.multiEdit);
        processEdit();
        return change;
    }

    /**
     * Registers an abstractTextEdit to a AbstractFileChangeProcessor using a single editroup
     *
     * @param edit
     * @param message
     * @throws MisconfigurationException
     */
    protected void registerEdit(AbstractTextEdit edit, String message) throws MisconfigurationException {
        TextEditGroup editGroup = new TextEditGroup(message);
        addGroup(editGroup);
        registerEditInGroup(edit, editGroup);
    }

    /**
     * Registers a group of textedits to a single editgroup
     *
     * @param edits
     * @param message
     * @throws MisconfigurationException
     */
    protected void registerEdit(List<AbstractTextEdit> edits, String message) throws MisconfigurationException {
        TextEditGroup group = new TextEditGroup(message);
        addGroup(group);

        for (AbstractTextEdit edit : edits) {
            registerEditInGroup(edit, group);
        }
    }

    private void addGroup(TextEditGroup group) {
        change.addTextEditGroup(group);
    }

    private void registerEditInGroup(AbstractTextEdit edit, TextEditGroup editGroup) throws MisconfigurationException {
        TextEdit textEdit = edit.getEdit();
        editGroup.addTextEdit(textEdit);
        multiEdit.addChild(textEdit);
    }
}
