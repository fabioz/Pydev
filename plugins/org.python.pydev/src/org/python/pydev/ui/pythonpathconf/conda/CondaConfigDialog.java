package org.python.pydev.ui.pythonpathconf.conda;

import org.eclipse.core.runtime.Assert;
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
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.field_editors.FileFieldEditorCustom;
import org.python.pydev.ui.pythonpathconf.ValidationFailedException;
import org.python.pydev.ui.pythonpathconf.package_manager.CondaPackageManager;

public class CondaConfigDialog extends Dialog {

    private FileFieldEditor fileFieldEditor;
    private IInterpreterInfo[] interpreterInfos;
    private Text errorMessageText;
    private String condaExecLocation;

    public CondaConfigDialog(Shell parentShell, IInterpreterInfo[] interpreterInfos) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MAX);
        Assert.isTrue(interpreterInfos != null, "IInterpreterInfo must not be null.");
        Assert.isTrue(interpreterInfos.length > 0, "Must pass at least one IInterpreterInfo.");
        this.interpreterInfos = interpreterInfos;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Conda executable selection dialog");
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
        try {
            path = PyDevCondaPreferences.getExecutablePath(
                    new CondaPackageManager(interpreterInfos[0], interpreterInfos[0].getCondaPrefix()));
        } catch (UnableToFindExecutableException e) {
            Log.log(e);
        }

        fileFieldEditor.setStringValue(path);

        errorMessageText = new Text(composite, SWT.READ_ONLY);
        errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
        GridData gridData = createGridData(numberOfColumns);
        gridData.heightHint = 0;
        errorMessageText.setLayoutData(gridData);

        composite.setLayout(new GridLayout(numberOfColumns, false));
        return top;
    }

    @Override
    protected void okPressed() {
        try {
            errorMessageText.setText("");
            this.condaExecLocation = check("conda executable location", fileFieldEditor);
            PyDevCondaPreferences.setStoredExecutablePath(this.condaExecLocation);
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
        CondaConfigDialog dialog = new CondaConfigDialog(shell, new IInterpreterInfo[0]);
        dialog.open();
    }
}
