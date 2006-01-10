/*
 * Created on 21/08/2005
 */
package org.python.pydev.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.python.pydev.core.log.Log;

public class ExtensionHelper {

    
    private static Map<String, IExtension[]> extensionsCache = new HashMap<String, IExtension[]>();
    public final static String PYDEV_COMPLETION = "org.python.pydev.pydev_completion";
    public final static String PYDEV_BUILDER = "org.python.pydev.pydev_builder";
    public final static String PYDEV_INTERPRETER_OBSERVER = "org.python.pydev.pydev_interpreter_observer";
    public final static String PYDEV_PARSER_OBSERVER = "org.python.pydev.parser.pydev_parser_observer";
    public static final String PYDEV_CTRL_1 = "org.python.pydev.pydev_ctrl_1";
    public static final String PYDEV_SIMPLE_ASSIST = "org.python.pydev.pydev_simpleassist";
    public static final String PYDEV_ORGANIZE_IMPORTS = "org.python.pydev.pydev_organize_imports";
    public static final String PYDEV_REFACTORING = "org.python.pydev.pydev_refactoring";
    public static final String PYDEV_QUICK_OUTLINE = "org.python.pydev.pydev_quick_outline";
    
    
    private static IExtension[] getExtensions(String type) {
        IExtension[] extensions = extensionsCache.get(type);
        if(extensions == null){
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            if(registry != null){ // we may not be in eclipse env when testing
                try {
                    IExtensionPoint extensionPoint = registry.getExtensionPoint(type);
                    extensions = extensionPoint.getExtensions();
                    extensionsCache.put(type, extensions);
                } catch (Exception e) {
                    Log.log(IStatus.ERROR, "Error getting extension for:"+ type, e);
                    throw new RuntimeException(e);
                }
            }else{
                extensions = new IExtension[0];
            }
        }
        return extensions;
    }
    
    public static Object getParticipant(String type) {
    	//only one participant may be used for this
    	List participants = getParticipants(type);
    	if(participants.size() == 1){
    		return participants.get(0);
    	}
    	
    	if(participants.size() == 0){
    		return null;
    	}
    	
    	if(participants.size() > 1){
    		throw new RuntimeException("More than one participant is registered for type:"+type);
    	}
    	
    	throw new RuntimeException("Should never get here!");
    	
    }
    
    /**
     * @param type the extension we want to get
     * @return a list of classes created from those extensions
     */
    public static List getParticipants(String type) {
        ArrayList list = new ArrayList();
        IExtension[] extensions = getExtensions(type);
        // For each extension ...
        for (int i = 0; i < extensions.length; i++) {
            IExtension extension = extensions[i];
            IConfigurationElement[] elements = extension.getConfigurationElements();
            // For each member of the extension ...
            for (int j = 0; j < elements.length; j++) {
                IConfigurationElement element = elements[j];
                
                try {
                    list.add(element.createExecutableExtension("class"));
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }
        return list;
    }
    

}
