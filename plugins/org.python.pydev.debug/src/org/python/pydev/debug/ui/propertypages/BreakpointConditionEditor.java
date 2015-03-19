/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.propertypages;

import java.util.Map;

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
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.AbstractHandler;
import org.eclipse.ui.commands.ExecutionException;
import org.eclipse.ui.commands.HandlerSubmission;
import org.eclipse.ui.commands.IHandler;
import org.eclipse.ui.commands.IWorkbenchCommandSupport;
import org.eclipse.ui.commands.Priority;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
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

    private HandlerSubmission submission;
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
        /*
        fViewer.configure(new DisplayViewerConfiguration() {
            public IContentAssistProcessor getContentAssistantProcessor() {
                    return getCompletionProcessor();
            }
        });*/
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
            public void documentAboutToBeChanged(DocumentEvent event) {
            }

            public void documentChanged(DocumentEvent event) {
                valueChanged();
            }
        };
        fViewer.getDocument().addDocumentListener(fDocumentListener);

        // we can only do code assist if there is an associated type
        /*
        try {
            //getCompletionProcessor().setType(type);            
            String source= null;
            ICompilationUnit compilationUnit= type.getCompilationUnit();
            if (compilationUnit != null) {
                source= compilationUnit.getSource();
            } else {
                IClassFile classFile= type.getClassFile();
                if (classFile != null) {
                    source= classFile.getSource();
                }
            }
            int lineNumber= fBreakpoint.getMarker().getAttribute(IMarker.LINE_NUMBER, -1);
            int position= -1;
            if (source != null && lineNumber != -1) {
                try {
                    position= new Document(source).getLineOffset(lineNumber - 1);
                } catch (BadLocationException e) {
                }
            }
            //getCompletionProcessor().setPosition(position);
        } catch (CoreException e) {
        }*/

        gd = (GridData) fViewer.getControl().getLayoutData();
        gd.heightHint = fPage.convertHeightInCharsToPixels(10);
        gd.widthHint = fPage.convertWidthInCharsToPixels(40);
        document.set(condition);
        valueChanged();

        IHandler handler = new AbstractHandler() {
            public Object execute(Map parameter) throws ExecutionException {
                fViewer.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
                return null;
            }
        };
        submission = new HandlerSubmission(null, parent.getShell(), null,
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, handler, Priority.MEDIUM);
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
     * Return the completion processor associated with this viewer.
     * @return BreakPointConditionCompletionProcessor
     */
    /*
    protected BreakpointConditionCompletionProcessor getCompletionProcessor() {
     if (fCompletionProcessor == null) {
         fCompletionProcessor= new BreakpointConditionCompletionProcessor(null);
     }
     return fCompletionProcessor;
    }*/

    /**
     * @see org.eclipse.jface.preference.FieldEditor#setEnabled(boolean, org.eclipse.swt.widgets.Composite)
     */
    public void setEnabled(boolean enabled) {
        fViewer.setEditable(enabled);
        fViewer.getTextWidget().setEnabled(enabled);
        if (enabled) {
            fViewer.updateViewerColors();
            fViewer.getTextWidget().setFocus();

            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchCommandSupport commandSupport = workbench.getCommandSupport();
            commandSupport.addHandlerSubmission(submission);
        } else {
            Color color = fViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
            fViewer.getTextWidget().setBackground(color);
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchCommandSupport commandSupport = workbench.getCommandSupport();
            commandSupport.removeHandlerSubmission(submission);
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
        if (fViewer.isEditable()) {
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchCommandSupport commandSupport = workbench.getCommandSupport();
            commandSupport.removeHandlerSubmission(submission);
        }
        fViewer.getDocument().removeDocumentListener(fDocumentListener);
        fViewer.dispose();
    }

}
