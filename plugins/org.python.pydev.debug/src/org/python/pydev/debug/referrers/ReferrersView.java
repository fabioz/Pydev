package org.python.pydev.debug.referrers;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.shared_ui.tree.PyFilteredTree;
import org.python.pydev.shared_ui.utils.UIUtils;
import org.python.pydev.tree.FileTreeLabelProvider;
import org.python.pydev.tree.FileTreePyFilesProvider;

public class ReferrersView extends ViewPart {

    private PyFilteredTree filter;

    private TreeViewer viewer;

    private static final String REFERRERS_VIEW_ID = "org.python.pydev.views.ReferrersView";

    public ReferrersView() {
    }

    private void configureToolBar() {
        IViewSite viewSite = getViewSite();
        if (viewSite == null) {
            return;
        }
        IActionBars actionBars = viewSite.getActionBars();
        IToolBarManager toolBar = actionBars.getToolBarManager();
        //IMenuManager menuManager = actionBars.getMenuManager(); -- not adding anything to the menu for now.

        IAction showTestRunnerPreferencesAction = new ClearCurrentReferrers(this);
        toolBar.add(showTestRunnerPreferencesAction);

    }

    @Override
    public void createPartControl(Composite parent) {
        configureToolBar();

        PatternFilter patternFilter = new PatternFilter();

        filter = PyFilteredTree.create(parent, patternFilter, true);

        viewer = filter.getViewer();
        viewer.setContentProvider(new FileTreePyFilesProvider());
        viewer.setLabelProvider(new FileTreeLabelProvider());

        GridDataFactory.fillDefaults().grab(true, true).applyTo(filter);
    }

    @Override
    public void setFocus() {
        filter.setFocus();
    }

    public void clear() {

    }

    /**
     * Gets the py unit view. May only be called in the UI thread. If the view is not visible, shows it if the
     * preference to do that is set to true.
     * 
     * Note that it may return null if the preference to show it is false and the view is not currently shown.
     */
    public static ReferrersView getView(boolean forceVisible) {
        return (ReferrersView) UIUtils.getView(REFERRERS_VIEW_ID, forceVisible);
    }

}
