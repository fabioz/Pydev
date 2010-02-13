package com.leosoto.bingo.debug.ui.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.ui.actions.project.PyRemoveNature;

import com.leosoto.bingo.debug.ui.launching.DjangoManagementRunner;

public class DjangoCreateApp extends PyRemoveNature implements IDebugEventSetListener { 

	private ILaunch launch;
	
    public void run(IAction action) {
    	showAppNameDialog();
    }
    
    private void createApp(String name) {      	 
    	try {    		
    		// This is to refresh workspace after completion:
    		DebugPlugin.getDefault().addDebugEventListener(this);
    		
    		launch = DjangoManagementRunner.launch(selectedProject, "startapp " + name);
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
    private void showAppNameDialog() {
		final Shell dialog = new Shell (SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText("Specify new app name");
		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = 10;
		formLayout.marginHeight = 10;
		formLayout.spacing = 10;
		dialog.setLayout (formLayout);
		
		Label label = new Label(dialog, SWT.NONE);
		label.setText("Name:");
		FormData data = new FormData ();
		label.setLayoutData (data);
		
		Button cancel = new Button(dialog, SWT.PUSH);
		cancel.setText ("Cancel");
		data = new FormData ();
		data.width = 60;
		data.right = new FormAttachment (100, 0);
		data.bottom = new FormAttachment (100, 0);
		cancel.setLayoutData (data);
		cancel.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				dialog.close ();
			}
		});
		
		final Text text = new Text (dialog, SWT.BORDER);
		data = new FormData ();
		data.width = 200;
		data.left = new FormAttachment (label, 0, SWT.DEFAULT);
		data.right = new FormAttachment (100, 0);
		data.top = new FormAttachment (label, 0, SWT.CENTER);
		data.bottom = new FormAttachment (cancel, 0, SWT.DEFAULT);
		text.setLayoutData (data);
		
		Button ok = new Button (dialog, SWT.PUSH);
		ok.setText ("OK");
		data = new FormData ();
		data.width = 60;
		data.right = new FormAttachment (cancel, 0, SWT.DEFAULT);
		data.bottom = new FormAttachment (100, 0);
		ok.setLayoutData (data);
		ok.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				createApp(text.getText ());
				dialog.close ();
			}
		});
		
		dialog.setDefaultButton (ok);
		dialog.pack ();
		dialog.open ();
    }
    
	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for(DebugEvent event: events) {
			if (!(event.getSource() instanceof IProcess)) {
				continue;
			}
			IProcess p = (IProcess)event.getSource();
			if (p.getLaunch().equals(launch) && 
					(event.getKind() == DebugEvent.TERMINATE)) {
				try {
					selectedProject.refreshLocal(IResource.DEPTH_INFINITE, null);
					DebugPlugin.getDefault().removeDebugEventListener(this);						
				} catch(Exception e) {
					throw new RuntimeException(e);
				}
			}
		}		
	}
}
