package org.python.pydev.ast.codecompletion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Helper to calculate priorities for completions/auto import.
 */
public class PriorityLRU {

    private static LinkedHashSet<String> lru;
    private static Map<String, Integer> priorityMap;
    private static final String COMPLETIONS_PRIORITY_LRU = "COMPLETIONS_PRIORITY_LRU";
    private static final int MAX_SIZE = 100;

    public static Integer getPriority(String realImportRep) {
        if (priorityMap == null) {
            loadLRU();
            updatePriorityMapFromLRU();
        }
        if (priorityMap == null) {
            return null; // If the preferences aren't available it won't be loaded. 
        }
        return priorityMap.get(realImportRep);
    }

    private static void loadLRU() {
        IEclipsePreferences eclipsePreferences = PydevPrefs.getEclipsePreferences();
        if (eclipsePreferences == null) {
            return;
        }
        String string = eclipsePreferences.get(COMPLETIONS_PRIORITY_LRU, "");
        List<String> split = StringUtils.split(string, '|');

        LinkedHashSet<String> temp = new LinkedHashSet<>(MAX_SIZE);
        temp.addAll(split);
        shrinkLRU(temp);
        lru = temp;
    }

    private static void updatePriorityMapFromLRU() {
        if (lru == null) {
            return;
        }
        Map<String, Integer> map = new HashMap<>(lru.size());
        int i = IPyCompletionProposal.LOWER_PRIORITY - 1;

        for (String s : lru) {
            i -= 1;
            map.put(s, i);
        }
        priorityMap = map;
    }

    public static void appliedCompletion(String realImportRep) {
        IEclipsePreferences eclipsePreferences = PydevPrefs.getEclipsePreferences();
        if (eclipsePreferences == null) {
            return;
        }
        if (lru == null) {
            loadLRU();
        } else {
            lru.remove(realImportRep);
            lru.add(realImportRep);
            shrinkLRU(lru);
        }
        updatePriorityMapFromLRU();

        eclipsePreferences.put(COMPLETIONS_PRIORITY_LRU, StringUtils.join("|", lru));
    }

    private static void shrinkLRU(LinkedHashSet<String> lru) {
        if (lru.size() > MAX_SIZE) {
            Iterator<String> iterator = lru.iterator();
            while (lru.size() > MAX_SIZE) {
                iterator.next();
                iterator.remove();
            }
        }
    }

}
