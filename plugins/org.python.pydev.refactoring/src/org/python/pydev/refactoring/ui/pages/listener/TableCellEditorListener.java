/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.pages.listener;

import java.util.Observable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.refactoring.ui.pages.core.SimpleTableItem;

public class TableCellEditorListener extends Observable implements Listener {

    private final Table table;

    private IValidationPage wizard;

    public TableCellEditorListener(IValidationPage wizard, Table parametersTable) {
        this.wizard = wizard;
        this.table = parametersTable;
    }

    /**
     * http://www.eclipse.org/swt/snippets/
     */
    @Override
    public void handleEvent(Event event) {

        final TableEditor editor = new TableEditor(table);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;

        Rectangle clientArea = table.getClientArea();
        if (table.getSelection().length != 1) {
            return;
        }

        Rectangle bounds = table.getSelection()[0].getBounds();
        Point pt = new Point(bounds.x, bounds.y);
        int index = table.getTopIndex();
        while (index < table.getItemCount()) {
            boolean visible = false;
            final SimpleTableItem item = (SimpleTableItem) table.getItem(index);
            for (int i = 0; i < table.getColumnCount(); i++) {
                Rectangle rect = item.getBounds(i);
                if (rect.contains(pt)) {

                    final Text text = new Text(table, SWT.NONE);
                    Listener textListener = new TextListener(item, text);

                    text.addListener(SWT.FocusOut, textListener);
                    text.addListener(SWT.Traverse, textListener);
                    text.addListener(SWT.FocusOut, wizard);
                    editor.setEditor(text, item, i);
                    text.setText(item.getText(i));
                    text.selectAll();
                    text.setFocus();
                    return;
                }
                if (!visible && rect.intersects(clientArea)) {
                    visible = true;
                }
            }
            if (!visible) {
                return;
            }
            index++;
        }
    }

    private final class TextListener implements Listener {
        private SimpleTableItem tableItem;

        private final Text text;

        private TextListener(SimpleTableItem item, Text text) {
            this.tableItem = item;
            this.text = text;
        }

        @Override
        public void handleEvent(final Event e) {
            if (e.type == SWT.FocusOut) {
                tableItem.setText(text.getText());
                text.dispose();
                table.setFocus();
            } else if (e.type == SWT.Traverse) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    tableItem.setText(text.getText());
                    e.doit = true;
                }
                if (e.detail == SWT.TRAVERSE_RETURN || e.detail == SWT.TRAVERSE_ESCAPE) {
                    text.dispose();
                    if (e.detail == SWT.TRAVERSE_ESCAPE) {
                        e.doit = false;
                    }
                }
            }
        }
    }
}
