/*
 * Created on Oct 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.python.pydev.plugin.PydevPrefs;

/**
 * @author Fabio Zadrozny
 */
public class RunManyDialog extends Dialog implements Listener {

    private String root;

    private Text textRootFolder;

    private Text textFilesThatMatch;

    private Text textWorkingDir;

    private Text textInterpreter;

    private Button check;

    private Text textScriptLocation;

    private Text textScriptArgs;

    //output gotten here.
    public String rootFolder;

    public String files;

    public String interpreter;

    public String working;

    public String scriptArgs;

    public String scriptLocation;

    public boolean scriptSelected;

    private Label labelScript0;

    private Label labelScript1;

    private Label labelFilesThatMatch;

    /**
     * @param parentShell
     */
    public RunManyDialog(Shell parentShell, String root) {
        super(parentShell);

        setShellStyle(getShellStyle() | SWT.RESIZE);
        this.root = root;
    }

    protected Control createDialogArea(Composite parent) {
        // create a composite with standard margins and spacing
        Composite composite = (Composite) super.createDialogArea(parent);

        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;

        Label label0 = new Label(composite, 0);
        label0
                .setText("Two runs can be specified now...\n"
                        + "If a script is not specified, each file to be executed will have its own shell and output.\n"
                        + "If a script is specified, it should receive the root folder and the args as parameters,\n"
                        + "and it should take the responsibility for selecting the files and executing each one.\n"
                        + "This can be very useful if many tests should be run with a single output (and for instance,\n"
                        + "you want to execute only unit-tests and see the output in a formated way).");
        label0.setLayoutData(layoutData);

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;

        Label label = new Label(composite, 0);
        label.setText("Root folder:");
        label.setLayoutData(layoutData);

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        textRootFolder = new Text(composite, SWT.SINGLE);
        textRootFolder.setText(root);
        textRootFolder.setLayoutData(layoutData);

        //-------
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        labelFilesThatMatch = new Label(composite, 0);
        labelFilesThatMatch.setText("Files that match (see JAVA regular expression):");
        labelFilesThatMatch.setLayoutData(layoutData);

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        textFilesThatMatch = new Text(composite, SWT.SINGLE);
        textFilesThatMatch.setText("test.*\\.py");
        textFilesThatMatch.setLayoutData(layoutData);

        //--------
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        Label label3 = new Label(composite, 0);
        label3.setText("Working Dir:");
        label3.setLayoutData(layoutData);

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        textWorkingDir = new Text(composite, SWT.SINGLE);
        textWorkingDir.setText(this.root);
        textWorkingDir.setLayoutData(layoutData);

        //-------
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        Label label4 = new Label(composite, 0);
        label4.setText("Interpreter");
        label4.setLayoutData(layoutData);

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        textInterpreter = new Text(composite, SWT.SINGLE);
        textInterpreter.setText(PydevPrefs.getDefaultInterpreter());
        textInterpreter.setLayoutData(layoutData);

        //-------
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        check = new Button(composite, SWT.CHECK);
        check.setText("Use script that receives: RootFolder args");
        check.setLayoutData(layoutData);

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        labelScript0 = new Label(composite, 0);
        labelScript0.setText("Script location (absolute)");
        labelScript0.setLayoutData(layoutData);

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        textScriptLocation = new Text(composite, SWT.SINGLE);
        textScriptLocation.setText("X:\\coilib30\\tools\\runtests.py");
        textScriptLocation.setLayoutData(layoutData);

        //-------
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        labelScript1 = new Label(composite, 0);
        labelScript1.setText("Aditional args for script.");
        labelScript1.setLayoutData(layoutData);

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        textScriptArgs = new Text(composite, SWT.SINGLE);
        textScriptArgs.setLayoutData(layoutData);

        check.addListener(SWT.Selection, this);
        check.setSelection(true);
        this.showDependingOnCheck(true);

        return composite;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    protected void okPressed() {
        rootFolder = textRootFolder.getText();
        files = textFilesThatMatch.getText();
        interpreter = textInterpreter.getText();
        working = textWorkingDir.getText();

        scriptArgs = textScriptArgs.getText();
        scriptLocation = textScriptLocation.getText();
        scriptSelected = check.getSelection();
        // TODO Auto-generated method stub
        super.okPressed();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    public void handleEvent(Event e) {
        Widget source = e.widget;
        if (source == check) {
            boolean sel = check.getSelection();
            showDependingOnCheck(sel);
        }
    }

    /**
     * @param sel
     */
    private void showDependingOnCheck(boolean sel) {
        textScriptLocation.setVisible(sel);
        textScriptArgs.setVisible(sel);
        labelScript0.setVisible(sel);
        labelScript1.setVisible(sel);
        labelFilesThatMatch.setVisible(!sel);
        textFilesThatMatch.setVisible(!sel);
    }
}