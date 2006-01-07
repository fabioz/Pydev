/*
 * Created on Jan 6, 2006
 */
package com.python.pydev.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class GetInfoDialog extends Dialog {
    

    private Text textEmail;
    private Text textName;
    private Button buttonGetInfo;
    private Button buttonSendInfo;
    private Text textInfo;

    public GetInfoDialog(Shell shell) {
        super(shell);
        setShellStyle(getShellStyle()| SWT.RESIZE);
    }

    protected Control createDialogArea(Composite parent) {
        Composite composite= (Composite) super.createDialogArea(parent);


        GridData gridData = null;
        GridLayout layout = (GridLayout) composite.getLayout();
        layout.numColumns = 2;
        
        createLabel(composite, "Name (complete)"); 
        textName = createText(composite);
        
        createLabel(composite, "e-mail"); 
        textEmail = createText(composite);
        
        buttonGetInfo = new Button(composite, SWT.NONE);
        buttonGetInfo.setText("Get information");
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        buttonGetInfo.setLayoutData(gridData);

        textInfo = new Text(composite, SWT.MULTI | SWT.BORDER);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        gridData.grabExcessVerticalSpace = true;
        gridData.grabExcessHorizontalSpace = true;
        textInfo.setLayoutData(gridData);
        
        buttonSendInfo = new Button(composite, SWT.NONE);
        buttonSendInfo.setText("Send information");
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        buttonSendInfo.setLayoutData(gridData);
        getShell().setSize(640, 640);
        setCentered();
        return composite;
    }
    

    private void setCentered() {
        Display display = getShell().getDisplay();
        int width = display.getClientArea().width;
        int height = display.getClientArea().height;
        getShell().setLocation(((width - getShell().getSize().x) / 2) + display.getClientArea().x, ((height - getShell().getSize().y) / 2) + display.getClientArea().y);
    }

    protected void createButtonsForButtonBar(Composite parent) {
        //no default create button
    }
    
    /**
     * @param composite
     * @return 
     */
    private Text createText(Composite composite) {
        Text text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return text;
    }

    /**
     * @param composite
     * @param labelMsg 
     */
    private void createLabel(Composite composite, String labelMsg) {
        Label label= new Label(composite, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        label.setText(labelMsg);
    }
    
}
