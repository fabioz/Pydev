/*
 * Created on Oct 7, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class PythonLabelProvider implements ILabelProvider{

    private WorkbenchLabelProvider provider;

    public PythonLabelProvider() {
        provider = new WorkbenchLabelProvider();
    }
    
    public Image getImage(Object element) {
        return provider.getImage(element);
    }

    public String getText(Object element) {
        if(element instanceof IFile){
            IFile f = (IFile) element;
            StringBuffer buffer = new StringBuffer();
            buffer.append(f.getName());
            buffer.append(" (");
            buffer.append(f.getFullPath().removeFileExtension().removeLastSegments(1));
            buffer.append(")");
            return buffer.toString();
        }else if(element instanceof PythonTreeData){
            PythonTreeData data = (PythonTreeData) element;
            return data.toString();
        }
        
        return provider.getText(element);
    }

    public void addListener(ILabelProviderListener listener) {
        provider.addListener(listener);
    }

    public void dispose() {
        provider.dispose();
    }

    public boolean isLabelProperty(Object element, String property) {
        return provider.isLabelProperty(element, property);
    }

    public void removeListener(ILabelProviderListener listener) {
        provider.removeListener(listener);
    }


}
