/*
 * Created on Jan 6, 2006
 */
package com.python.pydev.ui;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.docutils.WordUtils;

import com.python.pydev.license.ClientEncryption;
import com.python.pydev.util.EnvGetter;
import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.smtp.SMTPTransport;

public class GetInfoDialog extends Dialog {
    

    private static final int REGULAR_COLS = 125;
    private static final int BOLD_COLS = 95;
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
        
        String msg = "If you already paid for 'Pydev Extensions', please follow the instructions below in 'Request license'. If you still " +
                "haven't, please go to xxxxxxxxxx and follow the instructions so that you can request your license for 'Pydev Extensions'.\n\n";
        
        String msg1 = "Request license:\n";
        String msg2 = "Fill your complete name and e-mail (it MUST be the same e-mail you used in paypal) " +
                "and press 'Get Information'. If everything " +
                "succeeds, press 'Send Information'. You'll receive your license through the e-mail you specified within 2 work days.\n\n";
        
        String msg3a = "Contact:\n";
        String msg3 = "If you have some reason to believe that something went wrong, haven't received " +
                "your license within 2 work days, or have any doubts, please e-mail: fabiofz@gmail.com with the information that appears below.\n\n";
        
        String msg4 = "Note:\n";
        String msg4a = "The license will only be valid for your current installation. If you need to install it in another computer, you'll have " +
                "to install 'Pydev Extensions' in the other computer and follow the same steps again.";
        
        MainExtensionsPreferencesPage.setLabelBold(composite, createLabel(composite, WordUtils.wrap(msg, BOLD_COLS), 2));
        MainExtensionsPreferencesPage.setLabelBold(composite, createLabel(composite, WordUtils.wrap(msg1, BOLD_COLS), 2));
        createLabel(composite, WordUtils.wrap(msg2, REGULAR_COLS), 2);
        MainExtensionsPreferencesPage.setLabelBold(composite, createLabel(composite, WordUtils.wrap(msg3a, BOLD_COLS), 2));
        createLabel(composite, WordUtils.wrap(msg3, REGULAR_COLS), 2);
        MainExtensionsPreferencesPage.setLabelBold(composite, createLabel(composite, WordUtils.wrap(msg4, BOLD_COLS), 2));
        createLabel(composite, WordUtils.wrap(msg4a, REGULAR_COLS), 2);
        
        createLabel(composite, "Name (complete)"); 
        textName = createText(composite);
        
        createLabel(composite, "e-mail"); 
        textEmail = createText(composite);
        
        buttonGetInfo = new Button(composite, SWT.NONE);
        buttonGetInfo.setText("Get Information");
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        buttonGetInfo.setLayoutData(gridData);
        buttonGetInfo.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {

                try {
                    String name = getName();
                    String email = getEmail();
                    
                    
                    Properties envVariables = new Properties();
                    
                    envVariables.put("name", name);
                    envVariables.put("e-mail", email);
                    envVariables.put("time", getTime());
                    String cEnc = ClientEncryption.getInstance().encrypt(EnvGetter.getPropertiesStr(envVariables));
                    String enc = WordUtils.wrap(cEnc, 80, null, true);
                    
                    String info = "<info purpose=\"Pydev Extensions license request\">\n<name>"+name+"</name>\n<e-mail>"+email+"</e-mail>\n<aditional_info>"+enc+"</aditional_info></info>";
                    textInfo.setText(info);
                    
                } catch (Exception x) {
                    textInfo.setText("Unable to get information. Reason:"+x.getMessage());
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }});

        textInfo = new Text(composite, SWT.MULTI | SWT.BORDER);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        gridData.grabExcessVerticalSpace = true;
        gridData.grabExcessHorizontalSpace = true;
        textInfo.setLayoutData(gridData);
        
        buttonSendInfo = new Button(composite, SWT.NONE);
        buttonSendInfo.setText("Send information");
        buttonSendInfo.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                try {
                    String info = textInfo.getText();
                    if(info == null || info.length() == 0){
                        throw new RuntimeException("Information not generated.");
                    }
                    if(info.indexOf("<aditional_info>") == -1 || info.indexOf("</aditional_info>") == -1){
                        System.out.println(info);
                        throw new RuntimeException("The current information is invalid.\nPlease regenerate it.");
                    }
                    send("esss.com.br", 25, "pydev@pydev.com.br", "fabioz@esss.com.br", "PYDEV: LICENSE REQUEST", info);
                    MessageDialog.openInformation(getShell(), "Success Message", "Information sent successfully.");
                } catch (Exception x) {
                    MessageDialog.openError(getShell(), "Error", x.getMessage());
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }});
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        buttonSendInfo.setLayoutData(gridData);
        getShell().setSize(640, 640);
        setCentered();
        return composite;
    }
    
    
    public static void send(String smtpHost, int smtpPort, String from, String to, String subject, String content) throws AddressException, MessagingException {
        // Create a mail session
        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", "" + smtpPort);
        Session session = Session.getDefaultInstance(props, null);
        
        // Construct the message
        SMTPMessage msg = new SMTPMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(subject);
        msg.setText(content);

        // Send the message
        SMTPTransport.send(msg);
    }

    private String validate(String text, String label) {
        if (text == null || text.trim().length() == 0){
            throw new RuntimeException("The "+label+" must not be empty.");
        }
        return text;
    }
    

    protected String getName() {
        return validate(textName.getText(), "name");
    }
    
    protected String getTime() {
        return validate(""+System.currentTimeMillis(), "time");
    }

    protected String getEmail() {
        String validated = validate(textEmail.getText(), "e-mail");
        if(validated.indexOf('@')  == -1){
            throw new RuntimeException("The e-mail specified is not a valid e-mail address.");
        }
        return validated;
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

    private Label createLabel(Composite composite, String labelMsg) {
        return createLabel(composite, labelMsg, 1);
    }
    /**
     * @param composite
     * @param labelMsg 
     * @return 
     */
    private Label createLabel(Composite composite, String labelMsg, int colSpan) {
        Label label= new Label(composite, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = colSpan;
        label.setLayoutData(gridData);
        label.setText(labelMsg);
        return label;
    }
    
}
