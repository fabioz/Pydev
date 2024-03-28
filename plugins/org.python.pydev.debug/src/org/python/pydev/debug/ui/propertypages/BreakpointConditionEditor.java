/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.propertypages;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.core.partition.PyPartitioner;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyBreakpoint;
import org.python.pydev.debug.ui.PythonSourceViewer;

public class BreakpointConditionEditor {

    private boolean fIsValid;

    private String fOldValue;
    private String fErrorMessage;

    private IDocumentListener fDocumentListener;

    private PythonBreakpointPage fPage;

    private PyBreakpoint fBreakpoint;

    private PythonSourceViewer fViewer;

    public BreakpointConditionEditor(Composite parent, PythonBreakpointPage page) {
        fPage = page;
        fBreakpoint = fPage.getBreakpoint();
        String condition;
        try {
            condition = fBreakpoint.getCondition();
        } catch (DebugException e) {
            PydevDebugPlugin.log(IStatus.ERROR, "Can't read conditions", e);
            return;
        }
        fErrorMessage = "Enter a condition"; //$NON-NLS-1$
        fOldValue = ""; //$NON-NLS-1$

        // the source viewer
        fViewer = new PythonSourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fViewer.setInput(parent);

        IDocument document = new Document();
        IDocumentPartitioner partitioner = new PyPartitioner(new PyPartitionScanner(), IPythonPartitions.types);
        document.setDocumentPartitioner(partitioner);
        partitioner.connect(document);
        fViewer.setEditable(true);
        fViewer.setDocument(document);
        final IUndoManager undoManager = new TextViewerUndoManager(100);
        fViewer.setUndoManager(undoManager);
        undoManager.connect(fViewer);

        fViewer.getTextWidget().setFont(JFaceResources.getTextFont());

        Control control = fViewer.getControl();
        GridData gd = new GridData(GridData.FILL_BOTH);
        control.setLayoutData(gd);

        // listener for check the value
        fDocumentListener = new IDocumentListener() {
            @Override
            public void documentAboutToBeChanged(DocumentEvent event) {
            }

            @Override
            public void documentChanged(DocumentEvent event) {
                valueChanged();
            }
        };
        fViewer.getDocument().addDocumentListener(fDocumentListener);

        gd = (GridData) fViewer.getControl().getLayoutData();
        gd.heightHint = fPage.convertHeightInCharsToPixels(10);
        gd.widthHint = fPage.convertWidthInCharsToPixels(40);
        document.set(condition);
        valueChanged();

    }

    /**
     * Returns the condition defined in the source viewer.
     * @return the contents of this condition editor
     */
    public String getCondition() {
        return fViewer.getDocument().get();
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#refreshValidState()
     */
    protected void refreshValidState() {
        // the value is valid if the field is not editable, or if the value is not empty
        if (!fViewer.isEditable()) {
            fPage.removeErrorMessage(fErrorMessage);
            fIsValid = true;
        } else {
            String text = fViewer.getDocument().get();
            fIsValid = text != null && text.trim().length() > 0;
            if (!fIsValid) {
                fPage.addErrorMessage(fErrorMessage);
            } else {
                fPage.removeErrorMessage(fErrorMessage);
            }
        }
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#setEnabled(boolean, org.eclipse.swt.widgets.Composite)
     */
    public void setEnabled(boolean enabled) {
        fViewer.setEditable(enabled);
        fViewer.getTextWidget().setEnabled(enabled);
        if (enabled) {
            fViewer.updateViewerColors();
            fViewer.getTextWidget().setFocus();
        } else {
            Color color = fViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
            fViewer.getTextWidget().setBackground(color);
        }
        valueChanged();
    }

    protected void valueChanged() {
        refreshValidState();

        String newValue = fViewer.getDocument().get();
        if (!newValue.equals(fOldValue)) {
            fOldValue = newValue;
        }
    }

    public void dispose() {
        fViewer.getDocument().removeDocumentListener(fDocumentListener);
        fViewer.dispose();
    }

}
