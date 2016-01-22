/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.python.pydev.core.ObjectsInternPool;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * @author fabioz
 *
 */
public class InfoStrFactory {

    /**
     * @param iInfo
     * @return
     */
    public static String infoToString(List<IInfo> iInfo) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();

        FastStringBuffer infos = new FastStringBuffer();

        int next = 0;
        map.put(null, next);
        next++;

        for (Iterator<IInfo> it = iInfo.iterator(); it.hasNext();) {
            IInfo info = it.next();
            infos.append("&&");
            infos.append(info.getType());
            String name = info.getName();
            String declaringModuleName = info.getDeclaringModuleName();
            String path = info.getPath();

            next = add(map, infos, next, name);
            next = add(map, infos, next, declaringModuleName);
            next = add(map, infos, next, path);
        }

        FastStringBuffer header = new FastStringBuffer("INFOS:", map.size() * 30);
        Set<Entry<String, Integer>> entrySet = map.entrySet();

        //null is always 0 (not written to header)
        header.append(infos);
        header.append('\n');
        map.remove(null);
        for (Entry<String, Integer> entry : entrySet) {
            header.append(entry.getKey());
            header.append("=");
            header.append(entry.getValue());
            header.append("\n");
        }

        return header.toString();
    }

    private static int add(HashMap<String, Integer> map, FastStringBuffer infos, int next, String d) {
        Integer v = map.get(d);
        if (v == null) {
            v = next;
            map.put(d, next);
            next++;
        }
        infos.append("|");
        infos.append(v);
        return next;
    }

    /**
     * @param s
     * 
     * Some string as:
     * 
     * INFOS:&&2|1|2|0&&1|3|4|0&&2|1|2|0
     * Class=3
     * ClassMod=4
     * Foo=2
     * Bar=1
     * 
     * where number 0 is always null and the others are the numbers mapped as needed.
     */
    public static List<IInfo> strToInfo(String s) {
        if (!s.startsWith("INFOS:")) {
            return null;
        }
        s = s.substring(6);

        Iterable<String> iterLines = StringUtils.iterLines(s);
        Iterator<String> linesIt = iterLines.iterator();
        if (!linesIt.hasNext()) {
            return null;
        }
        String firstLine = linesIt.next().trim(); //line with the infos (we must read the other parts to actually 'get' it).

        HashMap<Integer, String> map = new HashMap<Integer, String>();
        map.put(0, null);
        synchronized (ObjectsInternPool.lock) {
            while (linesIt.hasNext()) {
                String line = linesIt.next().trim();
                int i = StringUtils.rFind(line, '=');
                if (i > 0) {
                    String token = line.substring(0, i);
                    String value = line.substring(i + 1);

                    map.put(Integer.parseInt(value), ObjectsInternPool.internUnsynched(token));
                }
            }
        }

        ArrayList<IInfo> ret = new ArrayList<IInfo>();

        List<String> split = StringUtils.split(firstLine, "&&");
        for (String string : split) {
            List<String> parts = StringUtils.split(string, '|');
            int type = Integer.parseInt(parts.get(0));
            int name = Integer.parseInt(parts.get(1));
            int declaringModuleName = Integer.parseInt(parts.get(2));
            int path = Integer.parseInt(parts.get(3));

            switch (type) {
                case AbstractInfo.NAME_WITH_IMPORT_TYPE:
                    //no intern construct (already interned when creating the map)
                    ret.add(new NameInfo(map.get(name), map.get(declaringModuleName), map.get(path), true));
                    break;

                case AbstractInfo.ATTRIBUTE_WITH_IMPORT_TYPE:
                    //no intern construct (already interned when creating the map)
                    ret.add(new AttrInfo(map.get(name), map.get(declaringModuleName), map.get(path), true));
                    break;

                case AbstractInfo.METHOD_WITH_IMPORT_TYPE:
                    //no intern construct (already interned when creating the map)
                    ret.add(new FuncInfo(map.get(name), map.get(declaringModuleName), map.get(path), true));
                    break;

                case AbstractInfo.CLASS_WITH_IMPORT_TYPE:
                    //no intern construct (already interned when creating the map)
                    ret.add(new ClassInfo(map.get(name), map.get(declaringModuleName), map.get(path), true));
                    break;

            }
        }
        return ret;
    }

}
