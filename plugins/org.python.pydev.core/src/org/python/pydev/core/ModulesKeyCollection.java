/*******************************************************************************
 * Copyright (c) 2014 Google, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Danny Yoo (Google) - initial API and implementation
 *******************************************************************************/
package org.python.pydev.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A collection of {@link ModulesKey}s and methods to search those keys.
 */
public class ModulesKeyCollection {
    /**
     * The collection of moduleKeys.
     */
    private TreeMap<String, ModulesKey> modulesKeys;

    /**
     * The following data structure accelerates autocompletion.  It holds a mapping between a
     * name component and the modules that contain that name component.
     *
     * <p>As an example, given a {@link ModulesKey} for {@code org.python.pydev.PydevPlugin},
     * the {@link #startingWithCache} will be populated with mappings for the four name component
     * strings: {@code "org"}, {@code "python"}, {@code "pydev"}, and {@code "PydevPlugin"}.
     * These will map to {@link Set}s which contain the {@link ModulesKey}.
     */
    private TreeMap<String, Set<ModulesKey>> startingWithCache;

    /**
     * Constructs an empty collection.
     */
    public ModulesKeyCollection() {
        modulesKeys = new TreeMap<>();
        startingWithCache = new TreeMap<>();
    }

    /**
     * Returns the keys in the collection.
     */
    public Collection<ModulesKey> getModulesKeys() {
        return modulesKeys.values();
    }

    /**
     * Returns the number of keys.
     */
    public int size() {
        return modulesKeys.size();
    }

    /**
     * Adds a {@link ModulesKey} to the collection.
     */
    public void add(ModulesKey key) {
        modulesKeys.put(key.name, key);

        for (String elt : key.name.split("\\.")) {
            elt = elt.toLowerCase();
            Set<ModulesKey> matchingKeys = startingWithCache.get(elt);
            if (matchingKeys == null) {
                matchingKeys = new TreeSet<>();
                startingWithCache.put(elt, matchingKeys);
            }
            matchingKeys.add(key);
        }
    }

    /**
     * Returns the {@link ModulesKey} sharing the same name as the input key, or
     * {@code null} if it's absent from the collection.
     */
    public ModulesKey get(ModulesKey key) {
        return modulesKeys.get(key.name);
    }

    /**
     * Returns the {@link ModulesKey} sharing the same name as the input, or
     * {@code null} if it's absent from the collection.
     */
    public ModulesKey get(String name) {
        return modulesKeys.get(name);
    }

    /**
     * Returns {@code true} if the container has a {@link ModulesKey} sharing the
     * same name as the input.
     */
    public boolean contains(ModulesKey key) {
        return modulesKeys.containsKey(key.name);
    }

    /**
     * Removes the the {@link ModulesKey} sharing the same name as the input.
     */
    public void remove(ModulesKey key) {
        modulesKeys.remove(key.name);
        for (String elt : key.name.split("\\.")) {
            elt = elt.toLowerCase();
            Set<ModulesKey> matchingKeys = startingWithCache.get(elt);
            if (matchingKeys != null) {
                matchingKeys.remove(key);
            }
        }
    }

    /**
     * Returns all {@link ModulesKey} elements in the collection whose name has a
     * component that begins with the input, matching case-insensitively.
     */
    public Set<ModulesKey> searchWithCaseInsensitivePrefixPart(String startingWith) {
        startingWith = startingWith.toLowerCase();
        Set<ModulesKey> matchingKeys = new HashSet<>();
        for (Set<ModulesKey> l : searchMapWithPrefix(startingWithCache, startingWith).values()) {
            matchingKeys.addAll(l);
        }
        return matchingKeys;
    }

    /**
     * Returns all {@link ModulesKey} elements in the collection whose name begins with the input.
     */
    public Set<ModulesKey> searchWithPrefix(String startingWith) {
        Set<ModulesKey> matchingKeys = new TreeSet<>();
        for (ModulesKey key : searchMapWithPrefix(modulesKeys, startingWith).values()) {
            matchingKeys.add(key);
        }
        return matchingKeys;
    }

    /**
     * Performs a string-based "starts with" search, returns only the mapping whose keys
     * start with the given prefix.
     */
    private static <V> SortedMap<String, V> searchMapWithPrefix(TreeMap<String, V> mapping,
            String prefix) {
        // Trival case: if empty string, return everything.
        if ("".equals(prefix)) {
            return mapping;
        }

        // Common case: the startsWith does not end with "\uffff", the highest character.
        // In which case, we can use a subMap query:
        if (prefix.charAt(prefix.length() - 1) != '\uffff') {
            String butLast = prefix.substring(0, prefix.length() - 1);
            return mapping.subMap(prefix, butLast + "\uffff");
        }

        // General case: search from the tail and accumulate results.
        // TODO: use a profiler and benchmark this; perhaps we can simplify the code to just
        // the general case without harming performance.
        TreeMap<String, V> result = new TreeMap<>();
        for (Entry<String, V> entry : mapping.tailMap(prefix, true).entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Clears the collection.
     */
    public void clear() {
        modulesKeys = new TreeMap<>();
        startingWithCache = new TreeMap<>();
    }
}
