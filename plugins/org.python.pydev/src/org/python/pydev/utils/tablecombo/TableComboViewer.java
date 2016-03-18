/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.utils.tablecombo;

import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

/**
 * From http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.nebula/org.eclipse.nebula.widgets.tablecombo/src/org/eclipse/nebula/?root=Technology_Project
 * 
 * TableComboViewerRow is basically identical to the TableViewer class with a
 * few modifications to reference the Table within the TableCombo widget 
 * instead of a parent Table widget.
  */
public class TableComboViewer extends AbstractTableViewer {

    private TableCombo tableCombo;

    /**
     * The cached row which is reused all over
     */
    private TableComboViewerRow cachedRow;

    public TableComboViewer(Composite parent) {
        this(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    }

    /**
     * Creates a table viewer on a newly-created table control under the given
     * parent. The table control is created using the given style bits. The
     * viewer has no input, no content provider, a default label provider, no
     * sorter, and no filters. The table has no columns.
     * 
     * @param parent
     * 		the parent control
     * @param style
     * 		SWT style bits
     */
    public TableComboViewer(Composite parent, int style) {
        this(new TableCombo(parent, style));
    }

    /**
     * Creates a table viewer on the given table control. The viewer has no
     * input, no content provider, a default label provider, no sorter, and no
     * filters.
     * 
     * @param table
     * 		the table control
     */
    public TableComboViewer(TableCombo tableCombo) {
        this.tableCombo = tableCombo;
        hookControl(tableCombo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doClear(int index) {
        tableCombo.getTable().clear(index);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doClearAll() {
        tableCombo.getTable().clearAll();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDeselectAll() {
        tableCombo.getTable().deselectAll();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Widget doGetColumn(int index) {
        return tableCombo.getTable().getColumn(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item doGetItem(int index) {
        return tableCombo.getTable().getItem(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int doGetItemCount() {
        return tableCombo.getTable().getItemCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item[] doGetItems() {
        return tableCombo.getTable().getItems();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item[] doGetSelection() {
        return tableCombo.getTable().getSelection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int[] doGetSelectionIndices() {
        return tableCombo.getTable().getSelectionIndices();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int doIndexOf(Item item) {
        return tableCombo.getTable().indexOf((TableItem) item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRemove(int[] indices) {
        tableCombo.getTable().remove(indices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRemove(int start, int end) {
        tableCombo.getTable().remove(start, end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRemoveAll() {
        tableCombo.getTable().removeAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doResetItem(Item item) {
        TableItem tableItem = (TableItem) item;
        int columnCount = Math.max(1, tableCombo.getTable().getColumnCount());
        for (int i = 0; i < columnCount; i++) {
            tableItem.setText(i, ""); //$NON-NLS-1$
            if (tableItem.getImage(i) != null) {
                tableItem.setImage(i, null);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSelect(int[] indices) {
        tableCombo.select(indices != null && indices.length > 0 ? indices[0] : -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetItemCount(int count) {
        tableCombo.getTable().setItemCount(count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetSelection(Item[] items) {
        if (items != null && items.length > 0) {
            tableCombo.select(tableCombo.getTable().indexOf((TableItem) items[0]));
        } else {
            tableCombo.select(-1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetSelection(int[] indices) {
        tableCombo.select(indices != null && indices.length > 0 ? indices[0] : -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doShowItem(Item item) {
        tableCombo.getTable().showItem((TableItem) item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doShowSelection() {
        tableCombo.getTable().showSelection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ViewerRow internalCreateNewRowPart(int style, int rowIndex) {
        TableItem item;

        if (rowIndex >= 0) {
            item = new TableItem(tableCombo.getTable(), style, rowIndex);
        } else {
            item = new TableItem(tableCombo.getTable(), style);
        }

        return getViewerRowFromItem(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnViewerEditor createViewerEditor() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int doGetColumnCount() {
        return tableCombo.getTable().getColumnCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item getItemAt(Point point) {
        return tableCombo.getTable().getItem(point);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ViewerRow getViewerRowFromItem(Widget item) {
        if (cachedRow == null) {
            cachedRow = new TableComboViewerRow((TableItem) item);
        } else {
            cachedRow.setItem((TableItem) item);
        }

        return cachedRow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Control getControl() {
        return tableCombo;
    }

    /**
     * returns the TableCombo reference.
     * @return
     */
    public TableCombo getTableCombo() {
        return tableCombo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
        super.handleLabelProviderChanged(event);
        setSelection(getSelection());
    }
}
