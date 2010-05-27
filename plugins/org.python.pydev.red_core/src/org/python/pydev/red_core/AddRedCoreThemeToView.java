package org.python.pydev.red_core;

import org.python.pydev.navigator.ui.PydevPackageExplorer;
import org.python.pydev.outline.PyOutlinePage;
import org.python.pydev.ui.IViewCreatedObserver;

public class AddRedCoreThemeToView implements IViewCreatedObserver{


	public void notifyViewCreated(Object view) {
		if(view instanceof PydevPackageExplorer){
			AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
			PydevPackageExplorer pydevPackageExplorer = (PydevPackageExplorer) view;
			pydevPackageExplorer.onTreeViewerCreated.registerListener(onViewCreatedListener.onTreeViewCreated);
			pydevPackageExplorer.onDispose.registerListener(onViewCreatedListener.onDispose);
			
		}else if(view instanceof PyOutlinePage){
			AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
			PyOutlinePage pyOutlinePage = (PyOutlinePage) view;
			pyOutlinePage.onTreeViewerCreated.registerListener(onViewCreatedListener.onTreeViewCreated);
			pyOutlinePage.onDispose.registerListener(onViewCreatedListener.onDispose);
		}
		
	}

}
