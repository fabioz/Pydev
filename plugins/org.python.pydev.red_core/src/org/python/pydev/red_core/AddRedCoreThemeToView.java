/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.red_core;

import org.python.pydev.debug.codecoverage.PyCodeCoverageView;
import org.python.pydev.debug.pyunit.PyUnitView;
import org.python.pydev.navigator.ui.PydevPackageExplorer;
import org.python.pydev.outline.PyOutlinePage;
import org.python.pydev.ui.IViewCreatedObserver;

public class AddRedCoreThemeToView implements IViewCreatedObserver{


	public void notifyViewCreated(Object view) {
	    if(AddRedCoreThemeAvailable.isRedCoreAvailable()){
    		if(view instanceof PydevPackageExplorer){
    			AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
    			PydevPackageExplorer castView = (PydevPackageExplorer) view;
    			castView.onTreeViewerCreated.registerListener(onViewCreatedListener.onTreeViewCreated);
    			castView.onDispose.registerListener(onViewCreatedListener.onDispose);
    			
    		}else if(view instanceof PyUnitView){
    			AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
    			PyUnitView castView = (PyUnitView) view;
    			castView.onControlCreated.registerListener(onViewCreatedListener.onTreeViewCreated);
    			castView.onDispose.registerListener(onViewCreatedListener.onDispose);
    			
    		}else if(view instanceof PyCodeCoverageView){
    		    AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
    		    PyCodeCoverageView castView = (PyCodeCoverageView) view;
    		    castView.onControlCreated.registerListener(onViewCreatedListener.onTreeViewCreated);
    		    castView.onDispose.registerListener(onViewCreatedListener.onDispose);
    			
    		}else if(view instanceof PyOutlinePage){
    		    AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
    		    PyOutlinePage castView = (PyOutlinePage) view;
    		    castView.onTreeViewerCreated.registerListener(onViewCreatedListener.onTreeViewCreated);
    		    castView.onDispose.registerListener(onViewCreatedListener.onDispose);
    		}
	    }
	}

}
