package org.python.pydev.refactoring.ui.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.python.pydev.refactoring.coderefactoring.extractmethod.ExtractMethodRequestProcessor;
import org.python.pydev.refactoring.ui.model.table.SimpleTableItem;
import org.python.pydev.refactoring.ui.pages.extractmethod.ExtractMethodComposite;
import org.python.pydev.refactoring.ui.pages.listener.IValidationPage;

public class ExtractMethodPage extends UserInputWizardPage implements IValidationPage {

	public ExtractMethodRequestProcessor requestProcessor;

	private ExtractMethodComposite extractComposite;

	private Composite parent;

	public ExtractMethodPage(String name, ExtractMethodRequestProcessor requestProcessor) {
		super(name);
		this.setTitle(name);
		this.requestProcessor = requestProcessor;
	}

	public void createControl(Composite parent) {
		this.parent = parent;
		setupComposite();
	}

	public void setupComposite() {
		if (extractComposite != null) {
			extractComposite.dispose();
			extractComposite = null;
		}
		boolean hasArguments = this.requestProcessor.getDeducer().getParameters().size() > 0;

		extractComposite = new ExtractMethodComposite(this, parent, hasArguments, requestProcessor.getScopeAdapter());

		extractComposite.registerListeners(this);
		updateArgumentTable();
		setControl(this.extractComposite);

		voodooResizeToPage();
		setPageComplete(false);
	}

	private void voodooResizeToPage() {
		Point size = getShell().getSize();
		size.x += 1;
		size.y += 1;
		getShell().setSize(size);
		getShell().layout(true);
		size.x -= 1;
		size.y -= 1;
		getShell().setSize(size);
		getShell().layout(true);
	}

	public void updateArgumentTable() {
		if (extractComposite != null && extractComposite.getArgumentsTable() != null) {
			extractComposite.initTable(requestProcessor.getDeducer().getParameters());
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	public void validate() {
		setErrorMessage(null);
		extractComposite.validate();
		setPageComplete(getErrorMessage() == null);
		if (isPageComplete()) {
			applySettings();
		}
	}

	private void applySettings() {
		this.requestProcessor.setMethodName(extractComposite.getFunctionName());
		this.requestProcessor.setOffsetStrategy(extractComposite.getOffsetStrategy());

		if (extractComposite.getArgumentsTable() != null) {
			List<String> parameterOrder = new ArrayList<String>();
			Map<String, String> parameterMap = new HashMap<String, String>();
			for (TableItem item : extractComposite.getArgumentsTable().getItems()) {
				if (item instanceof SimpleTableItem) {
					SimpleTableItem tableItem = (SimpleTableItem) item;
					parameterMap.put(tableItem.getOriginalName(), tableItem.getText());
					parameterOrder.add(tableItem.getOriginalName());
				}
			}
			requestProcessor.setParameterMap(parameterMap);
			requestProcessor.setParameterOrder(parameterOrder);
		}
	}

	public void handleEvent(Event event) {
		validate();
	}
}
