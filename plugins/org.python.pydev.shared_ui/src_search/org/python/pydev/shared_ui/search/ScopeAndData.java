/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.python.pydev.shared_core.string.StringUtils;

public class ScopeAndData {
    public final int scope;
    public final String scopeData;

    public ScopeAndData(int scope, String scopeData) {
        this.scope = scope;
        this.scopeData = scopeData;
    }

    public Set<String> getModuleNamesFilter() {
        if (this.scope == SearchIndexData.SCOPE_MODULES) {
            return separateSetFromCommas(this.scopeData);
        }
        return new HashSet<>(1);
    }

    public static Set<String> separateSetFromCommas(String data) {
        List<String> split = StringUtils.split(data, ',');
        Set<String> set = new HashSet<>(split.size());
        for (String string : split) {
            string = string.trim();
            if (string.length() > 0) {
                set.add(string);
            }
        }
        return set;
    }

}
