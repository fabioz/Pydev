/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 3, 2006
 */
package org.python.pydev.shared_ui.field_editors;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class MultiStringFieldEditor extends StringFieldEditor {
    /**
     * Text limit of text field in characters; initially unlimited.
     */
    private int textLimit = UNLIMITED;

    /**
     * The validation strategy; 
     * <code>VALIDATE_ON_KEY_STROKE</code> by default.
     */
    private int validateStrategy = VALIDATE_ON_KEY_STROKE;

    /**
     * The text field, or <code>null</code> if none.
     */
    Text textField;

    private boolean fillVertically;

    public MultiStringFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
    }

    public MultiStringFieldEditor(String name, String labelText, Composite parent, boolean fillVertically) {
        super(name, labelText, parent);
        this.fillVertically = fillVertically;
    }

    @Override
    public Text getTextControl() {
        return textField;
    }

    /**
     * Returns this field editor's text control.
     * <p>
     * The control is created if it does not yet exist
     * </p>
     *
     * @param parent the parent
     * @return the text control
     */
    @Override
    public Text getTextControl(Composite parent) {
        if (textField == null) {
            textField = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
            textField.setFont(parent.getFont());
            switch (validateStrategy) {
                case VALIDATE_ON_KEY_STROKE:
                    textField.addKeyListener(new KeyAdapter() {

                        /* (non-Javadoc)
                         * @see org.eclipse.swt.events.KeyAdapter#keyReleased(org.eclipse.swt.events.KeyEvent)
                         */
                        @Override
                        public void keyReleased(KeyEvent e) {
                            valueChanged();
                        }
                    });

                    break;
                case VALIDATE_ON_FOCUS_LOST:
                    textField.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            clearErrorMessage();
                        }
                    });
                    textField.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent e) {
                            refreshValidState();
                        }

                        @Override
                        public void focusLost(FocusEvent e) {
                            valueChanged();
                            clearErrorMessage();
                        }
                    });
                    break;
                default:
                    Assert.isTrue(false, "Unknown validate strategy");//$NON-NLS-1$
            }
            textField.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent event) {
                    textField = null;
                }
            });
            if (textLimit > 0) {//Only set limits above 0 - see SWT spec
                textField.setTextLimit(textLimit);
            }
        } else {
            checkParent(textField, parent);
        }
        return textField;
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        GridData gd = (GridData) textField.getLayoutData();
        gd.horizontalSpan = numColumns - 1;
        // We only grab excess space if we have to
        // If another field editor has more columns then
        // we assume it is setting the width.
        gd.grabExcessHorizontalSpace = gd.horizontalSpan == 1;
        gd.grabExcessVerticalSpace = true;
        gd.heightHint = 200;
        if (this.fillVertically) {
            gd.verticalAlignment = SWT.FILL;
        }
    }

    /**
     * Fills this field editor's basic controls into the given parent.
     * <p>
     * The string field implementation of this <code>FieldEditor</code>
     * framework method contributes the text field. Subclasses may override
     * but must call <code>super.doFillIntoGrid</code>.
     * </p>
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        adjustForNumColumns(numColumns);
    }

}
