/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Helper class to deal with properties.
 */
public class PropertiesHelper {

    public static Properties createPropertiesFromString(String asPortableString) {
        Properties properties = new Properties();
        try {
            properties.load(new ByteArrayInputStream(asPortableString.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    public static String createStringFromProperties(Properties properties) {
        OutputStream out = new ByteArrayOutputStream();
        try {
            properties.store(out, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    public static Properties createPropertiesFromMap(Map<String, String> treeItemsAsMap) {

        Properties properties = new Properties();
        properties.putAll(treeItemsAsMap);
        return properties;
    }

    public static Map<String, String> createMapFromProperties(Properties stringSubstitutionVariables) {
        HashMap<String, String> map = new HashMap<String, String>();
        Set<Entry<Object, Object>> entrySet = stringSubstitutionVariables.entrySet();
        for (Entry<Object, Object> entry : entrySet) {
            map.put((String) entry.getKey(), (String) entry.getValue());
        }
        return map;
    }

}
