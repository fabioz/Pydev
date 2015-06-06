package com.python.pydev.analysis.search_index;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.shared_core.string.StringMatcher;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.TreeNode;

public class SearchResultsViewerFilter extends ViewerFilter {

    private final StringMatcher[] stringMatcher;

    public SearchResultsViewerFilter(String text) {
        stringMatcher = createMatchers(text);
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof TreeNode<?>) {
            element = ((TreeNode) element).data;
        }
        if (element instanceof CustomModule) {
            CustomModule package1 = (CustomModule) element;
            String moduleName = package1.modulesKey.name;

            if (filterMatches(moduleName, stringMatcher)) {
                return false;
            }
        }
        return true;
    }

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
}