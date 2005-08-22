/*
 * Created on 21/08/2005
 */
package org.python.pydev.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.python.pydev.plugin.PydevPlugin;

public class ExtensionHelper {

    
    private static Map<String, IExtension[]> extensionsCache = new HashMap<String, IExtension[]>();
    public final static String PYDEV_COMPLETION = "org.python.pydev.pydev_completion";
    public final static String PYDEV_BUILDER = "org.python.pydev.pydev_builder";
    
    
    private static IExtension[] getExtensions(String type) {
        IExtension[] extensions = extensionsCache.get(type);
        if(extensions == null){
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint extensionPoint = registry.getExtensionPoint(type);
            extensions = extensionPoint.getExtensions();
            extensionsCache.put(type, extensions);
        }
        return extensions;
    }
    
    
    /**
     * "org.python.pydev.pydev_completion"
     * "org.python.pydev.pydev_builder"
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
                    PydevPlugin.log(e);
                }
            }
        }
        return list;
    }
    

}
