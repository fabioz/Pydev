/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.ui.search;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog telling the user that files are out of sync or matches
 * are stale and asks for confirmation to refresh/search again
 * @since 3.0
 */

public class SearchAgainConfirmationDialog extends Dialog {
    private List fOutOfSync;
    private List fOutOfDate;
    private ILabelProvider fLabelProvider;

    private class ProxyLabelProvider extends LabelProvider {

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
         */
        public Image getImage(Object element) {
            if (fLabelProvider != null)
                return fLabelProvider.getImage(element);
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
         */
        public String getText(Object element) {
            if (fLabelProvider != null)
                return fLabelProvider.getText(element);
            return null;
        }

    }

    public SearchAgainConfirmationDialog(Shell shell, ILabelProvider labelProvider, List outOfSync, List outOfDate) {
        super(shell);
        fOutOfSync = outOfSync;
        fOutOfDate = outOfDate;
        fLabelProvider = labelProvider;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createDialogArea(Composite parent) {
        Composite result = (Composite) super.createDialogArea(parent);

        if (fOutOfSync.size() > 0) {
            createLabel(result, SearchMessages.SearchAgainConfirmationDialog_outofsync_message);

            createLabel(result, SearchMessages.SearchAgainConfirmationDialog_outofsync_label);
            createTableViewer(fOutOfSync, result);
        } else {
            createLabel(result, SearchMessages.SearchAgainConfirmationDialog_stale_message);
        }

        createLabel(result, SearchMessages.SearchAgainConfirmationDialog_stale_label);
        createTableViewer(fOutOfDate, result);
        return result;
    }

    private void createLabel(Composite parent, String text) {
        Label message = new Label(parent, SWT.WRAP);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = convertWidthInCharsToPixels(70);
        message.setLayoutData(gd);
        message.setText(text);
    }

    private TableViewer createTableViewer(List input, Composite result) {
        TableViewer viewer = new TableViewer(result);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new ProxyLabelProvider());
        viewer.setInput(input);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = convertWidthInCharsToPixels(70);
        gd.heightHint = convertHeightInCharsToPixels(5);
        viewer.getControl().setLayoutData(gd);
        return viewer;
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(SearchMessages.SearchAgainConfirmationDialog_title);
    }

}
