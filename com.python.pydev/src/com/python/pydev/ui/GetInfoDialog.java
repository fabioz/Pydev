/*
 * Created on Jan 6, 2006
 */
package com.python.pydev.ui;

import java.math.BigInteger;

import org.eclipse.jface.dialogs.Dialog;
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
        
        String msg = "If you already paid for 'Pydev Extensions', please follow the instructions below in 'Request license'. If you still " +
                "haven't paid for it, please go to xxxxxxxxxx and follow the instructions to pay for your copy of 'Pydev Extensions'.\n\n";
        
        String msg1 = "Request license:\n";
        String msg2 = "Fill your complete name and e-mail (it MUST be the same e-mail you used in paypal) " +
                "and press 'Get Information'. If everything " +
                "succeeds, press 'Send Information'. You'll receive your license through the e-mail you specified within 2 work days.\n\n";
        
        String msg3a = "Contact:\n";
        String msg3 = "If you have some reason to believe that something went wrong, or you haven't received " +
                "your license within 2 work days, please e-mail: fabiofz at gmail.com\n\n";
        
        MainExtensionsPreferencesPage.setLabelBold(composite, createLabel(composite, WordUtils.wrap(msg, 80), 2));
        MainExtensionsPreferencesPage.setLabelBold(composite, createLabel(composite, WordUtils.wrap(msg1, 80), 2));
        createLabel(composite, WordUtils.wrap(msg2, 100), 2);
        MainExtensionsPreferencesPage.setLabelBold(composite, createLabel(composite, WordUtils.wrap(msg3a, 80), 2));
        createLabel(composite, WordUtils.wrap(msg3, 100), 2);
        
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
                
                class ServerEncryption  {
                    private BigInteger d;
                    private BigInteger N;
                    
                    public ServerEncryption() {
                        N = new BigInteger("115177032176946546558269068827440200244040503869596632334637862913980482577252368423165152466486515398576152630074226512838661350005676884681271881673730676993314466894521803768688453811901029052598776873607299993786360160003193977375556220882426365859708520873206921482917525578030271496655309864011180862013",10);
                        d = new BigInteger("38102953059010605855163215912350748149763372818357944148302233941712480625561264625824246923507242326571700950190568763062926541182432527225455172903369918321676003827609677610585495947295281799820243985598929226902590068459606241673933576135176093581307291078797473438808817354561704810596368627085064805953",10);
                    }
                    
                    public String encrypt(String data) {
                        BigInteger m = new BigInteger( data.getBytes() );
                        BigInteger encrypted = m.modPow( d, N ); 
                        return new String( encrypted.toString() );
                    }

                    public String decrypt(String data) {
                        BigInteger c = new BigInteger( data );
                        BigInteger decrypted = c.modPow( d, N );
                        return new String( decrypted.toByteArray() );
                    }
                }

                try {
                    String originalEnvVars = EnvGetter.getEnvVariables();
                    System.out.println(">>"+originalEnvVars+"<<");
                    String envVariables = getName() + getEmail() + originalEnvVars;
                    String cEnc = ClientEncryption.getInstance().encrypt(envVariables);
                    System.out.println(cEnc);
                    System.out.println(new ServerEncryption().decrypt(cEnc));
                    String enc = WordUtils.wrap(cEnc, 80, null, true);
                    textInfo.setText(envVariables + "\n\n\n" + enc);
                    
                    cEnc = ClientEncryption.getInstance().encrypt(originalEnvVars);
                    System.out.println(cEnc);
                    System.out.println(new ServerEncryption().decrypt(cEnc));
                    
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
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        buttonSendInfo.setLayoutData(gridData);
        getShell().setSize(640, 640);
        setCentered();
        return composite;
    }
    
    private String validate(String text, String label) {
        if (text == null || text.trim().length() == 0){
            throw new RuntimeException("The "+label+" must not be empty.");
        }
        return label+"="+text+"\n";
    }
    

    protected String getName() {
        return validate(textName.getText(), "name");
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
