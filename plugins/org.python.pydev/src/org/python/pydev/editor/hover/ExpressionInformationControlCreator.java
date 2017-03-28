package org.python.pydev.editor.hover;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.model.elements.ElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.variables.details.DefaultDetailPane;
import org.eclipse.debug.internal.ui.views.variables.details.DetailPaneProxy;
import org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.utils.UIUtils;

/**
 * Creates an information control to display an expression in a hover control.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * 
 * @since 3.3
 */
public class ExpressionInformationControlCreator implements IInformationControlCreator {

    class ExpressionInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

        /**
         * Dialog setting key for height
         */
        private static final String HEIGHT = "HEIGHT"; //$NON-NLS-1$

        /**
         * Dialog setting key for width. 
         */
        private static final String WIDTH = "WIDTH"; //$NON-NLS-1$

        /**
         * Dialog setting key for tree sash weight
         */
        private static final String SASH_WEIGHT_TREE = "SashWeightTree"; //$NON-NLS-1$

        /**
         * Dialog setting key for details sash weight
         */
        private static final String SASH_WEIGHT_DETAILS = "SashWeightDetails"; //$NON-NLS-1$		

        /**
         * Variable to display.
         */
        private IVariable fVariable;

        private IPresentationContext fContext;
        private TreeModelViewer fViewer;
        private SashForm fSashForm;
        private Composite fDetailPaneComposite;
        private DetailPaneProxy fDetailPane;
        private Tree fTree;

        /**
         * Creates the content for the root element of the tree viewer in the hover
         */
        private class TreeRoot extends ElementContentProvider {
            /* (non-Javadoc)
             * @see org.eclipse.debug.internal.ui.viewers.model.provisional.elements.ElementContentProvider#getChildCount(java.lang.Object, org.eclipse.debug.internal.ui.viewers.provisional.IPresentationContext)
             */
            @Override
            protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor)
                    throws CoreException {
                return 1;
            }

            /* (non-Javadoc)
             * @see org.eclipse.debug.internal.ui.viewers.model.provisional.elements.ElementContentProvider#getChildren(java.lang.Object, int, int, org.eclipse.debug.internal.ui.viewers.provisional.IPresentationContext)
             */
            @Override
            protected Object[] getChildren(Object parent, int index, int length, IPresentationContext context,
                    IViewerUpdate monitor) throws CoreException {
                return new Object[] { fVariable };
            }

            /* (non-Javadoc)
             * @see org.eclipse.debug.internal.ui.viewers.model.provisional.elements.ElementContentProvider#supportsContextId(java.lang.String)
             */
            @Override
            protected boolean supportsContextId(String id) {
                return true;
            }
        }

        /**
         * Inner class implementing IDetailPaneContainer methods.  Handles changes to detail
         * pane and provides limited access to the detail pane proxy.
         */
        private class DetailPaneContainer implements IDetailPaneContainer {

            /* (non-Javadoc)
             * @see org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer#getCurrentPaneID()
             */
            @Override
            public String getCurrentPaneID() {
                return fDetailPane.getCurrentPaneID();
            }

            /* (non-Javadoc)
             * @see org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer#getCurrentSelection()
             */
            @Override
            public IStructuredSelection getCurrentSelection() {
                return (IStructuredSelection) fViewer.getSelection();
            }

            /* (non-Javadoc)
             * @see org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer#refreshDetailPaneContents()
             */
            @Override
            public void refreshDetailPaneContents() {
                fDetailPane.display(getCurrentSelection());
            }

            /* (non-Javadoc)
             * @see org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer#getParentComposite()
             */
            @Override
            public Composite getParentComposite() {
                return fDetailPaneComposite;
            }

            /* (non-Javadoc)
             * @see org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer#getWorkbenchPartSite()
             */
            @Override
            public IWorkbenchPartSite getWorkbenchPartSite() {
                return null;
            }

            /* (non-Javadoc)
             * @see org.eclipse.debug.internal.ui.views.variables.details.IDetailPaneContainer#paneChanged(java.lang.String)
             */
            @Override
            public void paneChanged(String newPaneID) {
                if (newPaneID.equals(DefaultDetailPane.ID)) {
                    fDetailPane.getCurrentControl()
                            .setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
                    fDetailPane.getCurrentControl()
                            .setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                }
            }

        }

        /**
         * Constructs a new control in the given shell.
         * 
         * @param parentShell shell
         * @param resize whether resize is supported
         */
        ExpressionInformationControl(Shell parentShell, boolean resize) {
            super(parentShell, resize);
            create();
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.AbstractInformationControl#computeSizeHint()
         */
        @Override
        public Point computeSizeHint() {
            IDialogSettings settings = getDialogSettings(false);
            if (settings != null) {
                int x = getIntSetting(settings, WIDTH);
                if (x > 0) {
                    int y = getIntSetting(settings, HEIGHT);
                    if (y > 0) {
                        return new Point(x, y);
                    }
                }
            }
            return super.computeSizeHint();
        }

        /**
         * Returns the dialog settings for this hover or <code>null</code> if none
         * 
         * @param create whether to create the settings
         */
        private IDialogSettings getDialogSettings(boolean create) {
            IDialogSettings settings = PydevPlugin.getDefault().getDialogSettings();
            IDialogSettings section = settings.getSection(this.getClass().getName());
            if (section == null & create) {
                section = settings.addNewSection(this.getClass().getName());
            }
            return section;
        }

        /**
         * Returns an integer value in the given dialog settings or -1 if none.
         * 
         * @param settings dialog settings
         * @param key key
         * @return value or -1 if not present
         */
        private int getIntSetting(IDialogSettings settings, String key) {
            try {
                return settings.getInt(key);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.AbstractInformationControl#dispose()
         */
        @Override
        public void dispose() {
            persistSettings(getShell());
            fContext.dispose();
            super.dispose();
        }

        /**
         * Persists dialog settings.
         * 
         * @param shell
         */
        private void persistSettings(Shell shell) {
            if (shell != null && !shell.isDisposed()) {
                if (isResizable()) {
                    IDialogSettings settings = getDialogSettings(true);
                    Point size = shell.getSize();
                    settings.put(WIDTH, size.x);
                    settings.put(HEIGHT, size.y);
                    int[] weights = fSashForm.getWeights();
                    settings.put(SASH_WEIGHT_TREE, weights[0]);
                    settings.put(SASH_WEIGHT_DETAILS, weights[1]);
                }
            }
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.AbstractInformationControl#setVisible(boolean)
         */
        @Override
        public void setVisible(boolean visible) {
            if (!visible) {
                persistSettings(getShell());
            }
            super.setVisible(visible);
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.AbstractInformationControl#createContent(org.eclipse.swt.widgets.Composite)
         */
        @Override
        protected void createContent(Composite parent) {

            fSashForm = new SashForm(parent, parent.getStyle());
            fSashForm.setOrientation(SWT.VERTICAL);

            // update presentation context
            AbstractDebugView view = getViewToEmulate();
            fContext = new PresentationContext(IDebugUIConstants.ID_VARIABLE_VIEW);
            if (view != null) {
                // copy over properties
                IPresentationContext copy = ((TreeModelViewer) view.getViewer()).getPresentationContext();
                String[] properties = copy.getProperties();
                for (int i = 0; i < properties.length; i++) {
                    String key = properties[i];
                    fContext.setProperty(key, copy.getProperty(key));
                }
            }

            fViewer = new TreeModelViewer(fSashForm, SWT.NO_TRIM | SWT.MULTI | SWT.VIRTUAL, fContext);
            fViewer.setAutoExpandLevel(1);

            if (view != null) {
                // copy over filters
                StructuredViewer structuredViewer = (StructuredViewer) view.getViewer();
                if (structuredViewer != null) {
                    ViewerFilter[] filters = structuredViewer.getFilters();
                    for (int i = 0; i < filters.length; i++) {
                        fViewer.addFilter(filters[i]);
                    }
                }
            }

            fDetailPaneComposite = SWTFactory.createComposite(fSashForm, 1, 1, GridData.FILL_BOTH);
            Layout layout = fDetailPaneComposite.getLayout();
            if (layout instanceof GridLayout) {
                GridLayout gl = (GridLayout) layout;
                gl.marginHeight = 0;
                gl.marginWidth = 0;
            }

            fDetailPane = new DetailPaneProxy(new DetailPaneContainer());
            fDetailPane.display(null); // Bring up the default pane so the user doesn't see an empty composite

            fTree = fViewer.getTree();
            fTree.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    fDetailPane.display((IStructuredSelection) fViewer.getSelection());
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });

            initSashWeights();

            // add update listener to auto-select and display details of root expression
            fViewer.addViewerUpdateListener(new IViewerUpdateListener() {
                @Override
                public void viewerUpdatesComplete() {
                }

                @Override
                public void viewerUpdatesBegin() {
                }

                @Override
                public void updateStarted(IViewerUpdate update) {
                }

                @Override
                public void updateComplete(IViewerUpdate update) {
                    if (update instanceof IChildrenUpdate) {
                        TreeSelection selection = new TreeSelection(new TreePath(new Object[] { fVariable }));
                        fViewer.setSelection(selection);
                        fDetailPane.display(selection);
                        fViewer.removeViewerUpdateListener(this);
                    }
                }
            });

            setForegroundColor(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
            setBackgroundColor(getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        }

        /**
         * Attempts to find an appropriate view to emulate, this will either be the
         * variables view or the expressions view.
         * @return a view to emulate or <code>null</code>
         */
        private AbstractDebugView getViewToEmulate() {
            IWorkbenchPage page = UIUtils.getActiveWorkbenchWindow().getActivePage();
            AbstractDebugView expressionsView = (AbstractDebugView) page.findView(IDebugUIConstants.ID_EXPRESSION_VIEW);
            if (expressionsView != null && expressionsView.isVisible()) {
                return expressionsView;
            }
            AbstractDebugView variablesView = (AbstractDebugView) page.findView(IDebugUIConstants.ID_VARIABLE_VIEW);
            if (variablesView != null && variablesView.isVisible()) {
                return variablesView;
            }
            if (expressionsView != null) {
                return expressionsView;
            }
            return variablesView;
        }

        /**
         * Initializes the sash form weights from the preference store (using default values if 
         * no sash weights were stored previously).
         */
        protected void initSashWeights() {
            IDialogSettings settings = getDialogSettings(false);
            if (settings != null) {
                int tree = getIntSetting(settings, SASH_WEIGHT_TREE);
                if (tree > 0) {
                    int details = getIntSetting(settings, SASH_WEIGHT_DETAILS);
                    if (details > 0) {
                        fSashForm.setWeights(new int[] { tree, details });
                    }
                }
            }
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.AbstractInformationControl#setForegroundColor(org.eclipse.swt.graphics.Color)
         */
        @Override
        public void setForegroundColor(Color foreground) {
            super.setForegroundColor(foreground);
            fDetailPaneComposite.setForeground(foreground);
            fTree.setForeground(foreground);
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.AbstractInformationControl#setBackgroundColor(org.eclipse.swt.graphics.Color)
         */
        @Override
        public void setBackgroundColor(Color background) {
            super.setBackgroundColor(background);
            fDetailPaneComposite.setBackground(background);
            fTree.setBackground(background);
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.AbstractInformationControl#setFocus()
         */
        @Override
        public void setFocus() {
            super.setFocus();
            fTree.setFocus();
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.IInformationControlExtension#hasContents()
         */
        @Override
        public boolean hasContents() {
            return fVariable != null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.IInformationControlExtension2#setInput(java.lang.Object)
         */
        @Override
        public void setInput(Object input) {
            if (input instanceof IVariable) {
                fVariable = (IVariable) input;
                fViewer.setInput(new TreeRoot());
            }
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.AbstractInformationControl#getInformationPresenterControlCreator()
         */
        @Override
        public IInformationControlCreator getInformationPresenterControlCreator() {
            return new ExpressionInformationControlCreator() {
                /* (non-Javadoc)
                 * @see org.eclipse.jdt.internal.debug.ui.ExpressionInformationControlCreator#createInformationControl(org.eclipse.swt.widgets.Shell)
                 */
                @Override
                public IInformationControl createInformationControl(Shell shell) {
                    return new ExpressionInformationControl(shell, true);
                }
            };
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.IInformationControlCreator#createInformationControl(org.eclipse.swt.widgets.Shell)
     */
    @Override
    public IInformationControl createInformationControl(Shell parent) {
        return new ExpressionInformationControl(parent, false);
    }

}
