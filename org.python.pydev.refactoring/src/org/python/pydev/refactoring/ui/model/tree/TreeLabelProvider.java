package org.python.pydev.refactoring.ui.model.tree;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.refactoring.PepticImageCache;

public class TreeLabelProvider implements ILabelProvider {

	private PepticImageCache cache;

	public TreeLabelProvider() {
		cache = new PepticImageCache();
	}

	public Image getImage(Object element) {
		Image image = null;
		ITreeNode node = (ITreeNode) element;
		image = cache.get(node.getImageName());

		return image;
	}

	public String getText(Object element) {
		if (element instanceof ITreeNode) {
			return ((ITreeNode) element).getLabel();
		}

		return "";
	}

	public void addListener(ILabelProviderListener listener) {
	}

	public void dispose() {
		cache.dispose();
		cache = null;
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
	}

}
