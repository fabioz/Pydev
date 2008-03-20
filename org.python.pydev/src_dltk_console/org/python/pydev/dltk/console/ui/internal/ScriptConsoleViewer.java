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
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
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
import org.python.pydev.dltk.console.ui.ScriptConsoleContentAssistant;
import org.python.pydev.dltk.console.ui.internal.actions.HandleBackspaceAction;
import org.python.pydev.dltk.console.ui.internal.actions.HandleDeletePreviousWord;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This is the viewer for the console.
 */
public class ScriptConsoleViewer extends TextConsoleViewer implements IScriptConsoleViewer, IScriptConsoleViewer2ForDocumentListener {

    private class ScriptConsoleStyledText extends StyledText {

        private HandleBackspaceAction handleBackspaceAction;
        private HandleDeletePreviousWord handleDeletePreviousWord;
        
		public ScriptConsoleStyledText(Composite parent, int style) {
            super(parent, style);
            handleBackspaceAction = new HandleBackspaceAction();
            handleDeletePreviousWord = new HandleDeletePreviousWord();
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
                        handleBackspaceAction.execute(getDocument(), getCaretPosition(), getCommandLineOffset());
                        return;

                    case ST.DELETE_NEXT:
                        if (getCaretOffset() < getCommandLineOffset()) {
                            return;
                        }
                        break;

                    case ST.DELETE_WORD_PREVIOUS:
                    	handleDeletePreviousWord.execute(getDocument(), getCaretPosition(), getCommandLineOffset());
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

    private boolean inCompletion = false;
    
    private ScriptConsoleHistory history;

    private ScriptConsoleDocumentListener listener;

    IConsoleStyleProvider styleProvider;

	protected ScriptConsole console;
    
    public IConsoleStyleProvider getStyleProvider() {
        return styleProvider;
    }

    /**
     * @return the caret position (based on the document)
     */
    public int getCaretPosition() {
        return getTextWidget().getCaretOffset();
    }

    /**
     * Sets the new caret position in the console.
     */
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

    /**
     * Constructor 
     * 
     * @param parent parent for this viewer
     * @param console the console that this viewer is showing
     * @param contentHandler
     */
    public ScriptConsoleViewer(Composite parent, ScriptConsole console,
            final IScriptConsoleContentHandler contentHandler) {
        super(parent, console);

        this.console = console;
        
        this.styleProvider = createStyleProvider();

        this.history = console.getHistory();

        this.listener = new ScriptConsoleDocumentListener(this, console, console.getPrompt(), console.getHistory());
        
        this.listener.setDocument(getDocument());

        final StyledText styledText = getTextWidget();

        styledText.addFocusListener(new FocusListener() {

        	/**
        	 * When the initial focus is gained, set the caret position to the last position (just after the prompt)
        	 */
            public void focusGained(FocusEvent e) {
                setCaretPosition(getDocument().getLength());
                //just a 1-time listener
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
                        	//in a new line, always set the caret to the end of the document
                            getTextWidget().setCaretOffset(getDocument().getLength());
                            
                            //if we had an enter with the shift pressed and we're in a completion, we must stop it
                            if(inCompletion && (event.stateMask & SWT.SHIFT) != 0){
                            	((ScriptConsoleContentAssistant)ScriptConsoleViewer.this.fContentAssistant).hide();
                            }
                            return;
                        }
                        
                        if (event.character == SWT.ESC) {
                        	if(!inCompletion){
                        		//while in a completion, esc won't clear the line (just stop the completion)
                        		listener.setCommandLine("");
                        	}
                        	return;
                        }
                        
                        if (PyContentAssistant.matchesContentAssistKeybinding(event)) {
                            event.doit = false;
                            return;
                        }
                    }else{ //not printable char
                        if (isCaretOnLastLine()) {
	                    	if(event.keyCode == SWT.PAGE_UP){
	                    		event.doit = false;
	                    		System.out.println("Up");
	                    		return;
	                    	}
	                    	
	                    	if(event.keyCode == SWT.PAGE_DOWN){
	                    		event.doit = false;
	                    		System.out.println("Down");
	                    		return;
	                    	}
                        }
                    }
                } catch (Exception e) {
                    PydevPlugin.log(e);
                }
            }
        });

        styledText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
            	if (getCaretPosition() >= getCommandLineOffset()){
	                if (PyContentAssistant.matchesContentAssistKeybinding(e)) {
	                    contentHandler.contentAssistRequired();
	                }
            	}
            }

            public void keyReleased(KeyEvent e) {
            }
        });

        clear();
    }


    /**
     * Listen to the completions because we've to know when we're doing a completion or not.
     */
    @Override
    public void configure(SourceViewerConfiguration configuration) {
    	super.configure(configuration);
    	if(fContentAssistant != null){
    		((IContentAssistantExtension2)fContentAssistant).addCompletionListener(new ICompletionListener(){

    			public void assistSessionStarted(ContentAssistEvent event) {
    				inCompletion = true;
    			}
    			
				public void assistSessionEnded(ContentAssistEvent event) {
					inCompletion = false;
				}

				public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
				}}
    		);
    	}
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
