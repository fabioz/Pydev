/*
 * Created on 14/09/2005
 */
package org.python.pydev.builder.pycremover;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.plugin.PydevPlugin;

public class PycRemoverBuilderVisitor extends PyDevBuilderVisitor{

    @Override
    public boolean visitChangedResource(IResource resource, IDocument document) {
        return true;
    }

    @Override
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        String loc = resource.getLocation().toOSString()+"c"; //.py+c = .pyc
        if(loc.endsWith(".pyc")){
            //the .py has just been removed, so, remove the .pyc if it exists
            try {
                File file = new File(loc);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
        
        return true;
    }

}
