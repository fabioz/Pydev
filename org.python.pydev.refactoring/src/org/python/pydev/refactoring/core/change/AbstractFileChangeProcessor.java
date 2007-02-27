package org.python.pydev.refactoring.core.change;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.request.IRefactoringRequest;
import org.python.pydev.refactoring.core.request.IRequestProcessor;

public abstract class AbstractFileChangeProcessor<T extends IRefactoringRequest> implements IChangeProcessor {

	private TextChange change;

	protected MultiTextEdit multiEdit;

	private String name;

	private IFile file;

	protected IRequestProcessor<T> requestProcessor;

	public AbstractFileChangeProcessor(String name, RefactoringInfo info, IRequestProcessor<T> requestProcessor) {
		this(name, info.getSourceFile(), requestProcessor);
	}

	public AbstractFileChangeProcessor(String name, IFile file, IRequestProcessor<T> requestProcessor) {
		this.name = name;
		this.file = file;
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

}
