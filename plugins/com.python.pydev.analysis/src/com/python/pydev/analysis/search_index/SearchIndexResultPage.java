package com.python.pydev.analysis.search_index;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.python.pydev.shared_core.structure.TreeNodeContentProvider;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.search.AbstractSearchIndexResultPage;
import org.python.pydev.shared_ui.search.AbstractSearchResultsViewerFilter;
import org.python.pydev.shared_ui.search.GroupByAction;

/**
 * Show line matches when viewing in table
 * Filtering through this UI without requiring a new search
 */
public class SearchIndexResultPage extends AbstractSearchIndexResultPage {

    public SearchIndexResultPage() {
        ImageCache imageCache = SharedUiPlugin.getImageCache();

        fGroupByActions = new Action[] {
                new GroupByAction(this, PySearchIndexTreeContentProvider.GROUP_WITH_PROJECT,
                        imageCache.getDescriptor(UIConstants.PROJECT_ICON), "Group: Projects"),

                new GroupByAction(this, PySearchIndexTreeContentProvider.GROUP_WITH_MODULES,
                        imageCache.getDescriptor(UIConstants.FOLDER_PACKAGE_ICON), "Group: Packages"),

                new GroupByAction(this, PySearchIndexTreeContentProvider.GROUP_WITH_FOLDERS,
                        imageCache.getDescriptor(UIConstants.FOLDER_ICON), "Group: Folders"),

        };
    }

    @Override
    protected AbstractSearchResultsViewerFilter createFilterFilter(String text) {
        return new PySearchResultsViewerFilter(text);
    };

    @Override
    protected TreeNodeContentProvider createTreeContentProvider(TreeViewer viewer) {
        return new PySearchIndexTreeContentProvider(this, viewer);
    }

}
