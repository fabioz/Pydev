/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.shared_core.string.StringUtils;

public abstract class AbstractSearchResultsViewerFilter extends ViewerFilter {

    protected final IMatcher stringMatcher;
    private static Object[] EMPTY = new Object[0];
    private Map<Object, Boolean> foundAnyCache = new HashMap<>();
    private Map<Object, Object[]> cache = new HashMap<>();

    public AbstractSearchResultsViewerFilter(String text, boolean wholeWord) {
        stringMatcher = createMatcher(text, wholeWord);
    }

    @Override
    public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
        Object[] filtered = cache.get(parent);
        if (filtered == null) {
            Boolean foundAny = foundAnyCache.get(parent);
            if (foundAny != null && !foundAny.booleanValue()) {
                filtered = EMPTY;
            } else {
                filtered = super.filter(viewer, parent, elements);
            }
            cache.put(parent, filtered);
        }
        return filtered;
    }

    @Override
    public final boolean select(Viewer viewer, Object parentElement,
            Object element) {
        return isElementVisible(viewer, element);
    }

    private boolean computeAnyVisible(Viewer viewer, Object[] elements) {
        boolean elementFound = false;
        for (int i = 0; i < elements.length && !elementFound; i++) {
            Object element = elements[i];
            elementFound = isElementVisible(viewer, element);
        }
        return elementFound;
    }

    private boolean isAnyVisible(Viewer viewer, Object parent, Object[] elements) {
        Object[] filtered = cache.get(parent);
        if (filtered != null) {
            return filtered.length > 0;
        }
        Boolean foundAny = foundAnyCache.get(parent);
        if (foundAny == null) {
            foundAny = computeAnyVisible(viewer, elements) ? Boolean.TRUE : Boolean.FALSE;
            foundAnyCache.put(parent, foundAny);
        }
        return foundAny.booleanValue();
    }

    public boolean isElementSelectable(Object element) {
        return element != null;
    }

    public boolean isElementVisible(Viewer viewer, Object element) {
        return isParentMatch(viewer, element) || isLeafMatch(viewer, element);
    }

    protected boolean isParentMatch(Viewer viewer, Object element) {
        Object[] children = ((ITreeContentProvider) ((AbstractTreeViewer) viewer)
                .getContentProvider()).getChildren(element);

        if ((children != null) && (children.length > 0)) {
            return isAnyVisible(viewer, element, children);
        }
        return false;
    }

    public abstract boolean isLeafMatch(Viewer viewer, Object element);

    public static IMatcher createMatcher(String text, boolean wholeWord) {
        List<String> split = StringUtils.split(text, ',');
        ArrayList<StringMatcherWithIndexSemantics> includes = new ArrayList<>(split.size());
        ArrayList<StringMatcherWithIndexSemantics> excludes = new ArrayList<>(split.size());

        for (String string : split) {
            string = string.trim();
            if (string.length() > 0) {
                if (string.startsWith("!")) {
                    StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics(string.substring(1),
                            true, wholeWord);
                    excludes.add(matcher);
                } else {
                    StringMatcherWithIndexSemantics matcher = new StringMatcherWithIndexSemantics(string, true,
                            wholeWord);
                    includes.add(matcher);
                }
            }
        }

        return new IncludeExcludeMatcher(includes.toArray(new StringMatcherWithIndexSemantics[0]),
                excludes.toArray(new StringMatcherWithIndexSemantics[0]));
    }

    public static interface IMatcher {

        boolean match(String text);
    }

    public static class IncludeExcludeMatcher implements IMatcher {

        private final int strategy;
        private final StringMatcherWithIndexSemantics[] includes;
        private final StringMatcherWithIndexSemantics[] excludes;

        private static final int ACCEPT_ALL = 0;
        private static final int ONLY_INCLUDES = 1;
        private static final int ONLY_EXCLUDES = 2;
        private static final int EXCLUDE_AND_INCLUDES = 3;

        public IncludeExcludeMatcher(StringMatcherWithIndexSemantics[] includes,
                StringMatcherWithIndexSemantics[] excludes) {
            this.includes = includes;
            this.excludes = excludes;
            if (includes.length == 0 && excludes.length == 0) {
                strategy = ACCEPT_ALL;

            } else if (includes.length > 0 && excludes.length == 0) {
                strategy = ONLY_INCLUDES;

            } else if (includes.length == 0 && excludes.length > 0) {
                strategy = ONLY_EXCLUDES;

            } else {
                strategy = EXCLUDE_AND_INCLUDES;

            }
        }

        @Override
        public boolean match(String text) {
            final int includesLen = includes.length;
            final int excludesLen = excludes.length;

            switch (strategy) {
                case ACCEPT_ALL:
                    return true;

                case ONLY_INCLUDES:
                    for (int i = 0; i < includesLen; i++) {
                        StringMatcherWithIndexSemantics s = includes[i];
                        if (s.match(text)) {
                            return true;
                        }
                    }
                    return false;

                case ONLY_EXCLUDES:
                    for (int i = 0; i < excludesLen; i++) {
                        StringMatcherWithIndexSemantics s = excludes[i];
                        if (s.match(text)) {
                            return false;
                        }
                    }
                    return true;

                case EXCLUDE_AND_INCLUDES:
                    // If we have includes and excludes, we'll first check if an include matches
                    // and then we'll remove the excludes.
                    for (int i = 0; i < includesLen; i++) {
                        StringMatcherWithIndexSemantics s = includes[i];
                        if (s.match(text)) {
                            for (i = 0; i < excludesLen; i++) {
                                s = excludes[i];
                                if (s.match(text)) {
                                    return false;
                                }
                            }

                            return true;
                        }
                    }
                    return false;

            }
            throw new RuntimeException("Invalid strategy: " + strategy);
        }

    }

    /**
     * @return true if it should be added and false otherwise.
     */
    public static boolean filterMatches(String text, IMatcher stringMatcher) {
        return stringMatcher.match(text);
    }

    public void clearCache() {
        cache.clear();
        foundAnyCache.clear();
    }

}
