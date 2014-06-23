/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.tooltips.presenter.ToolTipPresenterHandler;

/**
 * Listener to do the sorting in the tree.
 */
final class PyUnitSortListener implements Listener {

    private PyUnitView view;

    PyUnitSortListener(PyUnitView view) {
        this.view = view;
    }

    public void handleEvent(Event e) {
        Tree tree = view.getTree();
        TreeItem[] items = tree.getItems();
        TreeColumn column = (TreeColumn) e.widget;
        Comparator<TreeItem> comparator = null;
        final int col;
        if (column == view.colIndex) {
            col = -1;
            comparator = new Comparator<TreeItem>() {
                public int compare(TreeItem o1, TreeItem o2) {
                    String txt0 = o1.getText(PyUnitView.COL_INDEX);
                    String txt1 = o2.getText(PyUnitView.COL_INDEX);
                    try {
                        int number0 = Integer.parseInt(txt0.trim());
                        int number1 = Integer.parseInt(txt1.trim());
                        if (number0 < number1) {
                            return -1;
                        } else if (number1 < number0) {
                            return 1;
                        }
                        return 0;
                    } catch (NumberFormatException e) {
                        Log.log(e);
                    }
                    return txt0.compareTo(txt1);
                }
            };

        } else if (column == view.colResult) {
            col = PyUnitView.COL_RESULT;

        } else if (column == view.colTest) {
            col = PyUnitView.COL_TEST;

        } else if (column == view.colFile) {
            col = PyUnitView.COL_FILENAME;

        } else if (column == view.colTime) {
            col = -1;
            comparator = new Comparator<TreeItem>() {
                public int compare(TreeItem o1, TreeItem o2) {
                    String txt0 = o1.getText(PyUnitView.COL_TIME);
                    String txt1 = o2.getText(PyUnitView.COL_TIME);
                    try {
                        float float0 = Float.parseFloat(txt0.trim());
                        float float1 = Float.parseFloat(txt1.trim());
                        if (float0 < float1) {
                            return -1;
                        } else if (float1 < float0) {
                            return 1;
                        }
                        return 0;
                    } catch (NumberFormatException e) {
                        Log.log(e);
                    }
                    return txt0.compareTo(txt1);
                }
            };
        } else {
            Log.log("Could not recognize column clicked: " + column);
            return;
        }

        if (comparator == null) {
            comparator = new Comparator<TreeItem>() {
                public int compare(TreeItem o1, TreeItem o2) {
                    return o1.getText(col).compareTo(o2.getText(col));
                }
            };
        }

        TreeColumn oldSortColumn = tree.getSortColumn();
        if (oldSortColumn == column) {
            //inverse the direction
            int sortDirection = tree.getSortDirection();
            if (sortDirection == SWT.DOWN) {
                tree.setSortDirection(SWT.UP);
                final Comparator<TreeItem> oldComparator = comparator;
                comparator = new Comparator<TreeItem>() {

                    public int compare(TreeItem o1, TreeItem o2) {
                        return -oldComparator.compare(o1, o2);
                    }
                };
            } else {
                tree.setSortDirection(SWT.DOWN);
            }
        } else {
            //new column selected (sort direction always down)
            tree.setSortDirection(SWT.DOWN);
        }
        Arrays.sort(items, comparator);
        String[][] strings = new String[items.length][PyUnitView.NUMBER_OF_COLUMNS];
        Object[][] results = new PyUnitTestResult[items.length][2];
        for (int i = 0; i < items.length; i++) {
            TreeItem it = items[i];
            for (int j = 0; j < PyUnitView.NUMBER_OF_COLUMNS; j++) {
                strings[i][j] = it.getText(j);
            }
            results[i][0] = it.getData(ToolTipPresenterHandler.TIP_DATA);
            results[i][1] = it.getData(PyUnitView.PY_UNIT_TEST_RESULT);
        }

        tree.setRedraw(false);
        try {
            Color errorColor = view.getErrorColor();
            for (int i = 0; i < strings.length; i++) {
                TreeItem item = tree.getItem(i);
                item.setText(strings[i]);
                item.setData(ToolTipPresenterHandler.TIP_DATA, results[i][0]);
                PyUnitTestResult result = (PyUnitTestResult) results[i][1];
                item.setData(PyUnitView.PY_UNIT_TEST_RESULT, result);
                if (result.isOk()) {
                    item.setForeground(null);

                } else if (result.isSkip()) {
                    item.setForeground(null);

                } else {
                    item.setForeground(errorColor);
                }
            }
        } finally {
            tree.setRedraw(true);
        }
        tree.setSortColumn(column);
    }
}