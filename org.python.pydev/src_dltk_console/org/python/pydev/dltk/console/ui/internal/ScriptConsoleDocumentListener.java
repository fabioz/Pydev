/**
 * 
 */
package org.python.pydev.dltk.console.ui.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextUtilities;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.dltk.console.InterpreterResponse;
import org.python.pydev.dltk.console.ScriptConsoleHistory;
import org.python.pydev.dltk.console.ScriptConsolePrompt;
import org.python.pydev.dltk.console.ui.IConsoleStyleProvider;
import org.python.pydev.dltk.console.ui.ScriptConsolePartitioner;
import org.python.pydev.dltk.console.ui.ScriptStyleRange;
import org.python.pydev.editor.autoedit.DocCmd;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This class will listen to the document and will:
 * 
 * - pass the commands to the handler
 * - add the results from the handler
 * - show the prompt
 * - set the color of the console regions
 */
public class ScriptConsoleDocumentListener implements IDocumentListener {

    private ICommandHandler handler;

    private ScriptConsolePrompt prompt;

    private ScriptConsoleHistory history;

    private int offset;

    private IDocument doc;
    
    private int disconnectionLevel = 0;  
    
    /**
     * Viewer for the documment contained in this listener.
     */
    private IScriptConsoleViewer2ForDocumentListener viewer;

    /**
     * Empty document (should not be written to).
     */
    IDocument EMPTY_DOCUMENT = new Document();


    /**
     * Strategy used for indenting / tabs
     */
    private PyAutoIndentStrategy strategy = new PyAutoIndentStrategy();
    
    public PyAutoIndentStrategy getIndentStrategy(){
        return strategy;
    }


    /**
     * Stops listening changes in one document and starts listening another one.
     * 
     * @param oldDoc may be null (if not null, this class will stop listening changes in it).
     * @param newDoc the document that should be listened from now on.
     */
    protected synchronized void reconnect(IDocument oldDoc, IDocument newDoc) {
        Assert.isTrue(disconnectionLevel == 0);
        
        if(oldDoc != null){
            oldDoc.removeDocumentListener(this);
        }
        
        newDoc.addDocumentListener(this);
        this.doc = newDoc;
        
    }
    
    /**
     * Stop listening to changes (so that we're able to change the document in this class without having
     * any loops back into the function that will change it)
     */
    protected synchronized void startDisconnected() {
        if(disconnectionLevel == 0){
            doc.removeDocumentListener(this);
        }
        disconnectionLevel += 1;
    }
    
    /**
     * Start listening to changes again.
     */
    protected synchronized void stopDisconnected() {
        disconnectionLevel -= 1;
        
        if(disconnectionLevel == 0){
            doc.addDocumentListener(this);
        }
    }


    /**
     * Clear the document and show the initial prompt.
     */
    public void clear() {
        try {
            startDisconnected();
            try{
                doc.set(""); //$NON-NLS-1$
                appendInvitation();
                viewer.setCaretOffset(doc.getLength());
            }finally{
                stopDisconnected();
            }
        } catch (BadLocationException e) {
            PydevPlugin.log(e);
        }
    }

    /**
     * Constructor
     */
    public ScriptConsoleDocumentListener(IScriptConsoleViewer2ForDocumentListener viewer, 
            ICommandHandler handler, ScriptConsolePrompt prompt,
            ScriptConsoleHistory history) {
        this.prompt = prompt;
        this.handler = handler;
        this.history = history;

        this.viewer = viewer;

        this.offset = 0;

        this.doc = null;
        
    }

    /**
     * Set the document that this class should listen.
     * 
     * @param doc the document that should be used in the console.
     */
    public void setDocument(IDocument doc) {
        reconnect(this.doc, doc);
    }

    /**
     * Ignore 
     */
    public void documentAboutToBeChanged(DocumentEvent event) {
        
    }

    /**
     * When the user presses a return and goes to a new line, this function should be called so that
     * the contents of the current line are sent to the interpreter (and its results properly handled).
     * 
     * @throws Exception if something happens while trying to push a command to the interpreter.
     */
    protected void handleCommandLine() throws Exception {
        final String command = getCommandLine();
        appendText(getDelimeter());
        processResult(handler.handleCommand(command));
    }

    
    /**
     * Process the result that came from pushing some text to the interpreter.
     * 
     * @param result the response from the interpreter after sending some command for it to process.
     * @throws BadLocationException
     */
    protected void processResult(final InterpreterResponse result) throws BadLocationException {
        if (result != null) {
            addToConsoleView(result.out, true);
            addToConsoleView(result.err, false);

            history.commit();
            offset = getLastLineLength();
        }
        appendInvitation();
    }

    /**
     * Adds some text that came as an output to stdout or stderr to the console.
     * 
     * @param out the text that should be added
     * @param stdout true if it came from stdout and falso if it came from stderr
     * @throws BadLocationException
     */
    private void addToConsoleView(String out, boolean stdout) throws BadLocationException {
        if(out.length() == 0){
            return; //nothing to add!
        }
        int start = doc.getLength();

        IConsoleStyleProvider styleProvider = viewer.getStyleProvider();
        if (styleProvider != null) {
            ScriptStyleRange style;
            if(stdout){
                style = styleProvider.createInterpreterOutputStyle(out, start);
            }else{ //stderr
                style = styleProvider.createInterpreterErrorStyle(out, start);
            }
            if (style != null) {
                addToPartitioner(style);
            }
        }
        appendText(out);
    }

    
    /**
     * Adds a given style range to the partitioner.
     * 
     * Note that the style must be added before the actual text is added! (because as 
     * soon as it's added, the style is asked for).
     *  
     * @param style the style to be added.
     */
    private void addToPartitioner(ScriptStyleRange style) {
        IDocumentPartitioner partitioner = viewer.getDocument().getDocumentPartitioner();
        if (partitioner instanceof ScriptConsolePartitioner) {
            ScriptConsolePartitioner scriptConsolePartitioner = (ScriptConsolePartitioner) partitioner;
            scriptConsolePartitioner.addRange(style);
        }
    }

    /**
     * Should be called right after adding some text to the console (it'll actually go on,
     * remove the text just added and add it line-by-line in the document so that it can be 
     * correctly treated in the console).
     * 
     * @param offset the offset where the addition took place
     * @param text the text that should be adedd
     * 
     * @throws Exception
     */
    protected void proccessAddition(int offset, String text) throws Exception {
        //we have to do some gymnastics here to add line-by-line the contents that the user entered.
        //(mostly because it may have been a copy/paste with multi-lines)
        
        String indentString = "";
        boolean addedNewLine = false;
        boolean addedParen = false;
        boolean addedCloseParen = false;
        int addedLen = text.length();
        if(addedLen == 1){
            if(text.equals("\r") || text.equals("\n")){
                addedNewLine = true;
                
            }else if(text.equals("(")){
                addedParen = true;
                
            } else if(text.equals(")")){
                addedCloseParen = true;
            }
            
        }else if(addedLen == 2){
            if(text.equals("\r\n")){
                addedNewLine = true;
            }
        }
        
        
        String delim = getDelimeter();
        
        int newDeltaCaretPosition = doc.getLength() - (offset + text.length());

        //1st, remove the text the user just entered (and enter it line-by-line later)
        text = doc.get(offset, doc.getLength() - offset);

        doc.replace(offset, text.length(), ""); //$NON-NLS-1$

        text = text.replaceAll("\r\n|\n|\r", delim); //$NON-NLS-1$

        //now, add it line-by-line (it won't even get into the loop if there's no 
        //new line in the text added).
        int start = 0;
        int index = -1;
        while ((index = text.indexOf(delim, start)) != -1) {
            String cmd = text.substring(start, index);

            cmd = convertTabs(cmd);
            applyStyleToUserAddedText(cmd, doc.getLength());
            appendText(cmd);

            String commandLine = getCommandLine();
            history.update(commandLine);
            start = index + delim.length();
            handleCommandLine();
            
            if(addedNewLine){
                IDocument historyDoc = this.history.getAsDoc();
                int currHistoryLen = historyDoc.getLength();
                if(currHistoryLen > 0){
                    DocCmd docCmd = new DocCmd(currHistoryLen-1, 0, delim);
                    strategy.customizeNewLine(historyDoc, docCmd);
                    indentString = docCmd.text.replaceAll("\\r\\n|\\n|\\r", ""); //remove any new line added!
                    if(currHistoryLen != historyDoc.getLength()){
                        PydevPlugin.log("Error: the document passed to the customizeNewLine should not be changed!");
                    }
                }
            }

        }
        
        boolean shiftsCaret = true;
        String newText = text.substring(start, text.length());
        if(addedParen){
            Document parenDoc = new Document(getCommandLine()+"(");
            int currentOffset = parenDoc.getLength();
            DocCmd docCmd = new DocCmd(currentOffset, 0, "(");
            docCmd.shiftsCaret = true;
            strategy.customizeParenthesis(parenDoc, docCmd);
            newText = docCmd.text;
            if(!docCmd.shiftsCaret){
                shiftsCaret = false;
                viewer.setCaretOffset(offset + (docCmd.caretOffset-currentOffset));
            }
        }else if (addedCloseParen){
            Document parenDoc = new Document(getCommandLine()+")");
            int currentOffset = parenDoc.getLength()-1;
            DocCmd docCmd = new DocCmd(currentOffset, 0, ")");
            docCmd.shiftsCaret = true;
            boolean canSkipOpenParenthesis = strategy.canSkipOpenParenthesis(parenDoc, docCmd);
            if(canSkipOpenParenthesis){
                shiftsCaret = false;
                viewer.setCaretOffset(offset + 1);
                newText = newText.substring(1);
            }
        }

        //and now add the last line (without actually handling it).
        String cmd = indentString+newText;
        cmd = convertTabs(cmd);
        applyStyleToUserAddedText(cmd, doc.getLength());
        appendText(cmd);
        if(shiftsCaret){
            viewer.setCaretOffset(doc.getLength()-newDeltaCaretPosition);
        }


        history.update(getCommandLine());
    }

    private String convertTabs(String cmd) {
        DocCmd newStr = new DocCmd(0, 0, cmd);
        strategy.getIndentPrefs().convertToStd(EMPTY_DOCUMENT, newStr);
        cmd = newStr.text;
        return cmd;
    }

    /**
     * Applies the style in the text for the contents that've been just added. 
     * 
     * @param cmd
     * @param offset2
     */
    private void applyStyleToUserAddedText(String cmd, int offset2) {
        IConsoleStyleProvider styleProvider = viewer.getStyleProvider();
        if (styleProvider != null) {
            ScriptStyleRange style = styleProvider.createUserInputStyle(cmd, offset2);
            if (style != null) {
                addToPartitioner(style);
            }
        }
    }

    /**
     * Whenever the document changes, we stop listening to change the document from
     * within this listener (passing commands to the handler if needed, getting results, etc).
     */
    public void documentChanged(DocumentEvent event) {
        startDisconnected();
        try{
            int eventOffset = event.getOffset();
            String eventText = event.getText();
            try {
                proccessAddition(eventOffset, eventText);
            } catch (BadLocationException e) {
                System.out.println(StringUtils.format(
                        "Error: bad location: offset:%s text:%s", eventOffset, eventText));
                
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }finally{
            stopDisconnected();
        }
    }

    /**
     * Appends some text at the end of the document.
     * 
     * @param text the text to be added.
     * 
     * @throws BadLocationException
     */
    protected void appendText(String text) throws BadLocationException {
        int initialOffset = doc.getLength();
        doc.replace(initialOffset, 0, text);
    }

    /**
     * Shows the prompt for the user (e.g.: >>>)
     * 
     * @throws BadLocationException
     */
    protected void appendInvitation() throws BadLocationException {
        int start = doc.getLength();
        String promptStr = prompt.toString();
        viewer.setCaretOffset(doc.getLength());
        viewer.revealEndOfDocument();
        IConsoleStyleProvider styleProvider = viewer.getStyleProvider();
        if (styleProvider != null) {
            ScriptStyleRange style = styleProvider.createPromptStyle(promptStr, start);
            if (style != null) {
                addToPartitioner(style);
            }
        }
        appendText(promptStr); //caret already updated
    }

    /**
     * @return the delimiter to be used to add new lines to the console.
     */
    public String getDelimeter() {
        return TextUtilities.getDefaultLineDelimiter(doc);
    }

    public int getLastLineLength() throws BadLocationException {
        int lastLine = doc.getNumberOfLines() - 1;
        return doc.getLineLength(lastLine);
    }
    
    public int getLastLineOffset() throws BadLocationException {
        int lastLine = doc.getNumberOfLines() - 1;
        return doc.getLineOffset(lastLine);
    }

    public int getLastLineReadOnlySize() {
        return offset + prompt.toString().length();
    }

    public int getCommandLineOffset() throws BadLocationException {
        int lastLine = doc.getNumberOfLines() - 1;
        return doc.getLineOffset(lastLine) + getLastLineReadOnlySize();
    }

    /**
     * @return the length of the current command line (all the currently
     * editable area)
     * 
     * @throws BadLocationException
     */
    public int getCommandLineLength() throws BadLocationException {
        int lastLine = doc.getNumberOfLines() - 1;
        return doc.getLineLength(lastLine) - getLastLineReadOnlySize();
    }

    
    /**
     * @return the command line that the user entered.
     * @throws BadLocationException
     */
    public String getCommandLine() throws BadLocationException {
        int commandLineOffset = getCommandLineOffset();
        int commandLineLength = getCommandLineLength();
        if(commandLineLength < 0){
            return "";
        }

        try {
            return doc.get(commandLineOffset, commandLineLength);
        } catch (BadLocationException e) {
            PydevPlugin.log(StringUtils.format(
                    "Error: bad location: offset:%s text:%s", commandLineOffset, commandLineLength));
            return "";
        }
    }

    /**
     * Sets the current command line to be executed (but without executing it).
     * Used by the up/down arrow to set a previous/next command.
     * 
     * @param command this is the command that should be in the command line.
     * 
     * @throws BadLocationException
     */
    public void setCommandLine(String command) throws BadLocationException {
        doc.replace(getCommandLineOffset(), getCommandLineLength(), command);
    }
}