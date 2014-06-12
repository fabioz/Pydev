/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author Fabio Zadrozny
 */
package org.python.pydev.shared_ui.field_editors;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.python.pydev.shared_ui.tooltips.presenter.ToolTipPresenterHandler;

/**
 * Helper class to provide a field that can be used as a link.
 *
 * @note: to actually create a text that can be linked, it must be written as html with <a>text</a>.
 *
 * @author Fabio
 */
public class LinkFieldEditor extends FieldEditor {

    /**
     * Link class
     */
    private Link link;

    /**
     * The selection listener that will do some action when the link is selected
     */
    private final SelectionListener selectionListener;

    private final String tooltip;

    private final ToolTipPresenterHandler tooltipPresenter;

    public LinkFieldEditor(String name, String linkText, Composite parent, SelectionListener selectionListener) {
        this(name, linkText, parent, selectionListener, null, null);
    }

    /**
     * @param name the name of the property
     * @param linkText the text that'll appear to the user
     * @param parent the parent composite
     * @param selectionListener a listener that'll be executed when the linked text is clicked
     */
    public LinkFieldEditor(String name, String linkText, Composite parent, SelectionListener selectionListener,
            String tooltip, ToolTipPresenterHandler tooltipPresenter) {
        this.tooltip = tooltip;
        init(name, linkText);
        this.selectionListener = selectionListener;
        this.tooltipPresenter = tooltipPresenter;
        createControl(parent);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        GridData gd = (GridData) link.getLayoutData();
        gd.horizontalSpan = numColumns;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Link link = getLinkControl(parent);

        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        link.setLayoutData(gd);

    }

    /**
     * Returns this field editor's link component.
     * <p>
     * The link is created if it does not already exist
     * </p>
     *
     * @param parent the parent
     * @return the label control
     */
    public Link getLinkControl(Composite parent) {
        if (link == null) {
            link = new Link(parent, SWT.NONE);
            link.setFont(parent.getFont());
            String text = getLabelText();
            if (text != null) {
                link.setText(text);
            }
            if (tooltip != null) {
                if (tooltipPresenter != null) {
                    link.setData(ToolTipPresenterHandler.TIP_DATA, tooltip);
                    tooltipPresenter.install(link);
                } else {
                    link.setToolTipText(tooltip);
                }
            }

            link.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    link = null;
                }
            });

            link.addSelectionListener(getSelectionListener());

        } else {
            checkParent(link, parent);
        }
        return link;
    }

    public Link getLink() {
        return link;
    }

    private SelectionListener getSelectionListener() {
        return selectionListener;
    }

    @Override
    protected void doLoad() {
    }

    @Override
    protected void doLoadDefault() {
    }

    @Override
    protected void doStore() {
    }

    @Override
    public int getNumberOfControls() {
        return 1;
    }

    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        //super.setEnabled(enabled, parent); -- don't call super!
        link.setEnabled(enabled);
    }
}
