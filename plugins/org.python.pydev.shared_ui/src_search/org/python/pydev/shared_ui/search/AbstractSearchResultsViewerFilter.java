package org.python.pydev.shared_ui.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.shared_core.string.StringMatcher;
import org.python.pydev.shared_core.string.StringUtils;

public abstract class AbstractSearchResultsViewerFilter extends ViewerFilter {

    protected final StringMatcher[] stringMatcher;
    private static Object[] EMPTY = new Object[0];
    private Map<Object, Boolean> foundAnyCache = new HashMap<>();
    private Map<Object, Object[]> cache = new HashMap<>();

    public AbstractSearchResultsViewerFilter(String text) {
        stringMatcher = createMatchers(text);
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

    public static StringMatcher[] createMatchers(String text) {
        List<String> split = StringUtils.split(text, ',');
        ArrayList<StringMatcher> lst = new ArrayList<>(split.size());
        for (String string : split) {
            string = string.trim();
            if (string.length() > 0) {
                StringMatcher matcher = new StringMatcher(string.trim(), true, false);
                lst.add(matcher);
            }
        }
        return lst.toArray(new StringMatcher[0]);
    }

    public static boolean filterMatches(String moduleName, StringMatcher[] stringMatcher) {
        for (int i = 0; i < stringMatcher.length; i++) {
            StringMatcher matcher = stringMatcher[i];
            if (matcher.match(moduleName, 0, moduleName.length())) {
                return true;
            }
        }
        return false;
    }

    public void clearCache() {
        cache.clear();
        foundAnyCache.clear();
    }

}
