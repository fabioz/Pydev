package com.python.pydev.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import com.python.pydev.PydevPlugin;

public class PydevExtensionNotifier extends Thread{
	//all times here are in secs
	private static final int FIRST_TIME = 60 * 30;
    private static final int VALIDATED_TIME = FIRST_TIME;
    private static final int MIN_TIME = 60 * 5;
    private boolean inMessageBox = false;

    public PydevExtensionNotifier() {
	}
	
	@Override
	public void run() {	
	    try {
	        sleep( FIRST_TIME * 1000); //whenever we start the plugin, the first xxx minutes are 'free'
	    } catch (Throwable e) {
	        e.printStackTrace();
	    }

        int seconds = MIN_TIME;
        while( true ) {
            try {
                sleep( seconds * 1000);
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
                                    MessageBox message = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
                                    message.setText("Pydev extensions.");
                                    message.setMessage("Pydev extensions:\nUnlicensed version.");
                                    message.open();
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
