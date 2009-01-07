/*
 * Created on May 19, 2006
 */
package org.python.pydev.editorinput;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.core.structure.FastStringBuffer;

public class PyFileLabelProvider implements ILabelProvider {

    private WorkbenchLabelProvider provider;

    public PyFileLabelProvider() {
        provider = new WorkbenchLabelProvider();
    }
    
    public Image getImage(Object element) {
        return provider.getImage(element);
    }

    public String getText(Object element) {
        if(element instanceof IFile){
            IFile f = (IFile) element;
            FastStringBuffer buffer = new FastStringBuffer();
            buffer.append(f.getName());
            buffer.append(" (");
            buffer.append(f.getFullPath().removeFileExtension().removeLastSegments(1).toString());
            buffer.append(")");
            return buffer.toString();
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
