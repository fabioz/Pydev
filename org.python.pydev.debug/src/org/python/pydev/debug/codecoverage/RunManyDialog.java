/*
 * Created on Oct 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Fabio Zadrozny
 */
public class RunManyDialog extends Dialog{

    private String root;
    public Text textRootFolder;
    public Text textFilesThatMatch;
    public Text textWorkingDir;
    public Text textInterpreter;
    public String rootFolder;
    public String files;
    public String interpreter;
    public String working;

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
        Label label2 = new Label(composite, 0);
        label2.setText("Files that match:");
        label2.setLayoutData(layoutData);

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        textFilesThatMatch = new Text(composite, SWT.SINGLE);
        textFilesThatMatch.setText("test*.py");
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
        textInterpreter.setText("python");
        textInterpreter.setLayoutData(layoutData);

        return composite;
	}

    
    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    protected void okPressed() {
	    rootFolder = textRootFolder.getText();
	    files = textFilesThatMatch.getText();
	    interpreter = textInterpreter.getText();
	    working = textWorkingDir.getText();
        // TODO Auto-generated method stub
        super.okPressed();
    }
}
