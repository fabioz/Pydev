/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console.ui.internal;


import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.TextConsoleViewer;
import org.python.pydev.dltk.console.ScriptConsoleHistory;
import org.python.pydev.dltk.console.ui.IConsoleStyleProvider;
import org.python.pydev.dltk.console.ui.IScriptConsoleViewer;
import org.python.pydev.dltk.console.ui.ScriptConsole;

/**
 * This is the viewer for the console.
 */
public class ScriptConsoleViewer extends TextConsoleViewer implements IScriptConsoleViewer, IScriptConsoleViewer2ForDocumentListener {

    private class ScriptConsoleStyledText extends StyledText {

        public ScriptConsoleStyledText(Composite parent, int style) {
            super(parent, style);
        }

        public void invokeAction(int action) {
            if (isCaretOnLastLine()) {
                try {
                    switch (action) {
                    case ST.LINE_UP:
                        history.prev();
                        listener.setCommandLine(history.get());
                        setCaretOffset(getDocument().getLength());
                        return;

                    case ST.LINE_DOWN:
                        history.next();
                        listener.setCommandLine(history.get());
                        setCaretOffset(getDocument().getLength());
                        return;

                    case ST.DELETE_PREVIOUS:
                        if (getCaretOffset() <= getCommandLineOffset()) {
                            return;
                        }
                        break;

                    case ST.DELETE_NEXT:
                        if (getCaretOffset() < getCommandLineOffset()) {
                            return;
                        }
                        break;

                    case ST.DELETE_WORD_PREVIOUS:
                        return;
                    }

                } catch (BadLocationException e) {
                    e.printStackTrace();
                    return;
                }

                super.invokeAction(action);

                if (isCaretOnLastLine() && getCaretOffset() <= getCommandLineOffset()) {
                    setCaretOffset(getCommandLineOffset());
                }
            } else {

                super.invokeAction(action);
            }
        }

        public void paste() {
            if (isCaretOnLastLine()) {
                super.paste();
            }
        }
    }

    private ScriptConsoleHistory history;

    private ScriptConsoleDocumentListener listener;

    IConsoleStyleProvider styleProvider;
    
    public IConsoleStyleProvider getStyleProvider() {
        return styleProvider;
    }

    public int getCaretPosition() {
        return getTextWidget().getCaretOffset();
    }

    public void setCaretPosition(final int offset) {
        getTextWidget().getDisplay().asyncExec(new Runnable() {
            public void run() {
                getTextWidget().setCaretOffset(offset);
            }
        });
    }

    public int beginLineOffset() throws BadLocationException {
        IDocument doc = getDocument();
        int offset = getCaretPosition();
        int line = doc.getLineOfOffset(offset);
        return offset - doc.getLineOffset(line);
    }

    protected boolean isCaretOnLastLine() {
        try {
            IDocument doc = getDocument();
            int line = doc.getLineOfOffset(getCaretPosition());
            return line == doc.getNumberOfLines() - 1;
        } catch (BadLocationException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected StyledText createTextWidget(Composite parent, int styles) {
        return new ScriptConsoleStyledText(parent, styles);
    }

    public ScriptConsoleViewer(Composite parent, ScriptConsole console,
            final IScriptConsoleContentHandler contentHandler) {
        super(parent, console);

        this.styleProvider = createStyleProvider();

        this.history = console.getHistory();

        this.listener = new ScriptConsoleDocumentListener(this, console, console.getPrompt(), console.getHistory());
        this.listener.setDocument(getDocument());

        final StyledText styledText = getTextWidget();

        // styledText.setEditable(false);

        // Correct keyboard actions
        styledText.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                setCaretPosition(getDocument().getLength());
                styledText.removeFocusListener(this);
            }

            public void focusLost(FocusEvent e) {

            }
        });

        styledText.addVerifyKeyListener(new VerifyKeyListener() {
            public void verifyKey(VerifyEvent event) {
                try {
                    if (event.character != '\0') {
                        // Printable character
                        if (!isCaretOnLastLine()) {
                            event.doit = false;
                            return;
                        }

                        if (beginLineOffset() < listener.getLastLineReadOnlySize()) {
                            event.doit = false;
                            return;
                        }

                        if (event.character == SWT.CR) {
                            getTextWidget().setCaretOffset(getDocument().getLength());
                            return;
                        }
                        
                        if (event.keyCode == 32 && (event.stateMask & SWT.CTRL) > 0) {
                            event.doit = false;
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        styledText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                // if (e.keyCode == 32 && (e.stateMask & SWT.CTRL) > 0) {
                if ((e.keyCode == 32 && (e.stateMask & SWT.CTRL) > 0)) {
                    // System.out.println(".keyPressed()");
                    contentHandler.contentAssistRequired();
                }

            }

            public void keyReleased(KeyEvent e) {
            }
        });

        clear();
    }

    /**
     * Can be overridden to create a style provider for the console.
     * @return a style provider.
     */
    protected IConsoleStyleProvider createStyleProvider() {
        return new IConsoleStyleProvider(){

            public StyleRange createInterpreterErrorStyle(String content, int offset) {
                return new StyleRange(offset, content.length(), Display.getDefault().getSystemColor(SWT.COLOR_RED),
                        Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            }

            public StyleRange createInterpreterOutputStyle(String content, int offset) {
                return new StyleRange(offset, content.length(), Display.getDefault().getSystemColor(SWT.COLOR_BLACK),
                        Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            }

            public StyleRange createPromptStyle(String prompt, int offset) {
                return new StyleRange(offset, prompt.length(), Display.getDefault().getSystemColor(SWT.COLOR_GREEN),
                        Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            }

            public StyleRange createUserInputStyle(String content, int offset) {
                return new StyleRange(offset, content.length(), Display.getDefault().getSystemColor(SWT.COLOR_BLUE),
                        Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            }
            
        };
    }

    // IConsoleTextViewer
    public String getCommandLine() {
        try {
            return listener.getCommandLine();
        } catch (BadLocationException e) {
            return null;
        }
    }

    public int getCommandLineOffset() {
        try {
            return listener.getCommandLineOffset();
        } catch (BadLocationException e) {
            return -1;
        }
    }

    public void clear() {
        listener.clear();
    }

    public void insertText(String text) {
        getTextWidget().append(text);
    }

    public boolean canDoOperation(int operation) {
        boolean canDoOperation = super.canDoOperation(operation);

        if (canDoOperation) {
            switch (operation) {
            case CUT:
            case DELETE:
            case PASTE:
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
            case PREFIX:
            case STRIP_PREFIX:
                canDoOperation = isCaretOnLastLine();
            }
        }

        return canDoOperation;
    }

    public void setStyleProvider(IConsoleStyleProvider provider) {
        this.styleProvider = provider;
    }
    
    /*
     * Overridden just to change visibility.
     * (non-Javadoc)
     * @see org.eclipse.ui.console.TextConsoleViewer#revealEndOfDocument()
     */
    @Override
    public void revealEndOfDocument() {
        super.revealEndOfDocument();
    }
}
