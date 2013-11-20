/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.concurrency;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IRunnableWithMonitor extends Runnable {

    void setMonitor(IProgressMonitor monitor);

}
