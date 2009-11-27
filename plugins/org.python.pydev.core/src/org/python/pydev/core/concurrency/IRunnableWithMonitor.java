package org.python.pydev.core.concurrency;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IRunnableWithMonitor extends Runnable{
    
    void setMonitor(IProgressMonitor monitor);

}
