package org.python.pydev.navigator.filters;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.core.structure.TreeNode;
import org.python.pydev.navigator.LabelAndImage;

public abstract class AbstractFilter extends ViewerFilter{

	protected String getName(Object element) {
		if(element instanceof IAdaptable){
            IAdaptable adaptable = (IAdaptable) element;
            Object adapted = adaptable.getAdapter(IResource.class);
            if(adapted instanceof IResource){
                IResource resource = (IResource) adapted;
                return resource.getName();
            }
            
        }else if(element instanceof TreeNode){
			TreeNode treeNode = (TreeNode) element;
			Object data = treeNode.getData();
			if(data instanceof LabelAndImage){
				return ((LabelAndImage) data).o1;
			}
        }
		return null;
	}

}
