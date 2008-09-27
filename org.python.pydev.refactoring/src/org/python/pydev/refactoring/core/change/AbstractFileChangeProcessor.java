/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.change;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.edit.AbstractTextEdit;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public abstract class AbstractFileChangeProcessor<T extends IRefactoringRequest> implements IChangeProcessor {

    private TextChange change;

    protected MultiTextEdit multiEdit;

    private String name;

    private IFile file;

    protected IRequestProcessor<T> requestProcessor;

    public AbstractFileChangeProcessor(String name, RefactoringInfo info, IRequestProcessor<T> requestProcessor) {
        this.name = name;
        this.file = info.getSourceFile();
        this.requestProcessor = requestProcessor;
    }

    protected abstract void processEdit();

    public Change createChange() {
        change = new TextFileChange(name, file);
        multiEdit = new MultiTextEdit();
        change.setEdit(this.multiEdit);
        processEdit();
        return change;
    }

    protected void addEdit(TextEdit edit) {
        multiEdit.addChild(edit);
    }

    protected void addGroup(TextEditGroup group) {
        change.addTextEditGroup(group);
    }

    /**
     * Registers an abstractTextEdit to a AbstractFileChangeProcessor using a single editroup
     * 
     * @param edit
     * @param message
     */
    protected void registerEdit(AbstractTextEdit edit, String message) {
        TextEditGroup editGroup = new TextEditGroup(message);
        addGroup(editGroup);
        registerEditInGroup(edit, editGroup);
    }
    
    protected void registerEdit(List<AbstractTextEdit> edits, String message) {
        TextEditGroup group = new TextEditGroup(message);
        addGroup(group);
        
        for (AbstractTextEdit edit : edits) {
            registerEditInGroup(edit, group);
        }
    }
    
    private void registerEditInGroup(AbstractTextEdit edit,
            TextEditGroup editGroup) {
        TextEdit textEdit = edit.getEdit();
        editGroup.addTextEdit(textEdit);    
        addEdit(textEdit);
    }
}
