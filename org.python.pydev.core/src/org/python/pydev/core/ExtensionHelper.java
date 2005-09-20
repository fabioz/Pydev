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
import org.eclipse.core.runtime.Platform;
import org.python.pydev.core.log.Log;

public class ExtensionHelper {

    
    private static Map<String, IExtension[]> extensionsCache = new HashMap<String, IExtension[]>();
    public final static String PYDEV_COMPLETION = "org.python.pydev.pydev_completion";
    public final static String PYDEV_BUILDER = "org.python.pydev.pydev_builder";
    public final static String PYDEV_INTERPRETER_OBSERVER = "org.python.pydev.pydev_interpreter_observer";
    public final static String PYDEV_PARSER_OBSERVER = "org.python.pydev.parser.pydev_parser_observer";
    public static final String PYDEV_CTRL_1 = "org.python.pydev.ctrl_1_participants";
    
    
    private static IExtension[] getExtensions(String type) {
        IExtension[] extensions = extensionsCache.get(type);
        if(extensions == null){
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            if(registry != null){ // we may not be in eclipse env when testing
                IExtensionPoint extensionPoint = registry.getExtensionPoint(type);
                extensions = extensionPoint.getExtensions();
                extensionsCache.put(type, extensions);
            }else{
                extensions = new IExtension[0];
            }
        }
        return extensions;
    }
    
    
    /**
     * "org.python.pydev.pydev_completion"
     * "org.python.pydev.pydev_builder"
     * "org.python.pydev.pydev_interpreter_observer"
     * "org.python.pydev.parser.pydev_parser_observer"
     * "org.python.pydev.ctrl_1_participants"
     * 
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
