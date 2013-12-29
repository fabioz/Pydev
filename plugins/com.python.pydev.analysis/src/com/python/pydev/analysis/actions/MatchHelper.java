/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import java.util.List;

import org.eclipse.ui.dialogs.SearchPattern;
import org.python.pydev.shared_core.callbacks.ICallback2;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Helper matching scopes vs declaring module names and the actual name of the token.
 */
public class MatchHelper {

    /**
     * Matches according to scopes.
     */
    public static boolean matchItem(SearchPattern patternMatcher, IInfo info) {
        //We want to match the package name in the beggining too...
        String pattern = patternMatcher.getPattern();
        List<String> split = StringUtils.splitAndRemoveEmptyTrimmed(pattern, '.');
        if (split.size() <= 1) {
            if (pattern.endsWith(".")) {
                split.add("");
            } else {
                return patternMatcher.matches(info.getName());
            }
        }

        //Otherwise, we have more things to match... We could match something like:
        //django.AAA -- which should match all the modules that start with django and the tokens that have AAA.

        String declaringModuleName = info.getDeclaringModuleName();
        if (declaringModuleName == null || declaringModuleName.length() == 0) {
            return false;
        }
        List<String> moduleParts = StringUtils.splitAndRemoveEmptyTrimmed(declaringModuleName, '.');

        while (split.size() > 1) {
            String head = split.remove(0);
            SearchPattern headPattern = new SearchPattern();
            headPattern.setPattern(head);
            if (moduleParts.size() == 0) {
                return false; //we cannot match it anymore
            }
            if (!headPattern.matches(moduleParts.remove(0))) {
                return false;
            }
        }
        //if it got here, we've matched the module correctly... let's go on and check the name.

        SearchPattern tailPattern = new SearchPattern();
        tailPattern.setPattern(split.get(0));
        return tailPattern.matches(info.getName());
    }

    /**
     * Checks if equals considering scopes.
     */
    public static boolean equalsFilter(String thisPattern, String otherPattern) {
        return checkPatternSubparts(thisPattern, otherPattern, new ICallback2<Boolean, SearchPattern, SearchPattern>() {

            public Boolean call(SearchPattern thisP, SearchPattern otherP) {
                if (!(thisP.equalsPattern(otherP))) {
                    return false;
                }
                return true;
            }
        });
    }

    /**
     * Checks if it's a sub-filter considering scopes.
     */
    public static boolean isSubFilter(String thisPattern, String otherPattern) {
        return checkPatternSubparts(thisPattern, otherPattern, new ICallback2<Boolean, SearchPattern, SearchPattern>() {

            public Boolean call(SearchPattern thisP, SearchPattern otherP) {
                if (!(thisP.isSubPattern(otherP))) {
                    return false;
                }
                return true;
            }
        });
    }

    private static boolean checkPatternSubparts(String thisPattern, String otherPattern,
            ICallback2<Boolean, SearchPattern, SearchPattern> check) {
        boolean thisEndsWithPoint = thisPattern.endsWith(".");
        boolean otherEndsWithPoint = otherPattern.endsWith(".");
        if (thisEndsWithPoint != otherEndsWithPoint) {
            return false;
        }

        List<String> thisSplit = StringUtils.splitAndRemoveEmptyNotTrimmed(thisPattern, '.');
        List<String> otherSplit = StringUtils.splitAndRemoveEmptyNotTrimmed(otherPattern, '.');

        if (thisEndsWithPoint) {
            thisSplit.add("");
        }
        if (otherEndsWithPoint) {
            otherSplit.add("");
        }

        if (thisSplit.size() != otherSplit.size()) {
            return false;
        }

        for (int i = 0; i < thisSplit.size(); i++) {
            String thisStr = thisSplit.get(i);
            String otherStr = otherSplit.get(i);
            SearchPattern thisP = new SearchPattern();
            thisP.setPattern(thisStr);

            SearchPattern otherP = new SearchPattern();
            otherP.setPattern(otherStr);
            if (!check.call(thisP, otherP)) {
                return false;
            }
        }
        return true;
    }
}
