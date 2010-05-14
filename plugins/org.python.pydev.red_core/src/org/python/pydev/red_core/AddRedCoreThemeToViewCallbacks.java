package org.python.pydev.red_core;

import org.eclipse.jface.viewers.TreeViewer;
import org.python.pydev.core.callbacks.ICallbackListener;

import com.aptana.editor.common.theme.TreeThemer;

/**
 * Used to create the callbacks that will add the theming to the pydev views.
 */
public class AddRedCoreThemeToViewCallbacks {

	public final ICallbackListener onDispose;
	public final ICallbackListener onTreeViewCreated;
	private TreeThemer treeThemer;
	
	public AddRedCoreThemeToViewCallbacks() {
		onDispose = new ICallbackListener() {
			
			public Object call(Object obj) {
				if(treeThemer!=null){
					treeThemer.dispose();
					treeThemer = null;
				}
				return null;
			}
		};
		
		onTreeViewCreated = new ICallbackListener() {
			
			public Object call(Object obj) {
				treeThemer = new TreeThemer((TreeViewer) obj);
				treeThemer.apply();
				return null;
			}
		};
	}
}
