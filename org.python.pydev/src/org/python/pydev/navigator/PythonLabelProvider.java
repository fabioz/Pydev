/*
 * Created on Oct 7, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

public class PythonLabelProvider implements ILabelProvider{

    private WorkbenchLabelProvider provider;

    public PythonLabelProvider() {
        provider = new WorkbenchLabelProvider();
    }
    
    /**
     * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
     */
    public Image getImage(Object element) {
        if(element instanceof PythonSourceFolder){
            return PydevPlugin.getImageCache().get(UIConstants.SOURCE_FOLDER_ICON);
        }
        if(element instanceof PythonNode){
            PythonNode node = (PythonNode) element;
            return node.entry.getImage();
        }
        if(element instanceof IWrappedResource){
        	IWrappedResource resource = (IWrappedResource) element;
			return provider.getImage(resource.getActualObject());
        }
        return provider.getImage(element);
    }

    /**
     * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
     */
    public String getText(Object element) {
        if(element instanceof PythonNode){
            PythonNode node = (PythonNode) element;
            return node.entry.toString();
        }
        if(element instanceof PythonSourceFolder){
            PythonSourceFolder sourceFolder = (PythonSourceFolder) element;
            return provider.getText(sourceFolder.folder);
        }
        if(element instanceof IWrappedResource){
        	IWrappedResource resource = (IWrappedResource) element;
        	return provider.getText(resource.getActualObject());
        }
        return provider.getText(element);
    }

    /**
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
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
