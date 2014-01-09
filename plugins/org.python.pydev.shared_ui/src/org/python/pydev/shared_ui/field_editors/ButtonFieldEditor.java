/******************************************************************************
* Copyright (C) 2008-2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.field_editors;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.shared_ui.tooltips.presenter.ToolTipPresenterHandler;

public class ButtonFieldEditor extends FieldEditor {

    /**
     * Button class
     */
    private Button button;

    /**
     * The selection listener that will do some action when the button is selected
     */
    private final SelectionListener selectionListener;

    private final String tooltip;

    private final ToolTipPresenterHandler tooltipPresenter;

    public ButtonFieldEditor(String name, String buttonText, Composite parent, SelectionListener selectionListener) {
        this(name, buttonText, parent, selectionListener, null, null);
    }

    /**
     * @param name the name of the property
     * @param buttonText the text that'll appear to the user
     * @param parent the parent composite
     * @param selectionListener a listener that'll be executed when the button is clicked
     */
    public ButtonFieldEditor(String name, String buttonText, Composite parent, SelectionListener selectionListener,
            String tooltip, ToolTipPresenterHandler tooltipPresenter) {
        this.tooltip = tooltip;
        init(name, buttonText);
        this.selectionListener = selectionListener;
        this.tooltipPresenter = tooltipPresenter;
        createControl(parent);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        getButtonControl(parent);
    }

    /**
     * Returns this field editor's button component.
     * <p>
     * The button is created if it does not already exist
     * </p>
     *
     * @param parent the parent
     * @return the label control
     */
    public Button getButtonControl(Composite parent) {
        if (button == null) {
            button = new Button(parent, SWT.PUSH);
            button.setFont(parent.getFont());
            String text = getLabelText();
            if (text != null) {
                button.setText(text);
            }
            if (tooltip != null) {
                if (tooltipPresenter != null) {
                    button.setData(ToolTipPresenterHandler.TIP_DATA, tooltip);
                    tooltipPresenter.install(button);
                } else {
                    button.setToolTipText(tooltip);
                }
            }

            button.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    button = null;
                }
            });

            button.addSelectionListener(getSelectionListener());

        } else {
            checkParent(button, parent);
        }
        return button;
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

}
