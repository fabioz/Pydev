package com.python.pydev.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import com.python.pydev.PydevPlugin;

public class PydevExtensionNotifier extends Thread{
	private boolean validated;
	
	public PydevExtensionNotifier() {
		validated = false;
	}
	
	@Override
	public void run() {	
		while( !validated ) {
            try {
                sleep( 6000 );
                final Display disp = Display.getDefault();
                disp.asyncExec(new Runnable(){
                    public void run() {
                        IWorkbenchWindow window = PydevPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
                        Shell shell = window == null ? new Shell(disp) : window.getShell();
                        MessageBox message = new MessageBox( shell, SWT.OK | SWT.ICON_INFORMATION );
                        message.setText("Pydev extension");
                        message.setMessage("Trial version");                                    
                        message.open();
                    }
                });
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
	}
	
	public void setValidated( boolean valid ) {
		validated = valid;
	}
	public boolean isValid() {
		return validated;
	}
}
