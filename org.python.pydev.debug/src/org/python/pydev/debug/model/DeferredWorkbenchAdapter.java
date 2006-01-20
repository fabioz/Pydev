package org.python.pydev.debug.model;

import org.eclipse.debug.ui.DeferredDebugElementWorkbenchAdapter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.AbstractRemoteDebugger;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;

public class DeferredWorkbenchAdapter extends DeferredDebugElementWorkbenchAdapter implements IDeferredWorkbenchAdapter, ICommandResponseListener{

	private PyVariable[] commandVariables;
	private AbstractDebugTarget target;
	private IVariableLocator locator;
	private Object parent;
	
	public Object[] getChildren(Object o) {
		parent = o;
		if(o instanceof PyVariableCollection){
			PyVariableCollection variableCollection = (PyVariableCollection)o;
			
			AbstractRemoteDebugger dbg = variableCollection.getDebugger();
			target = dbg.getTarget();
			locator = variableCollection;
			
			GetVariableCommand variableCommand = variableCollection.getVariableCommand(dbg);
			variableCommand.setCompletionListener(this);
			dbg.postCommand(variableCommand);
			return waitForCommand();
			
			
		}else if (o instanceof PyStackFrame){
			PyStackFrame f = (PyStackFrame) o;
			
			AbstractRemoteDebugger dbg = f.getDebugger();
			target = dbg.getTarget();
			locator = f;

			GetVariableCommand variableCommand = f.getFrameCommand(dbg);
			variableCommand.setCompletionListener(this);
			dbg.postCommand(variableCommand);
			return waitForCommand();
			
		}else if (o instanceof PyVariable){
			return new Object[0];
			
		}else{
			throw new RuntimeException("Unexpected class: "+o.getClass());
		}
	}

	private PyVariable[] waitForCommand() {
		try {
			// VariablesView does not deal well with children changing asynchronously.
			// it causes unneeded scrolling, because view preserves selection instead
			// of visibility.
			// I try to minimize the occurence here, by giving pydevd time to complete the
			// task before we are forced to do asynchronous notification.
			int i = 50; 
			while (--i > 0 && commandVariables == null){
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if(commandVariables != null){
			return commandVariables;
		}
		return new PyVariable[0];
	}

	public Object getParent(Object o) {
		//do we really need that?
		return null;
	}

	public void commandComplete(AbstractDebuggerCommand cmd) {
		PyVariable[] temp = PyVariableCollection.getCommandVariables(cmd, target, locator);
		if(parent instanceof PyVariableCollection){
			commandVariables = temp;
			
		} else if(parent instanceof PyStackFrame){
			PyStackFrame f = (PyStackFrame) parent;
			PyVariable[] temp1 = new PyVariable[temp.length +1];
			System.arraycopy(temp,0,temp1,1,temp.length);
			temp1[0] = new PyVariableCollection(target, "Globals", "frame.f_global", "Global variables", f.getGlobalLocator());
			commandVariables = temp1;
			
		}else{
			throw new RuntimeException("Unknown parent:"+parent.getClass());
		}
	}


}
