package org.python.pydev.ui.pythonpathconf;

import java.io.File;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.nature.PipenvHelper;
import org.python.pydev.shared_ui.field_editors.FileFieldEditorCustom;
import org.python.pydev.shared_ui.field_editors.ProjectDirectoryFieldEditorCustom;
import org.python.pydev.ui.dialogs.PyDialogHelpers;

public class PipenvDialog extends Dialog {

    private FileFieldEditor fileFieldEditor;
    private Combo comboBaseInterpreter;
    private IInterpreterInfo[] interpreterInfos;
    private ProjectDirectoryFieldEditorCustom projectLocationFieldEditor;
    private Text errorMessageText;
    private String defaultPipenvLocation;
    private String defaultProjectLocation;
    private String projectLocation;
    private String pipenvLocation;
    private IInterpreterInfo baseInterpreter;
    private String dialogTitle;
    private boolean showBaseInterpreter;

    public PipenvDialog(Shell parentShell, IInterpreterInfo[] interpreterInfos, String defaultPipenvLocation,
            String defaultProjectLocation, IInterpreterManager interpreterManager, String dialogTitle,
            boolean showBaseInterpreter) {
        super(parentShell);
        this.showBaseInterpreter = showBaseInterpreter;
        this.dialogTitle = dialogTitle;
        setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MAX);
        Assert.isTrue(interpreterInfos != null, "IInterpreterInfo must not be null.");
        Assert.isTrue(interpreterInfos.length > 0, "Must pass at least one IInterpreterInfo.");
        this.interpreterInfos = interpreterInfos;
        if (defaultPipenvLocation == null) {
            defaultPipenvLocation = PipenvHelper.searchDefaultPipenvLocation(interpreterInfos[0], interpreterManager);
        }
        this.defaultPipenvLocation = defaultPipenvLocation == null ? "" : defaultPipenvLocation;
        this.defaultProjectLocation = defaultProjectLocation == null ? "" : defaultProjectLocation;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(dialogTitle);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite) super.createDialogArea(parent);
        Composite composite = new Composite(top, SWT.None);

        composite.setLayoutData(createGridData(1));

        int numberOfColumns = 2;

        projectLocationFieldEditor = new ProjectDirectoryFieldEditorCustom("unused", "Project location", composite);
        projectLocationFieldEditor.fillIntoGrid(composite, numberOfColumns);
        projectLocationFieldEditor.setChangeButtonText("...");
        projectLocationFieldEditor.setStringValue(defaultProjectLocation);
        projectLocationFieldEditor.setEmptyStringAllowed(false);

        if (this.showBaseInterpreter) {
            Label label = new Label(composite, SWT.NONE);
            label.setText("Base Interpreter:");
            label.setLayoutData(createGridData(numberOfColumns));

            comboBaseInterpreter = new Combo(composite, SWT.READ_ONLY);
            comboBaseInterpreter.setLayoutData(createGridData(numberOfColumns));
            for (IInterpreterInfo info : interpreterInfos) {
                comboBaseInterpreter.add(info.getNameForUI());
                comboBaseInterpreter.setData(info.getNameForUI(), info);
            }
            comboBaseInterpreter.setText(interpreterInfos[0].getNameForUI());
        }

        fileFieldEditor = new FileFieldEditorCustom("unused", "pipenv executable", composite);
        fileFieldEditor.fillIntoGrid(composite, numberOfColumns);
        fileFieldEditor.setChangeButtonText("...");
        fileFieldEditor.setEmptyStringAllowed(false);
        fileFieldEditor.setStringValue(defaultPipenvLocation);

        errorMessageText = new Text(composite, SWT.READ_ONLY);
        errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
        GridData gridData = createGridData(numberOfColumns);
        gridData.heightHint = 60;
        errorMessageText.setLayoutData(gridData);

        composite.setLayout(new GridLayout(numberOfColumns, false));
        return top;
    }

    @Override
    protected void okPressed() {
        try {
            errorMessageText.setText("");
            String stringValue = projectLocationFieldEditor.getStringValue();
            if (stringValue != null && !stringValue.trim().isEmpty()) {
                File file = new File(stringValue.trim());
                if (!file.exists()) {
                    int openQuestionWithChoices = PyDialogHelpers.openQuestionWithChoices("Directory does not exist",
                            "Directory " + stringValue.trim() + " does not exist.\n\nHow do you want to proceed?",
                            "Create directory", "Don't create directory");
                    if (openQuestionWithChoices == 0) {
                        file.mkdirs();
                        projectLocationFieldEditor.refreshValidState();
                    }
                }
            }
            this.projectLocation = check("Project location", projectLocationFieldEditor);
            this.pipenvLocation = check("pipenv location", fileFieldEditor);
            if (this.showBaseInterpreter) {
                this.baseInterpreter = (IInterpreterInfo) comboBaseInterpreter.getData(comboBaseInterpreter.getText());
            }
            this.additionalValidation();
            super.okPressed();
        } catch (ValidationFailedException e) {
            // Exception just for the flow.
        }
    }

    protected void additionalValidation() throws ValidationFailedException {
        // Subclasses may override for additional validation.
    }

    public String getProjectLocation() {
        return this.projectLocation;
    }

    public String getPipenvLocation() {
        return pipenvLocation;
    }

    public IInterpreterInfo getBaseInterpreter() {
        return baseInterpreter;
    }

    private String check(String title, StringButtonFieldEditor fileFieldEditor) throws ValidationFailedException {
        if (!fileFieldEditor.isValid()) {
            String errorMessage = fileFieldEditor.getErrorMessage();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                final String msg = title + ": " + errorMessage;
                setErrorMessage(msg);
                throw new ValidationFailedException();
            }
        }
        return fileFieldEditor.getStringValue();
    }

    protected void setErrorMessage(final String msg) {
        errorMessageText.setText(msg);
    }

    @Override
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        if (id == IDialogConstants.OK_ID) {
            label = getOkButtonText();
        }
        Button button = super.createButton(parent, id, label, defaultButton);
        return button;
    }

    protected String getOkButtonText() {
        return "Create Pipenv interpreter";
    }

    private GridData createGridData(int horizontalSpan) {
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = horizontalSpan;
        gridData.widthHint = 500;
        return gridData;
    }

    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        PipenvDialog dialog = new PipenvDialog(shell, new IInterpreterInfo[0], null, null, null,
                "New Pipenv interpreter", true);

        dialog.open();
    }
}
