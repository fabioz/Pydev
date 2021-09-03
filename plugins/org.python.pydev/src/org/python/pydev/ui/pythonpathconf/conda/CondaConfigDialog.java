package org.python.pydev.ui.pythonpathconf.conda;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.ast.interpreter_managers.PyDevCondaPreferences;
import org.python.pydev.shared_ui.field_editors.FileFieldEditorCustom;
import org.python.pydev.ui.pythonpathconf.ValidationFailedException;

public class CondaConfigDialog extends Dialog {

    private FileFieldEditor fileFieldEditor;
    private Text errorMessageText;
    private String condaExecLocation;

    public CondaConfigDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MAX);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Select conda executable");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite) super.createDialogArea(parent);
        Composite composite = new Composite(top, SWT.None);

        composite.setLayoutData(createGridData(1));

        int numberOfColumns = 2;

        fileFieldEditor = new FileFieldEditorCustom("unused", "Conda executable", composite);
        fileFieldEditor.fillIntoGrid(composite, numberOfColumns);
        fileFieldEditor.setChangeButtonText("...");
        fileFieldEditor.setEmptyStringAllowed(false);

        String path = "";
        File executable = PyDevCondaPreferences.getExecutable();
        if (executable != null) {
            path = executable.getAbsolutePath();
        }
        fileFieldEditor.setStringValue(path);

        errorMessageText = new Text(composite, SWT.READ_ONLY);
        errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
        GridData gridData = createGridData(numberOfColumns);
        gridData.heightHint = 30;
        errorMessageText.setLayoutData(gridData);

        composite.setLayout(new GridLayout(numberOfColumns, false));
        return top;
    }

    @Override
    protected void okPressed() {
        try {
            errorMessageText.setText("");
            this.condaExecLocation = check("conda executable location", fileFieldEditor);
            PyDevCondaPreferences.setExecutable(new File(this.condaExecLocation));
            this.additionalValidation();
            super.okPressed();
        } catch (ValidationFailedException e) {
            // Exception just for the flow.
        }
    }

    protected void additionalValidation() throws ValidationFailedException {
        // Subclasses may override for additional validation.
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
        return "Save";
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
        CondaConfigDialog dialog = new CondaConfigDialog(shell);
        dialog.open();
    }
}
