package com.python.pydev.analysis.search_index;

import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_ui.search.AbstractSearchResultsViewerFilter;

public class SearchResultsViewerFilter extends AbstractSearchResultsViewerFilter {
    public SearchResultsViewerFilter(String text) {
        super(text);
    }

    @Override
    public boolean isLeafMatch(Viewer viewer, Object element) {
        if (element instanceof ModuleMatch) {
            ModuleMatch moduleMatch = (ModuleMatch) element;
            element = moduleMatch.getLineElement();
        }
        if (element instanceof TreeNode<?>) {
            element = ((TreeNode<?>) element).data;
        }
        if (element instanceof ModuleLineElement) {
            ModuleLineElement moduleLineElement = (ModuleLineElement) element;
            String moduleName = moduleLineElement.modulesKey.name;
            if (filterMatches(moduleName, stringMatcher)) {
                return false;
            }
            return true;
        }

        if (element instanceof CustomModule) {
            CustomModule package1 = (CustomModule) element;
            String moduleName = package1.modulesKey.name;

            if (filterMatches(moduleName, stringMatcher)) {
                return false;
            }
            return true;
        }

        // If not ModuleLineElement nor CustomModule it's a folder/project, so,
        // never a leaf match.
        return false;
    }
}