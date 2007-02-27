package org.python.pydev.refactoring.ui.actions.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.refactoring.core.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.ui.PythonRefactoringWizard;
import org.python.pydev.refactoring.ui.UITexts;

public abstract class AbstractRefactoringAction extends Action implements IEditorActionDelegate {

	private AbstractPythonRefactoring refactoring;

	private ITextEditor targetEditor;

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		if (targetEditor instanceof ITextEditor) {
			if (targetEditor.getEditorInput() instanceof FileEditorInput) {
				this.targetEditor = (ITextEditor) targetEditor;
			} else {
				this.targetEditor = null;
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	private Shell getShell() {
		Shell result = null;
		if (targetEditor != null) {
			result = targetEditor.getSite().getShell();
		} else {
			result = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		}
		return result;
	}

	private static boolean saveAll() {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		return IDE.saveAllEditors(new IResource[] { workspaceRoot }, true);
	}

	private void setupRefactoring(Class refactoringClass, String name) throws InterruptedException {
		RefactoringInfo req = null;

		try {
			IPythonNature nature = null;
			if (targetEditor instanceof IPyEdit) {
				nature = ((IPyEdit) targetEditor).getPythonNature();
			}
			req = new RefactoringInfo(targetEditor, nature);
			this.refactoring = (AbstractPythonRefactoring) refactoringClass.getConstructors()[0].newInstance(new Object[] { name, req });
			return;
		} catch (Throwable e) {
			showError(UITexts.infoUnavailable);
		}
		throw new InterruptedException();
	}

	private void showError(String msg) {
		MessageDialog.openError(getShell(), UITexts.errorTitle, msg);

	}

	protected void showInfo(String msg) {
		MessageDialog.openInformation(getShell(), UITexts.infoTitle, msg);
	}

	private void openWizard(IAction action, Class refactoring) {

		String title = getTitle(action);

		try {
			this.setupRefactoring(refactoring, title);
			PythonRefactoringWizard wizard = new PythonRefactoringWizard(this.refactoring);
			wizard.setWindowTitle(title);
			RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
			op.run(this.getShell(), action.getText());
			targetEditor.getDocumentProvider().changed(targetEditor.getEditorInput());
		} catch (InterruptedException e) {
		}

	}

	private String getTitle(IAction action) {
		String title = action.getText();
		title = title.substring(0, title.length() - 3);
		return title;
	}

	protected void run(Class refactoring, IAction action) {
		if (saveAll()) {
			this.openWizard(action, refactoring);
		}
	}

	public abstract void run(final IAction action);

}
