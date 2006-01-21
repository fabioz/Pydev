/*
 * Created on Jan 21, 2006
 */
package org.python.pydev.ui.dialogs;

import org.eclipse.core.resources.IContainer;

public class SourceFolder{
    public IContainer folder;
    
    public SourceFolder(IContainer folder){
        this.folder = folder;
    }
}
