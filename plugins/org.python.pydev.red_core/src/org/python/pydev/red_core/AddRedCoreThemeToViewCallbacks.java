/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.red_core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Control;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;

import com.aptana.theme.ThemePlugin;

/**
 * Used to create the callbacks that will add the theming to the pydev views.
 */
@SuppressWarnings("rawtypes")
public class AddRedCoreThemeToViewCallbacks {

    public final ICallbackListener onDispose;
	public final ICallbackListener onTreeViewCreated;
	
	private class Container{
	    public Container(Control viewer) {
	        this.obj = viewer;
	    }
	    
	    public Container(TreeViewer viewer) {
            this.obj = viewer;
        }

        public final Object obj;
	}
	
	private Container container;
	
    public AddRedCoreThemeToViewCallbacks() {
		onDispose = new ICallbackListener() {
			
			public Object call(Object obj) {
				try {
				    if(container.obj instanceof TreeViewer){
				        ThemePlugin.getDefault().getControlThemerFactory().dispose((TreeViewer)container.obj);
				        
    				}else if(obj instanceof Control){
    				    ThemePlugin.getDefault().getControlThemerFactory().dispose((Control)container.obj);
    				    
    				    
    				}else{
    				    Log.log("Cannot handle: "+obj);
    				}
                } catch (Throwable e) {
                    Log.log(IStatus.ERROR, "Unable to dispose properly. Probably using incompatible version of Aptana Studio", e);
                }
				return null;
			}
		};
		
		onTreeViewCreated = new ICallbackListener() {
			


            public Object call(Object obj) {
                if(obj instanceof TreeViewer){
    			    TreeViewer treeViewer = (TreeViewer) obj;
    			    container = new Container(treeViewer);
                    try {
                        ThemePlugin.getDefault().getControlThemerFactory().apply(treeViewer);
                    } catch (Throwable e) {
                        Log.log(IStatus.ERROR, "Unable to apply theme. Probably using incompatible version of Aptana Studio", e);
                    }
                    
                }else if(obj instanceof Control){
                    Control control = (Control) obj;
                    container = new Container(control);
                    try {
                        ThemePlugin.getDefault().getControlThemerFactory().apply(control);
                    } catch (Throwable e) {
                        Log.log(IStatus.ERROR, "Unable to apply theme. Probably using incompatible version of Aptana Studio", e);
                    }
                    
                    
                }else{
                    Log.log("Cannot handle: "+obj);
                }
                    
                return null;
			}
		};
	}
}
