/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.docutils.StringUtils;

/**
 * Helper to get a structure from a zip file
 */
public class ZipStructure {

    private Map<Integer, TreeSet<String>> levelToContents = new HashMap<Integer, TreeSet<String>>();
    public final ZipFile zipFile;
    public final File file;

    /*package*/ZipStructure() { //just for testing
        this.zipFile = null;
        this.file = null;
    }

    /**
     * @param file the file that's treated as a zip
     * @param zipFile the zip wrapping of the passed file
     */
    public ZipStructure(File file, ZipFile zipFile) {
        this.zipFile = zipFile;
        this.file = file;
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry element = entries.nextElement();
            String name = element.getName();
            int level = StringUtils.count(name, '/');
            if (StringUtils.endsWith(name, '/')) {
                level--; //it's one level below if that's a directory
            }
            TreeSet<String> treeSet = levelToContents.get(level);
            if (treeSet == null) {
                treeSet = new TreeSet<String>();
                levelToContents.put(level, treeSet);
            }
            treeSet.add(name);
        }
    }

    /*package*/Map<Integer, TreeSet<String>> getLevelToContents() {
        return levelToContents;
    }

    /**
     * In this method we'll get the contents within the zip file for the passed directory
     * 
     * @param string: Must be a directory within the zip file or an empty string to get the root contents
     */
    public List<String> contents(String name) {
        ArrayList<String> ret = new ArrayList<String>();

        int level;
        int length = name.length();
        if (length == 0) {
            level = 0;
        } else {
            Assert.isTrue(StringUtils.endsWith(name, '/')); //must be a directory
            level = StringUtils.count(name, '/');
        }

        TreeSet<String> treeSet = levelToContents.get(level);
        if (treeSet != null) {
            if (length == 0) {
                ret.addAll(treeSet);
            } else {
                for (String s : treeSet.tailSet(name)) {
                    if (s.startsWith(name)) {
                        ret.add(s);
                    }
                }
            }
        }

        return ret;
    }

}
