package org.python.pydev.red_core;

import com.aptana.editor.common.CommonEditorPlugin;

/**
 * Helper just to know if red core is available.
 */
public class AddRedCoreThemeAvailable {

	private static volatile Boolean redCoreAvailable = null;
	
	public static boolean isRedCoreAvailable(){
		if(redCoreAvailable == null){
			try {
				if(CommonEditorPlugin.getDefault() != null){
					redCoreAvailable = true;
				}else{
					redCoreAvailable = false;
				}
			} catch (Throwable e) {
				redCoreAvailable = false;//the plugin is not in the environment.
			}
		}
		return redCoreAvailable;
	}

	public static void setRedCoreAvailable(boolean b) {
		redCoreAvailable = false;
	}
}
