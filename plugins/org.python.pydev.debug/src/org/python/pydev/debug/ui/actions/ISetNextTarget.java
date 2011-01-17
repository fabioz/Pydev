package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

public interface ISetNextTarget {

	/**
	 * 
	 * @param part
	 * @param selection
	 * @param target
	 * @throws CoreException
	 */
	public boolean setNextToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target) throws CoreException;
	
	/**
	 * 
	 * @param part
	 * @param selection
	 * @param target
	 * @return
	 */
	public boolean canSetNextToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target);

}
