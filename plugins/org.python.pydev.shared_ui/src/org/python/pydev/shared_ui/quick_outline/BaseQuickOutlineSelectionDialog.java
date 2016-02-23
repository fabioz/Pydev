/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.quick_outline;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.shared_core.structure.DataAndImageTreeNode;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.dialogs.DialogMemento;
import org.python.pydev.shared_ui.dialogs.TreeSelectionDialog;

public abstract class BaseQuickOutlineSelectionDialog extends TreeSelectionDialog {

    /**
     * Structure without parents.
     */
    protected DataAndImageTreeNode<Object> root;

    /**
     * Structure with the parent methods.
     */
    protected DataAndImageTreeNode<Object> rootWithParents;

    /**
     * The first line selected (starts at 1)
     */
    protected int startLineIndex = -1;

    protected DataAndImageTreeNode<Object> initialSelection;

    /**
     * Helper to save/restore geometry.
     */
    private final DialogMemento memento;

    /**
     * Listener to handle the 2nd ctrl+O
     */
    private KeyListener ctrlOlistener;

    /**
     * Should we show the parents or not?
     */
    private boolean showParentHierarchy;

    /**
     * Label indicating what are we showing.
     */
    private Label labelCtrlO;

    protected final UIJob uiJobSetRootWithParentsInput = new UIJob("Set input") {

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            if (!monitor.isCanceled()) {
                getTreeViewer().setInput(rootWithParents);
            } else {
                //Will be recalculated if asked again!
                rootWithParents = null;
            }

            return Status.OK_STATUS;
        }
    };

    private final boolean canShowParentHierarchy;

    protected BaseQuickOutlineSelectionDialog(Shell shell, String pluginId, ILabelProvider labelProvider,
            ITreeContentProvider contentProvider, boolean canShowParentHierarchy) {
        super(shell, labelProvider, contentProvider);
        setShellStyle(getShellStyle() & ~SWT.APPLICATION_MODAL); //Not modal because then the user may cancel the progress.
        if (SharedUiPlugin.getDefault() != null) {
            memento = new DialogMemento(getShell(), pluginId + ".actions.QuickShowOutline");
        } else {
            memento = null;
        }
        this.canShowParentHierarchy = canShowParentHierarchy;

        setMessage("Filter (press enter to go to selected element)");
        setTitle("Quick Outline");
        setAllowMultiple(false);
        this.showParentHierarchy = false;
    }

    protected void toggleShowParentHierarchy() {
        showParentHierarchy = !showParentHierarchy;
        updateShowParentHierarchyMessage();

        TreeViewer treeViewer = this.getTreeViewer();
        if (showParentHierarchy) {
            //Create the DataAndImageTreeNode structure if it's still not created...
            calculateHierarchyWithParents();
        } else {
            calculateHierarchy();
            treeViewer.setInput(root);
        }
    }

    protected abstract void calculateHierarchy();

    protected abstract void calculateHierarchyWithParents();

    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        //Whenever the shell is deactivated, we want to go on and close it (i.e.: work as a popup dialog)
        shell.addShellListener(new ShellListener() {

            @Override
            public void shellIconified(ShellEvent e) {
            }

            @Override
            public void shellDeiconified(ShellEvent e) {
            }

            @Override
            public void shellDeactivated(ShellEvent e) {
                shell.close();
            }

            @Override
            public void shellClosed(ShellEvent e) {
            }

            @Override
            public void shellActivated(ShellEvent e) {
            }
        });

    }

    @Override
    public Control createDialogArea(Composite parent) {
        if (memento != null) {
            memento.readSettings();
        }
        Control ret = super.createDialogArea(parent);
        if (canShowParentHierarchy) {
            ctrlOlistener = new KeyListener() {

                @Override
                public void keyReleased(KeyEvent e) {
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if ((e.keyCode == 'o' || e.keyCode == 'O') && e.stateMask == SWT.CTRL) {
                        toggleShowParentHierarchy();
                    }
                }
            };
            this.text.addKeyListener(ctrlOlistener);
        }

        this.text.addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.LF || e.keyCode == SWT.KEYPAD_CR) {
                    okPressed();
                }
            }
        });

        if (canShowParentHierarchy) {
            this.getTreeViewer().getTree().addKeyListener(ctrlOlistener);
        }
        return ret;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.dialogs.SelectionStatusDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createButtonBar(Composite parent) {
        if (canShowParentHierarchy) {
            labelCtrlO = new Label(parent, SWT.NONE);
            this.labelCtrlO.addKeyListener(ctrlOlistener);
            updateShowParentHierarchyMessage();
        }
        return labelCtrlO;
    }

    protected void updateShowParentHierarchyMessage() {
        if (canShowParentHierarchy) {
            if (showParentHierarchy) {
                labelCtrlO.setText("Press Ctrl+O to hide parent hierarchy.");
            } else {
                labelCtrlO.setText("Press Ctrl+O to show parent hierarchy.");
            }
        }
    }

    @Override
    public boolean close() {
        if (memento != null) {
            memento.writeSettings(getShell());
        }
        return super.close();
    }

    @Override
    protected Point getInitialSize() {
        if (memento != null) {
            return memento.getInitialSize(super.getInitialSize(), getShell());
        }
        return new Point(640, 480);
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        if (memento != null) {
            return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
        }
        return new Point(250, 250);
    }

    @Override
    protected int getDefaultMargins() {
        return 0;
    }

    @Override
    protected int getDefaultSpacing() {
        return 0;
    }
}
