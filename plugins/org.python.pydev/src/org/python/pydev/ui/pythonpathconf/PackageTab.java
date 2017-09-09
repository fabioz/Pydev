package org.python.pydev.ui.pythonpathconf;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.pythonpathconf.package_manager.AbstractPackageManager;
import org.python.pydev.ui.pythonpathconf.package_manager.CondaPackageManager;
import org.python.pydev.ui.pythonpathconf.package_manager.PipPackageManager;

public class PackageTab {

    private Composite boxPackage;
    private volatile InterpreterInfo interpreterInfo;
    private Tree tree;

    AbstractPackageManager packageManager;
    private Button btConda;
    private Button btPip;

    public void createPackageControlTab(TabFolder tabFolder) {
        Composite parent;
        GridData gd;
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("Packages");
        ImageCache imageCache = PydevPlugin.getImageCache();

        tabItem.setImage(imageCache.get(UIConstants.FOLDER_PACKAGE_ICON));

        Composite composite = new Composite(tabFolder, SWT.None);
        parent = composite;
        GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);

        tree = new Tree(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        tree.setHeaderVisible(true);
        createColumn("Library", 300);
        createColumn("Version", 100);
        createColumn("", 100);
        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 1;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        tree.setLayoutData(gd);

        //buttons at the side of the tree
        Composite control = getButtonBoxPackage(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        control.setLayoutData(gd);

        tabItem.setControl(composite);
    }

    public Composite getButtonBoxPackage(Composite parent) {
        if (boxPackage == null) {
            boxPackage = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            boxPackage.setLayout(layout);
            btPip = AbstractInterpreterEditor.createBt(boxPackage, "Manage pip", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    AbstractPackageManager packageManager = new PipPackageManager(interpreterInfo);
                    packageManager.manage();
                }
            });
            btConda = AbstractInterpreterEditor.createBt(boxPackage, "Manage conda", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    AbstractPackageManager packageManager = new CondaPackageManager(interpreterInfo);
                    packageManager.manage();
                }
            });
        } else {
            checkParent(boxPackage, parent);
        }

        return boxPackage;
    }

    protected void checkParent(Control control, Composite parent) {
        Assert.isTrue(control.getParent() == parent, "Different parents");//$NON-NLS-1$
    }

    public void setInfo(InterpreterInfo info) {
        this.interpreterInfo = info;
        this.update();
    }

    private TreeColumn createColumn(String text, int width) {
        TreeColumn col;
        col = new TreeColumn(tree, SWT.LEFT);
        col.setText(text);
        if (width > 0) {
            col.setWidth(width);
        }
        col.setMoveable(true);

        return col;
    }

    private void update() {
        makeUIClean();
        packageManager = null;
        if (interpreterInfo == null) {
            return;
        }

        final TreeItem loadingItem = new TreeItem(tree, SWT.None);
        loadingItem.setText(new String[] { "Loading info...", "" });
        synchronized (listJobLock) {
            if (listJob != null) {
                listJob.cancel();
            }
            listJob = new ListJob(interpreterInfo);
            listJob.schedule();
        }
    }

    private void makeUIClean() {
        if (tree.isDisposed()) {
            return;
        }
        TreeColumn column = tree.getColumn(0);
        column.setText("Library");
        tree.clearAll(true);
        tree.setItemCount(0);
        btConda.setEnabled(false);
        btPip.setEnabled(false);
    }

    private class ListJob extends Job {

        private final IInterpreterInfo initialInfo;
        private AbstractPackageManager packageManager;

        public ListJob(IInterpreterInfo initialInfo) {
            super("List dependencies for: " + initialInfo.getNameForUI());
            this.initialInfo = initialInfo;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            packageManager = AbstractPackageManager.createPackageManager(interpreterInfo);
            if (initialInfo != interpreterInfo || monitor.isCanceled() || tree.isDisposed()) {
                return Status.OK_STATUS;
            }
            final List<String[]> list = packageManager.list();

            RunInUiThread.async(() -> {
                // Update tree only in UI thread!
                if (initialInfo != interpreterInfo || tree.isDisposed() || monitor.isCanceled()) {
                    return;
                }
                makeUIClean();
                packageManager.updateTree(tree, list);
                btPip.setEnabled(true);
                if (packageManager instanceof CondaPackageManager) {
                    btConda.setEnabled(true);
                }
            });
            return Status.OK_STATUS;
        }
    }

    private final static Object listJobLock = new Object();
    private volatile static ListJob listJob;

}
