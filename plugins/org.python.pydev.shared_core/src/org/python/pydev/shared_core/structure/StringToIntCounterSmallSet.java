/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.structure;

import java.util.Arrays;

import org.python.pydev.shared_core.log.Log;

/**
 * This is a low-memory/optimized map from Strings to ints (note that it may be slow for larger collections
 * -- not recommended for more than 8 items, ideally the usual case is 1-3 items).
 *
 * It's like a map with a single bucket optimized to have a counter for each string.
 */
public final class StringToIntCounterSmallSet {

    private static final String[] EMPTY_STRINGS = new String[0];

    private static final int[] EMPTY_INTS = new int[0];

    private String[] strings = EMPTY_STRINGS;

    private int[] ints = EMPTY_INTS;

    private int size;

    private boolean logged;

    /**
     * Gets the current count for the given string (if not there, 0 is returned).
     */
    public int get(String representation) {
        for (int i = 0; i < size; i++) {
            if (strings[i].equals(representation)) {
                return ints[i];
            }
        }
        return 0;
    }

    public int increment(String representation) {
        for (int i = 0; i < size; i++) {
            if (strings[i].equals(representation)) {
                int ret = ints[i] + 1;
                ints[i] = ret;
                return ret;
            }
        }
        put(representation, 1);
        return 1;
    }

    /**
     * Sets the current count for the given string.
     */
    public void put(String representation, int curr) {
        for (int i = 0; i < size; i++) {
            if (strings[i].equals(representation)) {
                ints[i] = curr;
                return;
            }
        }
        //Still not there: create entry (extend current array if needed);
        if (strings.length <= size) {
            strings = Arrays.copyOf(strings, size + 3);
            ints = Arrays.copyOf(ints, size + 3);
        }
        strings[size] = representation;
        ints[size] = curr;

        size++;
        if (size > 8) {
            if (!logged) {
                logged = true;
                Log.log("StringToIntCounterSmallSet size > 8 may be big for its use. Consider using a HashMap<String, Integer>.");
            }
        }
    }

    public void clear() {
        size = 0; //note: keep the memory we allocated so that we don't have to do that again later on.
    }

}
