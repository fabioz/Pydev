package com.python.pydev.util;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.python.pydev.core.docutils.WordUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.ui.UIConstants;
import org.python.pydev.utils.CounterThread;
import org.python.pydev.utils.ICallback;

import com.python.pydev.PydevPlugin;
import com.python.pydev.ui.MainExtensionsPreferencesPage;



final class DialogNotifier extends Dialog{
    
    private static final int NUMBER_OF_SECS_TO_ENABLE_BUTTON = 2;
	private static final int BOLD_COLS = 120;
	private Label label;

    public DialogNotifier(Shell shell) {
        super(shell);
        setShellStyle(getShellStyle()| SWT.RESIZE | SWT.MAX);
    }

    @Override
    public boolean close() {
    	//do nothing
    	return false;
    }
    
    @Override
    protected Point getInitialSize() {
        return new Point(800,600);
    }

    protected Control createDialogArea(Composite parent) {
        Composite composite= (Composite) super.createDialogArea(parent);

        GridData gridData = null;
        GridLayout layout = (GridLayout) composite.getLayout();
        layout.numColumns = 1;

        
        String msg = "Unlicensed version of Pydev Extensions.";
        MainExtensionsPreferencesPage.setLabelBold(composite, createLabel(composite, WordUtils.wrap(msg, BOLD_COLS), 1));


        try {
            final String html = "<html><head>"+
            "<base href=\"http://www.fabioz.com/pydev/\" >"+
//          "<base href=\"file://D:/eclipse_workspace/com.python.pydev.docs/new_homepage/final/\" >"+
//          "<base href=\"file://E:/eclipse_workspace/com.python.pydev.docs/new_homepage/final/\" >"+
            "<title>Licensing Pydev Extensions and other info</title></head>"+
            "<body>" +
            "<strong>Thank you for evaluating Pydev Extensions. </strong><br/><br/>" +
            "If you wish to license Pydev Extensions, please visit the link " +
            "below for instructions on how to buy your license:<br/><br/>" +
            "" +
            "<a href=\"buy.html\"><strong>Buy</strong></a>" +
            "<br/>" +
            "<br/>" +
            "When you license it, not only will you get rid of this dialog, " +
            "but you will also help fostering the Pydev Open Source version.<br/><br/>" +
            "" +
            "If you wish more information on the available features, you can check it on the link below:<br/><br/>" +
            "" +
            "<a href=\"manual_adv_features.html\"><strong>Features Matrix</strong></a><br/><br/>" +
            "" +
            "Or you may browse the homepage starting at its initial page:<br/><br/>" +
            "<a href=\"index.html\"><strong>Pydev Extensions Main Page</strong></a><br/><br/>" +
            
            "</body></html>";
    		ToolBar navBar = new ToolBar(composite, SWT.NONE);
    		//this is the place where it might fail
    		final Browser browser = new Browser(composite, SWT.BORDER);
    		browser.setText(html);
    		gridData = new GridData(GridData.FILL_BOTH);
    		browser.setLayoutData(gridData);

    		final ToolItem back = new ToolItem(navBar, SWT.PUSH);
    		back.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.BACK));
    		
    		final ToolItem forward = new ToolItem(navBar, SWT.PUSH);
    		forward.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.FORWARD));
    		
    		final ToolItem stop = new ToolItem(navBar, SWT.PUSH);
    		stop.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.STOP));
    		
    		final ToolItem refresh = new ToolItem(navBar, SWT.PUSH);
    		refresh.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.REFRESH));

    		final ToolItem home = new ToolItem(navBar, SWT.PUSH);
    		home.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.HOME));
    		
    		back.addListener(SWT.Selection, new Listener() {
    			public void handleEvent(Event event) {
    				browser.back();
    			}
    		});
    		forward.addListener(SWT.Selection, new Listener() {
    			public void handleEvent(Event event) {
    				browser.forward();
    			}
    		});
    		stop.addListener(SWT.Selection, new Listener() {
    			public void handleEvent(Event event) {
    				browser.stop();
    			}
    		});
    		refresh.addListener(SWT.Selection, new Listener() {
    			public void handleEvent(Event event) {
    				browser.refresh();
    			}
    		});
    		home.addListener(SWT.Selection, new Listener() {
    			public void handleEvent(Event event) {
    				browser.setText(html);
    			}
    		});

    		
        } catch (Throwable e) {
            //some error might happen creating it according to the docs, so, let's put another text into the widget
            String msg2 = "Thank you for evaluating Pydev Extensions.\n\n" +
                    "If you wish to license Pydev Extensions, please visit:\n\n" +
                    "http://www.fabioz.com/pydev/buy.html.\n\n" +
                    "When you license it, not only will you get rid of this dialog, " +
                    "but you will also help fostering the Pydev Open Source version.\n\n\n\n\n" +
                    "If you wish more information on the available features, you can check it on the link below:\n\n" +
                    "http://www.fabioz.com/pydev/manual_adv_features.html.\n\n" +
                    "Or you may browse the homepage starting at its initial page:\n\n" +
                    "http://www.fabioz.com/pydev/index.html.\n\n" +
                    "";
            createText(composite, msg2, 1);
        }

        //set the counter to enable the button.
    	new CounterThread(new ICallback(){

			public Object call(Object args) {
				final int call = (Integer) args;
					
                final Display disp = Display.getDefault();
                if(disp != null){
                    disp.asyncExec(new Runnable(){
                        public void run() {
							try {
								if(call == NUMBER_OF_SECS_TO_ENABLE_BUTTON-1){
									Button button = getButton(IDialogConstants.OK_ID);
									if(button != null){
										button.setEnabled(true);
									}
									label.setText("");
								}else{
									label.setText("Enabling button in..."+ (NUMBER_OF_SECS_TO_ENABLE_BUTTON - call-1));
								}
							} catch (Exception e) {
								Log.log(e);
							}
                        }
                    });
				}else{
					//let's close it, otherwise we might be with it forever...(altought this should never happen).
					doClose();
				}
				return null;
			}
    		
    	}, 1000, NUMBER_OF_SECS_TO_ENABLE_BUTTON).start();
        return composite;
    }
    
    public boolean doClose(){
    	return super.close();
    }
    
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
    	label = createLabel(parent, "Enabling button in..."+NUMBER_OF_SECS_TO_ENABLE_BUTTON, 1);
        Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        button.addSelectionListener(new SelectionListener(){

			public void widgetSelected(SelectionEvent e) {
				doClose();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
        	
        });
        button.setEnabled(false);
    }

    /**
     * @param composite
     * @param labelMsg 
     * @return 
     */
    private Text createText(Composite composite, String labelMsg, int colSpan) {
        Text text= new Text(composite, SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = colSpan;
        text.setLayoutData(gridData);
        text.setText(labelMsg);
        return text;
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
public class PydevExtensionNotifier extends Thread{
    
	//all times here are in secs
	private static final int FIRST_TIME = 60*30;
    private static final int VALIDATED_TIME = 60 * 120;
    private static final int MIN_TIME = 60 * 120;
    private boolean inMessageBox = false;

    public PydevExtensionNotifier() {
        setName("Looping");
        setDaemon(true);
	}
	
	@Override
	public void run() {	
        boolean firstTime = true;

        int seconds = MIN_TIME;
        while( true ) {
            try {
                if(firstTime){
                    firstTime = false;
                    sleep( FIRST_TIME * 1000); //whenever we start the plugin, the first xxx minutes are 'free'
                }else{
                    sleep( seconds * 1000L);
                }
                
                boolean validated = PydevPlugin.getDefault().isValidated();
                if(!validated){
                    seconds = MIN_TIME;
                    final Display disp = Display.getDefault();
                    disp.asyncExec(new Runnable(){
                        public void run() {
                            if(!inMessageBox){
                                inMessageBox = true;
                                try {
                                    IWorkbenchWindow window = PydevPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
                                    Shell shell = (window == null) ? new Shell(disp) : window.getShell();
                                    DialogNotifier notifier = new DialogNotifier(shell);
                                    notifier.setBlockOnOpen(true);
                                    notifier.open();
                                    
//                                    MessageBox message = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
//                                    message.setText("Pydev extensions.");
//                                    String msg = "Pydev extensions:\nUnlicensed version.\n\n";
//                                    msg += "To license Pydev Extensions, follow the instructions at \n" +
//                                            "http://www.fabioz.com/pydev/buy.html";
//                                    message.setMessage(msg);
//                                    message.open();
                                } finally {
                                    inMessageBox = false;
                                }
                            }
                        }
                    });
                }else{
                    seconds = VALIDATED_TIME; //make this less often... no need to spend too much time on validation once the user has validated once
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        
	}
	
}
