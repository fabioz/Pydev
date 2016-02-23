/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.pythonpathconf;

import java.lang.ref.WeakReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Widget;

/**
 * Helper to create a list of strings with buttons for new/remove.
 * 
 * Used for the forced builtins.
 */
abstract/*default*/class AbstractListWithNewRemoveControl extends SelectionAdapter implements DisposeListener {

    protected Composite box;

    private Button addBt;

    private Button removeBt;

    protected List itemsList;

    protected WeakReference<AbstractInterpreterEditor> container;

    public AbstractListWithNewRemoveControl(AbstractInterpreterEditor container) {
        this.container = new WeakReference<AbstractInterpreterEditor>(container);
    }

    /**
     * Creates the tab
     */
    void createTab(String tabLabel, String internalLabel) {
        AbstractInterpreterEditor interpreterEditor = container.get();
        Composite parent;
        GridData gd;
        TabItem tabItem;
        Composite composite;
        Composite control;
        tabItem = new TabItem(interpreterEditor.tabFolder, SWT.None);
        tabItem.setText(tabLabel);

        composite = new Composite(interpreterEditor.tabFolder, SWT.None);
        parent = composite;
        composite.setLayout(new GridLayout(2, false));

        //label
        Link l2 = new Link(parent, SWT.None);
        l2.setText(internalLabel);
        l2.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                Program.launch("http://pydev.org/manual_101_interpreter.html");
            }
        });

        gd = new GridData();
        gd.horizontalSpan = 2;
        l2.setLayoutData(gd);

        //the list with the items
        List list = getListControl(parent);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.heightHint = 200;
        list.setLayoutData(gd);

        //the buttons
        control = getButtonBoxControlOthers(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        control.setLayoutData(gd);
        tabItem.setControl(composite);
    }

    /**
     * Returns this field editor's button box containing the Add and Remove
     * 
     * @param parent the parent control
     * @return the button box
     */
    public Composite getButtonBoxControlOthers(Composite parent) {
        AbstractInterpreterEditor interpreterEditor = this.container.get();
        Assert.isNotNull(interpreterEditor);
        if (box == null) {
            box = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            box.setLayout(layout);
            createButtons(interpreterEditor);
            box.addDisposeListener(this);
        } else {
            checkParent(box, parent);
        }

        return box;
    }

    /**
     * To create a button in a subclass, one must override
     * 
     * - createButtons
     * - widgetDisposed
     * - widgetSelected
     */
    protected void createButtons(AbstractInterpreterEditor interpreterEditor) {
        addBt = interpreterEditor.createBt(box, "ListEditor.add", this);//$NON-NLS-1$
        removeBt = interpreterEditor.createBt(box, "ListEditor.remove", this);//$NON-NLS-1$
    }

    @Override
    public void widgetDisposed(DisposeEvent event) {
        if (addBt != null) {
            addBt.dispose();
            addBt = null;
        }
        if (removeBt != null) {
            removeBt.dispose();
            removeBt = null;
        }
        if (box != null) {
            box.dispose();
            box = null;
        }
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        Widget widget = event.widget;
        if (widget == addBt) {
            addItem();
        } else if (widget == removeBt) {
            removeItem();
        }
    }

    /**
     * Checks if the given parent is the current parent of the
     * supplied control; throws an (unchecked) exception if they
     * are not correctly related.
     *
     * @param control the control
     * @param parent the parent control
     */
    protected void checkParent(Control control, Composite parent) {
        Assert.isTrue(control.getParent() == parent, "Different parents");//$NON-NLS-1$
    }

    /**
     * @param parent
     * @return
     */
    private List getListControl(Composite parent) {
        if (itemsList == null) {
            itemsList = new List(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            itemsList.setFont(parent.getFont());
            itemsList.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent event) {
                    itemsList = null;
                }
            });
        } else {
            checkParent(itemsList, parent);
        }
        return itemsList;
    }

    /**
     * 
     */
    protected void addItem() {
        AbstractInterpreterEditor interpreterEditor = this.container.get();
        Assert.isNotNull(interpreterEditor);

        InterpreterInfo info = interpreterEditor.getSelectedInfo();
        if (info != null) {
            String item = getInput();

            if (item != null) {
                addInputToInfo(info, item);
                interpreterEditor.updateTree();
            }
        }
    }

    /**
     * 
     */
    protected void removeItem() {
        AbstractInterpreterEditor interpreterEditor = this.container.get();
        Assert.isNotNull(interpreterEditor);
        InterpreterInfo info = interpreterEditor.getSelectedInfo();
        if (info != null) {
            String[] selected = itemsList.getSelection();
            removeSelectedFrominfo(info, selected);
            interpreterEditor.updateTree();
        }
    }

    /**
     * Removes all items from the internal list.
     */
    public void removeAllFromList() {
        this.itemsList.removeAll();
    }

    /**
     * Updates the internal list given the passed info.
     */
    public void update(InterpreterInfo info) {
        java.util.List<String> stringsFromInfo = this.getStringsFromInfo(info);
        for (String s : stringsFromInfo) {
            itemsList.add(s);
        }
    }

    /**
     * Subclasses must remove the list of selected strings from the corresponding fields
     * in the interpreter info.
     */
    protected abstract void removeSelectedFrominfo(InterpreterInfo info, String[] item);

    /**
     * Subclasses must return the list of strings that should be added to the gui from the 
     * passed info.
     */
    protected abstract java.util.List<String> getStringsFromInfo(InterpreterInfo info);

    /**
     * Subclasses must add the passed item to the info.
     */
    protected abstract void addInputToInfo(InterpreterInfo info, String item);

    /**
     * Subclasses must override to get the input to be added to the list. If null is returned,
     * nothing is added. 
     */
    protected abstract String getInput();

}
