/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.utils.tablecombo;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerRow;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

/**
 * From http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.nebula/org.eclipse.nebula.widgets.tablecombo/src/org/eclipse/nebula/?root=Technology_Project
 * 
 * TableComboViewerRow is basically identical to the TableRow class with a
 * few modifications to reference the TableComboViewer row instead of a standar
 * TableViewer row.
 *
 */
/**
 * @author martyj
 *
 */
public class TableComboViewerRow extends ViewerRow {
    private TableItem item;

    /**
     * Create a new instance of the receiver from item.
     * @param item
     */
    TableComboViewerRow(TableItem item) {
        this.item = item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getBounds(int columnIndex) {
        return item.getBounds(columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getBounds() {
        return item.getBounds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Widget getItem() {
        return item;
    }

    void setItem(TableItem item) {
        this.item = item;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return item.getParent().getColumnCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getBackground(int columnIndex) {
        return item.getBackground(columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Font getFont(int columnIndex) {
        return item.getFont(columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getForeground(int columnIndex) {
        return item.getForeground(columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(int columnIndex) {
        return item.getImage(columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(int columnIndex) {
        return item.getText(columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBackground(int columnIndex, Color color) {
        item.setBackground(columnIndex, color);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFont(int columnIndex, Font font) {
        item.setFont(columnIndex, font);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setForeground(int columnIndex, Color color) {
        item.setForeground(columnIndex, color);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImage(int columnIndex, Image image) {
        Image oldImage = item.getImage(columnIndex);
        if (oldImage != image) {
            item.setImage(columnIndex, image);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setText(int columnIndex, String text) {
        item.setText(columnIndex, text == null ? "" : text); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Control getControl() {
        return item.getParent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewerRow getNeighbor(int direction, boolean sameLevel) {
        if (direction == ViewerRow.ABOVE) {
            return getRowAbove();
        } else if (direction == ViewerRow.BELOW) {
            return getRowBelow();
        } else {
            throw new IllegalArgumentException("Illegal value of direction argument."); //$NON-NLS-1$
        }
    }

    private ViewerRow getRowAbove() {
        int index = item.getParent().indexOf(item) - 1;

        if (index >= 0) {
            return new TableComboViewerRow(item.getParent().getItem(index));
        }

        return null;
    }

    private ViewerRow getRowBelow() {
        int index = item.getParent().indexOf(item) + 1;

        if (index < item.getParent().getItemCount()) {
            TableItem tmp = item.getParent().getItem(index);
            //TODO NULL can happen in case of VIRTUAL => How do we deal with that
            if (tmp != null) {
                return new TableComboViewerRow(tmp);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreePath getTreePath() {
        return new TreePath(new Object[] { item.getData() });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        return new TableComboViewerRow(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getElement() {
        return item.getData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVisualIndex(int creationIndex) {
        int[] order = item.getParent().getColumnOrder();

        for (int i = 0; i < order.length; i++) {
            if (order[i] == creationIndex) {
                return i;
            }
        }

        return super.getVisualIndex(creationIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCreationIndex(int visualIndex) {
        if (item != null && !item.isDisposed() && hasColumns() && isValidOrderIndex(visualIndex)) {
            return item.getParent().getColumnOrder()[visualIndex];
        }
        return super.getCreationIndex(visualIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getTextBounds(int index) {
        return item.getTextBounds(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getImageBounds(int index) {
        return item.getImageBounds(index);
    }

    private boolean hasColumns() {
        return this.item.getParent().getColumnCount() != 0;
    }

    private boolean isValidOrderIndex(int currentIndex) {
        return currentIndex < this.item.getParent().getColumnOrder().length;
    }

    @Override
    protected boolean scrollCellIntoView(int columnIndex) {
        item.getParent().showItem(item);
        if (hasColumns()) {
            item.getParent().showColumn(item.getParent().getColumn(columnIndex));
        }

        return true;
    }
}
