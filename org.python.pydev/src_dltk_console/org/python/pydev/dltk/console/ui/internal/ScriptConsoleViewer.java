/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console.ui.internal;


import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
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
import org.python.pydev.dltk.console.ui.internal.actions.HandleLineStartAction;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This is the viewer for the console. It's responsible for making sure that the actions the 
 * user does are issued in the correct places in the document and that only editable places are
 * actually editable 
 */
public class ScriptConsoleViewer extends TextConsoleViewer implements IScriptConsoleViewer, 
    IScriptConsoleViewer2ForDocumentListener {

    /**
     * Boolean determining if we're currently requesting a completion
     */
    private boolean inCompletion = false;
    
    /**
     * Holds the command history for the console
     */
    private ScriptConsoleHistory history;

    /**
     * Listens and acts to document changes (and passes them to the shell)
     */
    private ScriptConsoleDocumentListener listener;

    /**
     * Provides the colors for the console.
     */
    IConsoleStyleProvider styleProvider;

    /**
     * Console itself
     */
    protected ScriptConsole console;

    /**
     * Attribute defines if this is the main viewer (other viewers may be associated to the same document) 
     */
    private boolean isMainViewer;
    
    /**
     * This class is responsible for checking if commands should be issued or not given the command requested
     * and updating the caret to the correct position for it to happen (if needed).
     */
    private final class KeyChecker implements VerifyKeyListener {
        
        public void verifyKey(VerifyEvent event) {
            try {
                if (event.character != '\0') { // Printable character
                    
                    if(Character.isLetter(event.character) && (event.stateMask == 0 || (event.stateMask & SWT.SHIFT) != 0)){
                        //it's a valid letter without any stateMask (so, just entering regular text or upper/lowercase -- if shift is there).
                        if (!isSelectedRangeEditable()) {
                            getTextWidget().setCaretOffset(getDocument().getLength());
                        }
                        
                    }

                    if (!isSelectedRangeEditable()) {
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
                }else{ //not printable char
                    if (isCaretInEditableRange()) {
                    	if(!inCompletion && event.keyCode == SWT.PAGE_UP){
                    		event.doit = false;
                    		List<String> commands = history.getAsList();
                    		List<String> commandsToExecute = ScriptConsoleHistorySelector.select(commands);
                    		if(commandsToExecute != null){
                    		    //remove the current command (substituted by the one gotten from page up)
                    		    listener.setCommandLine("");
                    		    for(String s:commandsToExecute){
                    		        IDocument d = getDocument();
                    		        d.replace(d.getLength(), 0, s+"\n");
                    		    }
                    		}
                    		return;
                    	}
                    }
                }
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
    }


    /**
     * This is the text widget that's used to edit the console. It has some treatments to handle
     * commands that should act differently (special handling for when the caret is on the last line
     * to execute custom commands, such using the history, etc).
     */
    private class ScriptConsoleStyledText extends StyledText {

        /**
         * Handles a backspace (should guarantee that it does not delete things that are not in the last
         * line -- nor in the prompt)
         */
        private HandleBackspaceAction handleBackspaceAction;
        
        /**
         * Handles a delete previous word (should guarantee that it does not delete things that are not in the last
         * line -- nor in the prompt)
         */
        private HandleDeletePreviousWord handleDeletePreviousWord;

        /**
         * Handles a line start action (home) stays within the same line changing from the 
         * 1st char of text, beggining of prompt, beggining of line.
         */
        private HandleLineStartAction handleLineStartAction;
        
		public ScriptConsoleStyledText(Composite parent, int style) {
            super(parent, style);
            handleBackspaceAction = new HandleBackspaceAction();
            handleDeletePreviousWord = new HandleDeletePreviousWord();
            handleLineStartAction = new HandleLineStartAction();
        }

		
		public void invokeAction(int action) {
		    
		    //some actions have a different scope (not in selected range / out of selected range)
            switch (action) {
                case ST.LINE_START:
                    if(handleLineStartAction.execute(getDocument(), getCaretOffset(), 
                            getCommandLineOffset(), ScriptConsoleViewer.this)){
                        return;
                    }else{
                        super.invokeAction(action);
                    }

            }
            
            
            if (isSelectedRangeEditable()) {
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
                        handleBackspaceAction.execute(getDocument(), getCaretOffset(), getCommandLineOffset());
                        return;

                    case ST.DELETE_WORD_PREVIOUS:
                    	handleDeletePreviousWord.execute(getDocument(), getCaretOffset(), getCommandLineOffset());
                        return;
                    }

                } catch (BadLocationException e) {
                    PydevPlugin.log(e);
                    return;
                }

                super.invokeAction(action);

            } else {
                //we're not in the editable range (so, as the command was already checked to be valid, 
                //let's just let it keep its way)
                super.invokeAction(action);
            }
        }

        

        
        /**
         * When cutting something, we must be sure that it'll only mess with the contents
         * in the command line.
         */
        @Override
        public void cut() {
            changeSelectionToEditableRange();
            super.cut();
        }
        
        /**
         * When pasting something, we must be sure that it'll only mess with the contents
         * in the command line.
         */
        @Override
        public void paste() {
            changeSelectionToEditableRange();
            super.paste();
        }
        
        /**
         * When copying something, we don't want to copy the prompt contents.
         */
        @Override
        public void copy(int clipboardType) {
            checkWidget();
            
            Point selectedRange = getSelectedRange();
            if (selectedRange.y > 0) {
                IDocument doc = getDocument();
                
                new ClipboardHandler().putIntoClipboard(doc, selectedRange, clipboardType, getDisplay());
            }
        }
        

        /**
         * Changes the selected range to be all editable. 
         */
        protected void changeSelectionToEditableRange() {
            Point range = getSelectedRange();
            int commandLineOffset = getCommandLineOffset();
            
            int minOffset = range.x;
            int maxOffset = range.x+range.y;
            boolean changed = false;
            
            if(minOffset < commandLineOffset){
                minOffset = commandLineOffset;
                changed = true;
            }
            
            if(maxOffset < commandLineOffset){
                maxOffset = commandLineOffset;
                changed = true;
            }
            
            if(changed){
                setSelectedRange(minOffset, maxOffset-minOffset);
            }
            
            if (getCaretOffset() <= getCommandLineOffset()) {
                setCaretOffset(getDocument().getLength());
            }
        }
    }

    /**
     * @return the style provider that should be used.
     */
    public IConsoleStyleProvider getStyleProvider() {
        return styleProvider;
    }

    /**
     * @return the caret offset (based on the document)
     */
    public int getCaretOffset() {
        return getTextWidget().getCaretOffset();
    }

    /**
     * Sets the new caret position in the console.
     */
    public void setCaretOffset(final int offset) {
        final StyledText textWidget = getTextWidget();
        if(textWidget != null){
            Display display = textWidget.getDisplay();
            if(display != null){
                display.asyncExec(new Runnable() {
                    public void run() {
                        textWidget.setCaretOffset(offset);
                    }
                });
            }
        }
    }

    /**
     * @return true if the currently selected range is editable (all chars must be editable)
     */
    protected boolean isSelectedRangeEditable(){
        Point range = getSelectedRange();
        int commandLineOffset = getCommandLineOffset();
        
        if(range.x < commandLineOffset){
            return false;
        }
        
        if((range.x+range.y) < commandLineOffset){
            return false;
        }
        
        return true;
    }
    
    /**
     * @return true if the caret is currently in a position that can be edited.
     * @throws BadLocationException 
     */
    protected boolean isCaretInLastLine() throws BadLocationException{
        return getTextWidget().getCaretOffset() >= listener.getLastLineOffset();
    }

    /**
     * @return true if the caret is currently in a position that can be edited.
     */
    protected boolean isCaretInEditableRange(){
        return getTextWidget().getCaretOffset() >= getCommandLineOffset();
    }
    
    /**
     * Creates the styled text for the console
     */
    @Override
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
            final IScriptConsoleContentHandler contentHandler, IConsoleStyleProvider styleProvider) {
        super(parent, console);

        this.console = console;
        this.getTextWidget().setBackground(console.getBackground());

        
        ScriptConsoleViewer existingViewer = this.console.getViewer();
        
        if(existingViewer == null){
            this.isMainViewer = true;
            this.console.setViewer(this);
            this.styleProvider = styleProvider;
            this.history = console.getHistory();
            this.listener = new ScriptConsoleDocumentListener(this, console, console.getPrompt(), console.getHistory());
            this.listener.setDocument(getDocument());
        }else{
            this.isMainViewer = false;
            this.styleProvider = existingViewer.styleProvider;
            this.history = existingViewer.history;
            this.listener = existingViewer.listener;
            this.listener.addViewer(this);
        }
        

        final StyledText styledText = getTextWidget();

        styledText.addFocusListener(new FocusListener() {

        	/**
        	 * When the initial focus is gained, set the caret position to the last position (just after the prompt)
        	 */
            public void focusGained(FocusEvent e) {
                setCaretOffset(getDocument().getLength());
                //just a 1-time listener
                styledText.removeFocusListener(this);
            }

            public void focusLost(FocusEvent e) {

            }
        });

        styledText.addVerifyKeyListener(new KeyChecker());
        
        //content assist handling (we don't want to execute the event because the content assist handling
        //will be done here).
        
        //verify if it was a content assist
        styledText.addVerifyKeyListener(new VerifyKeyListener(){
            public void verifyKey(VerifyEvent event) {
                if (PyContentAssistant.matchesContentAssistKeybinding(event)) {
                    event.doit = false;
                    return;
                }
            }}
        );


        
        //execute the content assist
        styledText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
            	if (getCaretOffset() >= getCommandLineOffset()){
	                if (PyContentAssistant.matchesContentAssistKeybinding(e)) {
	                    contentHandler.contentAssistRequired();
	                }
            	}
            }

            public void keyReleased(KeyEvent e) {
            }
        });

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
    	
    	if(isMainViewer){
    	    clear();
    	}
    }
    
    
    /**
     * @return the contents of the current buffer (text edited still not passed to the shell)
     */
    public String getCommandLine() {
        try {
            return listener.getCommandLine();
        } catch (BadLocationException e) {
            return "";
        }
    }

    /**
     * @return the offset where the current buffer starts (editable area of the document)
     */
    public int getCommandLineOffset() {
        try {
            return listener.getCommandLineOffset();
        } catch (BadLocationException e) {
            return -1;
        }
    }

    /**
     * Used to clear the contents of the document
     */
    public void clear() {
        listener.clear();
    }

    
    /*
     * Overridden just to change visibility.
     * 
     * (non-Javadoc)
     * @see org.eclipse.ui.console.TextConsoleViewer#revealEndOfDocument()
     */
    @Override
    public void revealEndOfDocument() {
        super.revealEndOfDocument();
    }
}
