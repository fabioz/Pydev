package org.python.pydev.refactoring.coderefactoring.extractmethod;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionException;
import org.python.pydev.refactoring.core.AbstractPythonRefactoring;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.core.change.IChangeProcessor;
import org.python.pydev.refactoring.ui.UITexts;
import org.python.pydev.refactoring.ui.pages.ExtractMethodPage;
import org.python.pydev.refactoring.ui.pages.ExtractMethodPreviewPage;

public class ExtractMethodRefactoring extends AbstractPythonRefactoring {

	private ExtractMethodRequestProcessor requestProcessor;

	private IChangeProcessor changeProcessor;

	private ModuleAdapter parsedExtendedSelection;

	private ModuleAdapter parsedUserSelection;

	private ModuleAdapter module;

	public ExtractMethodRefactoring(String name, RefactoringInfo req) {
		super(name, req);
		this.parsedExtendedSelection = null;
		this.parsedUserSelection = req.getParsedUserSelection();
		this.parsedExtendedSelection = req.getParsedExtendedSelection();
		this.module = req.getModule();

		validateSelections();

		try {
			initWizard(name);
		} catch (Throwable e) {
			status.addInfo(UITexts.infoFixCode);
		}
	}

	private void initWizard(String name) throws Throwable {
		ITextSelection standardSelection = req.getUserSelection();
		ModuleAdapter standardModule = this.parsedUserSelection;
		if (standardModule == null) {
			standardSelection = req.getExtendedSelection();
			standardModule = this.parsedExtendedSelection;
		}

		this.requestProcessor = new ExtractMethodRequestProcessor(req.getScopeAdapter(), standardModule, this.getModule(), standardSelection);

		if (req.isSelectionExtensionRequired() && this.parsedExtendedSelection != null && this.parsedUserSelection != null) {
			this.pages.add(new ExtractMethodPreviewPage(name, this.req, this.requestProcessor));
		}
		this.pages.add(new ExtractMethodPage(name, this.requestProcessor));
	}

	@Override
	protected List<IChangeProcessor> getChangeProcessors() {
		List<IChangeProcessor> processors = new ArrayList<IChangeProcessor>();
		this.changeProcessor = new ExtractMethodChangeProcessor(this.name, this.req, this.requestProcessor);
		processors.add(changeProcessor);
		return processors;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		if (this.requestProcessor.getScopeAdapter() == null || this.requestProcessor.getScopeAdapter() instanceof IClassDefAdapter) {
			status.addFatalError(UITexts.extractMethodScopeInvalid);
			return status;
		}
		if (status.getEntries().length > 0)
			return status;

		if (parsedExtendedSelection == null && parsedUserSelection == null) {
			status.addFatalError(UITexts.extractMethodIncompleteSelection);
			return status;
		}
		return status;
	}

	private void validateSelections() {
		try {
			if (parsedUserSelection != null) {
				VisitorFactory.validateSelection(parsedUserSelection);
			}
		} catch (SelectionException e) {
			this.parsedUserSelection = null;
			if (parsedExtendedSelection == null) {
				status.addFatalError(e.getMessage());
				return;
			}
		}
		try {
			if (parsedExtendedSelection != null) {
				VisitorFactory.validateSelection(parsedExtendedSelection);
			}
		} catch (SelectionException e) {
			this.parsedExtendedSelection = null;
			if (parsedUserSelection == null) {
				status.addFatalError(e.getMessage());
				return;
			}
		}
	}

	public void setModule(ModuleAdapter module) {
		this.module = module;
	}

	public ModuleAdapter getModule() {
		return module;
	}
}
