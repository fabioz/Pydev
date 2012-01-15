/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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

    /**
     * This should be used to add participants at test-time. It should be the name
     * of the extension point to a list (which will be returned)
     */
    public static Map<String, List<Object>> testingParticipants;
    
    private static Map<String, IExtension[]> extensionsCache = new HashMap<String, IExtension[]>();
    
    //pydev
    public final static String PYDEV_COMPLETION = "org.python.pydev.pydev_completion";
    public final static String PYDEV_BUILDER = "org.python.pydev.pydev_builder";
    public final static String PYDEV_MODULES_OBSERVER = "org.python.pydev.pydev_modules_observer";
    public final static String PYDEV_VIEW_CREATED_OBSERVER = "org.python.pydev.pydev_view_created_observer";
    public final static String PYDEV_INTERPRETER_OBSERVER = "org.python.pydev.pydev_interpreter_observer";
    public final static String PYDEV_MANAGER_OBSERVER = "org.python.pydev.pydev_manager_observer";
    public final static String PYDEV_PARSER_OBSERVER = "org.python.pydev.parser.pydev_parser_observer";
    public static final String PYDEV_CTRL_1 = "org.python.pydev.pydev_ctrl_1";
    public static final String PYDEV_SIMPLE_ASSIST = "org.python.pydev.pydev_simpleassist";
    public static final String PYDEV_ORGANIZE_IMPORTS = "org.python.pydev.pydev_organize_imports";
    public static final String PYDEV_REFACTORING = "org.python.pydev.pydev_refactoring";
    public static final String PYDEV_QUICK_OUTLINE = "org.python.pydev.pydev_quick_outline";
    public static final String PYDEV_PYEDIT_LISTENER = "org.python.pydev.pydev_pyedit_listener";
    public static final String PYDEV_PREFERENCES_PROVIDER = "org.python.pydev.pydev_preferences_provider";
    public static final String PYDEV_FORMATTER = "org.python.pydev.pydev_formatter";
    public static final String PYDEV_GLOBALS_BROWSER = "org.python.pydev.pydev_globals_browser";
    public static final String PYDEV_DEBUG_PREFERENCES_PAGE = "org.python.pydev.pydev_debug_preferences_page";
    public static final String PYDEV_HOVER = "org.python.pydev.pydev_hover";
    
    //IInterpreterInfoBuilder
    public static final String PYDEV_INTERPRETER_INFO_BUILDER = "org.python.pydev.pydev_interpreter_info_builder";

    //debug
    public static final String PYDEV_DEBUG_CONSOLE_INPUT_LISTENER = "org.python.pydev.debug.pydev_debug_console_input_listener";



    
    public static IExtension[] getExtensions(String type) {
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
    
    /**
     * @param type  the name of the extension
     * @param allowOverride  if true, the last registered participant will be
     *                       returned, thus "overriding" any previously
     *                       registered participants. If false, an exception
     *                       is thrown if more than one participant is
     *                       registered.
     * @return  the participant for the given extension type, or null if none
     *          is registered.
     */
    public static Object getParticipant(String type, boolean allowOverride) {
        List<Object> participants = getParticipants(type);
        if (participants.isEmpty()){
            return null;
        }
        if (!allowOverride && participants.size() > 1) {
            // only one participant may be used for this
            throw new RuntimeException("More than one participant is registered for type:"+type);
        }
        return participants.get(participants.size() - 1);
    }
    
    /**
     * @param type the extension we want to get
     * @return a list of classes created from those extensions
     */
    public static List getParticipants(String type) {
        List<Object> list = null;
        if (testingParticipants != null) {
            list = testingParticipants.get(type);
            if (list == null) {
                list = new ArrayList<Object>();
            }
            return list;
        }

        list = new ArrayList<Object>();
        // For each extension ...
        for (IExtension extension : getExtensions(type)) {
            IConfigurationElement[] elements = extension
                    .getConfigurationElements();
            // For each member of the extension ...
            for (IConfigurationElement element : elements) {
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
