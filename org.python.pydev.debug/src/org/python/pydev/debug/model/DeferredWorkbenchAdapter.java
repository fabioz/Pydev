package org.python.pydev.debug.model;

import org.eclipse.debug.ui.DeferredDebugElementWorkbenchAdapter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;

public class DeferredWorkbenchAdapter extends DeferredDebugElementWorkbenchAdapter implements IDeferredWorkbenchAdapter, ICommandResponseListener{

	private PyVariableCollection variableCollection;
	private PyVariable[] commandVariables;
	
	public Object[] getChildren(Object o) {
		if(variableCollection != null){
			throw new RuntimeException("This object might not be reused.");
		}

		if(o instanceof PyVariableCollection){
			variableCollection = (PyVariableCollection)o;
			
			AbstractRemoteDebugger dbg = variableCollection.getDebugger();
			GetVariableCommand variableCommand = variableCollection.getVariableCommand(dbg);
			variableCommand.setCompletionListener(this);
			dbg.postCommand(variableCommand);
			return waitForCommand(variableCollection);
			
		}
		return new Object[0];
	}

	private Object[] waitForCommand(PyVariableCollection c) {
		try {
			// VariablesView does not deal well with children changing asynchronously.
			// it causes unneeded scrolling, because view preserves selection instead
			// of visibility.
			// I try to minimize the occurence here, by giving pydevd time to complete the
			// task before we are forced to do asynchronous notification.
			int i = 10; 
			while (--i > 0 && commandVariables == null){
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return commandVariables;
	}

	public Object getParent(Object o) {
		//do we really need that?
		return null;
	}

	public void commandComplete(AbstractDebuggerCommand cmd) {
		commandVariables = variableCollection.getCommandVariables(cmd);
	}


}
