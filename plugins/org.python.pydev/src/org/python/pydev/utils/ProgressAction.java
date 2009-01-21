/*
 * Created on Oct 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * @author Fabio Zadrozny
 */
public class ProgressAction extends org.eclipse.jface.action.Action {

    public IProgressMonitor monitor; //this monitor should be set before executing the action.
}
