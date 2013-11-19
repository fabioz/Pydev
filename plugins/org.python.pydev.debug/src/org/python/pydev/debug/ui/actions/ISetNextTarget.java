/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * @author Hussain Bohra
 */
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
