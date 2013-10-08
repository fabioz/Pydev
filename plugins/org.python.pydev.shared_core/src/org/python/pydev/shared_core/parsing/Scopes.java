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
package org.python.pydev.shared_core.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.python.pydev.shared_core.string.BaseParsingUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;

public class Scopes {

    public static int TYPE_COMMENT = 1;
    public static final int TYPE_PEER = 2;
    public static final int TYPE_STRING = 3;
    public static final int TYPE_MODULE = 4;
    public static final int TYPE_SUITE = 5;

    /**
     * Structure mapping the offset to the scope entries at that offset.
     * 
     * At a given position, opening entries should appear before the position and closing entries after the position.
     */
    private Map<Integer, List<ScopeEntry>> offsetToEntries = new HashMap<Integer, List<ScopeEntry>>();
    private int scopeId = 0;
    private Map<Integer, Tuple<ScopeEntry, ScopeEntry>> idToStartEnd = new HashMap<Integer, Tuple<ScopeEntry, ScopeEntry>>();

    private List<ScopeEntry> getAtOffset(int offset) {
        List<ScopeEntry> list = offsetToEntries.get(offset);
        if (list == null) {
            list = new ArrayList<ScopeEntry>();
            offsetToEntries.put(offset, list);
        }
        return list;
    }

    public IRegion getScopeForSelection(final int offset, final int len) {
        final int endOffset = offset + len - 1;
        for (int i = offset; i >= 0; i--) {
            //We have to get a scope that starts before the current offset and ends after offset+len
            //If it's the same, we must expand to an outer scope!
            List<ScopeEntry> list = offsetToEntries.get(i);
            if (list != null) {
                ListIterator<ScopeEntry> listIterator = list.listIterator(list.size());
                while (listIterator.hasPrevious()) {
                    ScopeEntry scopeEntry = listIterator.previous();
                    if (scopeEntry.open) {
                        //Only interested in the opening ones at this point
                        Tuple<ScopeEntry, ScopeEntry> tup = idToStartEnd.get(scopeEntry.id);
                        if (i == offset && endOffset == tup.o2.offset) {
                            continue;
                        }
                        if (endOffset > tup.o2.offset) {
                            continue;
                        }

                        return new Region(tup.o1.offset, tup.o2.offset - tup.o1.offset + 1);
                    }
                }
            }
        }
        return null;
    }

    public int startScope(int offset, int type) {
        scopeId++;
        List<ScopeEntry> list = getAtOffset(offset);
        ScopeEntry startEntry = new ScopeEntry(scopeId, type, true, offset);
        list.add(startEntry);
        idToStartEnd.put(scopeId, new Tuple<ScopeEntry, ScopeEntry>(startEntry, null));
        return scopeId;
    }

    public void endScope(int id, int offset, int type) {
        offset--;
        List<ScopeEntry> list = getAtOffset(offset);
        ScopeEntry endEntry = new ScopeEntry(id, type, false, offset);
        idToStartEnd.get(id).o2 = endEntry;
        list.add(endEntry);
    }

    public FastStringBuffer debugString(Object doc) {
        BaseParsingUtils utils = BaseParsingUtils.create(doc);
        FastStringBuffer temp = new FastStringBuffer(utils.len() + (utils.len() / 10));

        int len = utils.len();
        for (int i = 0; i < len; i++) {
            char c = utils.charAt(i);
            printEntries(temp, i, true);
            temp.append(c);
            printEntries(temp, i, false);
        }
        return temp;
    }

    private void printEntries(FastStringBuffer temp, int i, boolean opening) {
        List<ScopeEntry> list = offsetToEntries.get(i);
        if (list != null) {
            for (ScopeEntry e : list) {
                if (e.open == opening) {
                    e.toString(temp);
                }
            }
        }
    }
}
