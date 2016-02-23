/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.ui.IActionFilter;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.log.Log;

/**
 * This class was created to forward the action filter checks to the property testers available in the
 * customizations plugin (needed to test the app_engine property in the action filter -- to be consistent
 * with the Eclipse 3.2 way of adding popup menus).
 * 
 * If we do drop support for eclipse 3.2, this class should probably be removed.
 */
public class PythonSourceFolderActionFilter implements IActionFilter {

    private static Map<String, List<PropertyTester>> propertyTesters = new HashMap<String, List<PropertyTester>>();
    private final IActionFilter platformActionFilter;

    public PythonSourceFolderActionFilter(IActionFilter platformActionFilter) {
        this.platformActionFilter = platformActionFilter;
    }

    /**
     * Adds a way to check for the app_engine (currently hardcoded) property from this plugin.
     */
    @Override
    public boolean testAttribute(Object target, String name, String value) {
        //For now let's leave it checking only app_engine, but this could grow if needed.
        if ("app_engine".equals(name)) {
            List<PropertyTester> propertyTesters = getPropertyTestersFromPydevCustomizations(name);
            for (PropertyTester tester : propertyTesters) {
                if (tester.test(target, name, null, value)) {
                    return true;
                }
            }
        }

        //If we didn't find what we were looking for, use the platform action filter
        if (this.platformActionFilter != null) {
            return this.platformActionFilter.testAttribute(target, name, value);
        }

        //if the platform didn't provide it, just return false.
        return false;
    }

    /**
     * Cache them after the 1st request for a given name.
     * 
     * Gets the property testers in org.python.pydev.customizations that match the passed name.
     */
    private static synchronized List<PropertyTester> getPropertyTestersFromPydevCustomizations(String name) {
        List<PropertyTester> propertyTester = propertyTesters.get(name);
        if (propertyTester == null) {
            IExtension[] extensions = ExtensionHelper.getExtensions("org.eclipse.core.expressions.propertyTesters");
            // For each extension ...
            propertyTester = new ArrayList<PropertyTester>();
            propertyTesters.put(name, propertyTester);
            for (int i = 0; i < extensions.length; i++) {
                IExtension extension = extensions[i];
                IConfigurationElement[] elements = extension.getConfigurationElements();
                // For each member of the extension ...
                for (int j = 0; j < elements.length; j++) {
                    IConfigurationElement element = elements[j];
                    //Any property tester that's declared in "org.python.pydev.customizations"
                    //is considered to be an object that provides the objectState for an IActionFilter.
                    if ("org.python.pydev.customizations".equals(element.getAttribute("namespace"))) {
                        String attribute = element.getAttribute("properties");
                        if (name.equals(attribute)) {//i.e.: app_engine (and future references)
                            try {
                                PropertyTester executableExtension = (PropertyTester) element
                                        .createExecutableExtension("class");
                                propertyTester.add(executableExtension);
                            } catch (Exception e) {
                                Log.log(e);
                            }
                        }
                    }
                }
            }
        }
        return propertyTester;
    }

}
