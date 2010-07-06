package org.python.pydev.red_core;

import org.eclipse.jface.viewers.TreeViewer;
import org.python.pydev.core.callbacks.ICallbackListener;

import com.aptana.theme.ThemePlugin;

/**
 * Used to create the callbacks that will add the theming to the pydev views.
 */
public class AddRedCoreThemeToViewCallbacks {

	public final ICallbackListener onDispose;
	public final ICallbackListener onTreeViewCreated;
	
	private class TreeViewerContainer{
	    public TreeViewerContainer(TreeViewer viewer) {
            this.viewer = viewer;
        }

        public final TreeViewer viewer;
	}
	
	private TreeViewerContainer treeViewerContainer;
	
	public AddRedCoreThemeToViewCallbacks() {
		onDispose = new ICallbackListener() {
			
			public Object call(Object obj) {
				ThemePlugin.getDefault().getControlThemerFactory().dispose(treeViewerContainer.viewer);
				return null;
			}
		};
		
		onTreeViewCreated = new ICallbackListener() {
			


            public Object call(Object obj) {
			    TreeViewer treeViewer = (TreeViewer) obj;
			    treeViewerContainer = new TreeViewerContainer(treeViewer);
                ThemePlugin.getDefault().getControlThemerFactory().apply(treeViewer);
				return null;
			}
		};
	}
}
