package com.python.pydev.analysis.search_index;

import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_ui.search.AbstractSearchResultsViewerFilter;

public class PySearchResultsViewerFilter extends AbstractSearchResultsViewerFilter {
    public PySearchResultsViewerFilter(String text, boolean wholeWord) {
        super(text, wholeWord);
    }

    @Override
    public boolean isLeafMatch(Viewer viewer, Object element) {
        if (element instanceof PyModuleMatch) {
            PyModuleMatch moduleMatch = (PyModuleMatch) element;
            element = moduleMatch.getLineElement();
        }
        if (element instanceof TreeNode<?>) {
            element = ((TreeNode<?>) element).data;
        }
        if (element instanceof PyModuleLineElement) {
            PyModuleLineElement moduleLineElement = (PyModuleLineElement) element;
            String moduleName = moduleLineElement.modulesKey.name;
            if (filterMatches(moduleName, stringMatcher)) {
                return true;
            }
            return false;
        }

        if (element instanceof PyCustomModule) {
            PyCustomModule package1 = (PyCustomModule) element;
            String moduleName = package1.modulesKey.name;

            if (filterMatches(moduleName, stringMatcher)) {
                return true;
            }
            return false;
        }

        // If not PyModuleLineElement nor PyCustomModule it's a folder/project, so,
        // never a leaf match.
        return false;
    }
}