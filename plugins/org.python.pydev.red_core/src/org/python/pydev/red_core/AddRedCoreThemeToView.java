/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.red_core;

import org.eclipse.swt.custom.StyleRange;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.debug.codecoverage.CoverageCache;
import org.python.pydev.debug.codecoverage.PyCodeCoverageView;
import org.python.pydev.debug.pyunit.PyUnitView;
import org.python.pydev.navigator.ui.PydevPackageExplorer;
import org.python.pydev.outline.PyOutlinePage;
import org.python.pydev.ui.IViewCreatedObserver;

import com.python.pydev.ui.hierarchy.PyHierarchyView;

public class AddRedCoreThemeToView implements IViewCreatedObserver{

    private static boolean registeredForStyleOnCoverage = false;

	@SuppressWarnings("unchecked")
    public void notifyViewCreated(Object view) {
	    if(AddRedCoreThemeAvailable.isRedCoreAvailableForTheming()){
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
    			
    		}else if(view instanceof PyHierarchyView){
    		    AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
    		    PyHierarchyView castView = (PyHierarchyView) view;
    		    castView.onControlCreated.registerListener(onViewCreatedListener.onTreeViewCreated);
    		    castView.onDispose.registerListener(onViewCreatedListener.onDispose);
    		    
    		}else if(view instanceof PyCodeCoverageView){
    		    AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
    		    PyCodeCoverageView castView = (PyCodeCoverageView) view;
    		    castView.onControlCreated.registerListener(onViewCreatedListener.onTreeViewCreated);
    		    castView.onDispose.registerListener(onViewCreatedListener.onDispose);
    		    
    		    if(!registeredForStyleOnCoverage){
    		        //Only register once as it's static.
    		        registeredForStyleOnCoverage = true;
    		        final AddRedCorePreferences preferences = new AddRedCorePreferences();
    		        CoverageCache.onStyleCreated.registerListener(new ICallbackListener<StyleRange>() {
                        
                        public Object call(StyleRange obj) {
                            obj.foreground = preferences.getHyperlinkTextAttribute().getForeground();
                            return null;
                        }
                    });
    		    }
    			
    		}else if(view instanceof PyOutlinePage){
    		    AddRedCoreThemeToViewCallbacks onViewCreatedListener = new AddRedCoreThemeToViewCallbacks();
    		    PyOutlinePage castView = (PyOutlinePage) view;
    		    castView.onTreeViewerCreated.registerListener(onViewCreatedListener.onTreeViewCreated);
    		    castView.onDispose.registerListener(onViewCreatedListener.onDispose);
    		}
	    }
	}

}
