/*
 * Created on Jan 21, 2006
 */
package org.python.pydev.ui.dialogs;

import org.eclipse.core.resources.IFolder;

public class Package{
    public IFolder folder;
    public SourceFolder sourceFolder;
    
    public Package(IFolder folder, SourceFolder sourceFolder){
        this.folder = folder;
        this.sourceFolder = sourceFolder;
    }

    public String getPackageName() {
        String srcPath = sourceFolder.folder.getFullPath().toString();
        String complete = folder.getFullPath().toString();
        if(complete.startsWith(srcPath)){
            complete = complete.substring(srcPath.length()).replace('/', '.');
            if(complete.startsWith(".")){
                complete = complete.substring(1);
            }
        }
        return complete;
    }
    
}
