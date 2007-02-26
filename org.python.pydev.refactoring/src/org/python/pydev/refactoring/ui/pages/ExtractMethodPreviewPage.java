package org.python.pydev.refactoring.ui.pages;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.python.pydev.refactoring.coderefactoring.extractmethod.ExtractMethodRequestProcessor;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.ui.UITexts;
import org.python.pydev.refactoring.ui.controls.preview.PyPreview;

public class ExtractMethodPreviewPage extends UserInputWizardPage implements
		SelectionListener {

	private PyPreview userPreview;

	private PyPreview extendedPreview;

	private RefactoringInfo info;

	private ExtractMethodRequestProcessor requestProcessor;

	private Button userCheckbox;

	private Button extendedCheckbox;

	public ExtractMethodPreviewPage(String name, RefactoringInfo info,
			ExtractMethodRequestProcessor requestProcessor) {
		super(name);
		this.setTitle(name);
		this.info = info;
		this.requestProcessor = requestProcessor;
	}

	public void createControl(Composite parent) {	
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());	
		createLabelComp(main);	
		
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.numColumns = 2;

		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		Composite previewSelection = new Composite(main, SWT.NONE);
		previewSelection.setLayoutData(gridData);
		
		userPreview = createUserPreview(previewSelection);
		extendedPreview = createExtendedPreview(previewSelection);
		previewSelection.pack();

		this.userPreview.revealExtendedSelection(this.info.getUserSelection());
		this.extendedPreview.revealExtendedSelection(this.info.getExtendedSelection());
		this.userCheckbox.addSelectionListener(this);
		this.extendedCheckbox.addSelectionListener(this);

		previewSelection.setLayout(gridLayout2);

		setControl(main);
	}

	private void createLabelComp(Composite parent) {
		Composite functionSignatureComposite = new Composite(parent, SWT.NONE);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.makeColumnsEqualWidth = true;
		GridData compositeLData = new GridData();
		compositeLData.horizontalAlignment = GridData.FILL;
		compositeLData.grabExcessHorizontalSpace = true;
		functionSignatureComposite.setLayoutData(compositeLData);
		functionSignatureComposite.setLayout(compositeLayout);

		Label descriptionLabel = new Label(functionSignatureComposite, SWT.NONE);
		GridData labelLData = new GridData();
		labelLData.horizontalAlignment = GridData.FILL;
		labelLData.grabExcessHorizontalSpace = true;
		descriptionLabel.setLayoutData(labelLData);
		descriptionLabel.setText(UITexts.extractMethodSelectionPreviewLabel);
	}

	private PyPreview createUserPreview(Composite parent) {
		Composite sourceViewComposite = new Composite(parent, SWT.FLAT);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.makeColumnsEqualWidth = true;
		GridData compositeLData = new GridData();
		compositeLData.horizontalAlignment = GridData.FILL;
		compositeLData.grabExcessHorizontalSpace = true;
		compositeLData.grabExcessVerticalSpace = true;
		compositeLData.verticalAlignment = GridData.FILL;
		sourceViewComposite.setLayoutData(compositeLData);
		sourceViewComposite.setLayout(compositeLayout);

		userCheckbox = new Button(sourceViewComposite, SWT.RADIO);
		GridData labelLData = new GridData();
		labelLData.verticalAlignment = GridData.BEGINNING;
		userCheckbox.setLayoutData(labelLData);
		userCheckbox.setText("User selection");
		userCheckbox.setSelection(true);

		Composite previewComposite = new Composite(sourceViewComposite,
				SWT.NONE);

		FormLayout composite8Layout = new FormLayout();
		GridData composite8LData = new GridData();
		composite8LData.horizontalAlignment = GridData.FILL;
		composite8LData.grabExcessHorizontalSpace = true;
		composite8LData.grabExcessVerticalSpace = true;
		composite8LData.verticalAlignment = GridData.FILL;
		previewComposite.setLayoutData(composite8LData);
		previewComposite.setLayout(composite8Layout);

		return new PyPreview(previewComposite, this.info.getDocument());

	}

	private PyPreview createExtendedPreview(Composite parent) {
		Composite sourceViewComposite = new Composite(parent, SWT.FLAT);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.makeColumnsEqualWidth = true;
		GridData compositeLData = new GridData();
		compositeLData.horizontalAlignment = GridData.FILL;
		compositeLData.grabExcessHorizontalSpace = true;
		compositeLData.grabExcessVerticalSpace = true;
		compositeLData.verticalAlignment = GridData.FILL;
		sourceViewComposite.setLayoutData(compositeLData);
		sourceViewComposite.setLayout(compositeLayout);

		extendedCheckbox = new Button(sourceViewComposite, SWT.RADIO);
		GridData labelLData = new GridData();
		labelLData.verticalAlignment = GridData.BEGINNING;
		extendedCheckbox.setLayoutData(labelLData);
		extendedCheckbox.setText("Extended selection");

		Composite previewComposite = new Composite(sourceViewComposite,
				SWT.NONE);

		FormLayout composite8Layout = new FormLayout();
		GridData composite8LData = new GridData();
		composite8LData.horizontalAlignment = GridData.FILL;
		composite8LData.grabExcessHorizontalSpace = true;
		composite8LData.grabExcessVerticalSpace = true;
		composite8LData.verticalAlignment = GridData.FILL;
		previewComposite.setLayoutData(composite8LData);
		previewComposite.setLayout(composite8Layout);

		return new PyPreview(previewComposite, this.info.getDocument());
	}

	@Override
	public boolean canFlipToNextPage() {
		return true;
	}

	@Override
	public boolean isPageComplete() {
		return !isCurrentPage();
	}

	public void widgetDefaultSelected(SelectionEvent e) {
	}

	public void widgetSelected(SelectionEvent e) {
		Button button = (Button) e.widget;
		if (button == ExtractMethodPreviewPage.this.userCheckbox) {
			ExtractMethodPreviewPage.this.extendedCheckbox.setSelection(!button
					.getSelection());
		} else {
			ExtractMethodPreviewPage.this.userCheckbox.setSelection(!button
					.getSelection());
		}
		button.setSelection(true);
		updateRequestProcessor();
		ExtractMethodPage page = (ExtractMethodPage) getNextPage();
		page.setupComposite();
	}

	private void updateRequestProcessor() {	
		if (this.userCheckbox.getSelection()) {
			this.requestProcessor.initProcessor(this.info.getScopeAdapter(),
					info.getParsedUserSelection(), info.getUserSelection());
		} else {
			this.requestProcessor.initProcessor(info.getScopeAdapter(), info
					.getParsedExtendedSelection(), info.getExtendedSelection());
		}
	}
}
