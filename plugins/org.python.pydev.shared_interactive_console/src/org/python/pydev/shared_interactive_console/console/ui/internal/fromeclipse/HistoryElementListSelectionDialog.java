/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_interactive_console.console.ui.internal.fromeclipse;

import java.util.Arrays;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * A class to select elements out of a list of elements.
 * 
 * @since 2.0
 */
public class HistoryElementListSelectionDialog extends AbstractHistoryElementListSelectionDialog {

    private Object[] fElements;

    /**
     * Creates a list selection dialog.
     * @param parent   the parent widget.
     * @param renderer the label renderer.
     */
    public HistoryElementListSelectionDialog(Shell parent, ILabelProvider renderer) {
        super(parent, renderer);
    }

    /**
     * Sets the elements of the list.
     * @param elements the elements of the list.
     */
    public void setElements(Object[] elements) {
        fElements = elements;
    }

    /*
     * @see SelectionStatusDialog#computeResult()
     */
    @Override
    protected void computeResult() {
        setResult(Arrays.asList(getSelectedElements()));
    }

    /*
     * @see Dialog#createDialogArea(Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite contents = (Composite) super.createDialogArea(parent);

        createMessageArea(contents);
        createFilteredList(contents);
        createFilterText(contents);

        setListElements(fElements);
        setSelectionIndices(new int[] { fElements.length - 1 });

        setSelection(getInitialElementSelections().toArray());

        return contents;
    }
}
