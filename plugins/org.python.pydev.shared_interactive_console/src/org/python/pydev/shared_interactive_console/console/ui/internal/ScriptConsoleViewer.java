/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *

 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console.ui.internal;

import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.TextConsoleViewer;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_interactive_console.console.ScriptConsoleHistory;
import org.python.pydev.shared_interactive_console.console.codegen.IScriptConsoleCodeGenerator;
import org.python.pydev.shared_interactive_console.console.codegen.PythonSnippetUtils;
import org.python.pydev.shared_interactive_console.console.codegen.SafeScriptConsoleCodeGenerator;
import org.python.pydev.shared_interactive_console.console.ui.IConsoleStyleProvider;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleSession;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleViewer;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;
import org.python.pydev.shared_interactive_console.console.ui.internal.actions.AbstractHandleBackspaceAction;
import org.python.pydev.shared_interactive_console.console.ui.internal.actions.HandleDeletePreviousWord;
import org.python.pydev.shared_interactive_console.console.ui.internal.actions.HandleLineStartAction;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;

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
     * Should tab completion be enabled in this interpreter
     */
    private boolean tabCompletionEnabled;

    /**
     * This class is responsible for checking if commands should be issued or not given the command requested
     * and updating the caret to the correct position for it to happen (if needed).
     */
    private final class KeyChecker implements VerifyKeyListener {

        private Method fHideMethod;

        private Method getHideMethod() {
            if (fHideMethod == null) {
                try {
                    fHideMethod = ScriptConsoleViewer.this.fContentAssistant.getClass()
                            .getDeclaredMethod("hide");
                    fHideMethod.setAccessible(true);
                } catch (Exception e) {
                    Log.log(e);
                }
            }
            return fHideMethod;

        }

        public void verifyKey(VerifyEvent event) {
            try {
                if (event.character != '\0') { // Printable character

                    if (Character.isLetter(event.character)
                            && (event.stateMask == 0 || (event.stateMask & SWT.SHIFT) != 0)
                            || Character.isWhitespace(event.character)) {
                        //it's a valid letter without any stateMask (so, just entering regular text or upper/lowercase -- if shift is there).
                        if (!isSelectedRangeEditable()) {
                            getTextWidget().setCaretOffset(getDocument().getLength());
                        }
                    }

                    if (!isSelectedRangeEditable()) {
                        event.doit = false;
                        return;
                    }

                    if (event.character == SWT.CR || event.character == SWT.LF) {

                        //if we had an enter with the shift pressed and we're in a completion, we must stop it
                        if (inCompletion && (event.stateMask & SWT.SHIFT) != 0) {
                            //Work-around the fact that hide() is a protected method.
                            Method hideMethod = getHideMethod();
                            if (hideMethod != null) {
                                hideMethod.invoke(ScriptConsoleViewer.this.fContentAssistant);
                            }
                        }

                        if (!inCompletion) {
                            //in a new line, always set the caret to the end of the document (if not in completion)
                            //(note that when we make a hide in the previous 'if', it will automatically exit the
                            //completion mode (so, it'll also get into this part of the code)
                            getTextWidget().setCaretOffset(getDocument().getLength());
                        }
                        return;
                    }

                    if (event.character == SWT.ESC) {
                        if (!inCompletion) {
                            //while in a completion, esc won't clear the line (just stop the completion)
                            listener.setCommandLine("");
                        }
                        return;
                    }
                } else { //not printable char
                    if (isCaretInEditableRange()) {
                        if (!inCompletion && event.keyCode == SWT.PAGE_UP) {
                            event.doit = false;
                            List<String> commandsToExecute = ScriptConsoleHistorySelector.select(history);
                            if (commandsToExecute != null) {
                                //remove the current command (substituted by the one gotten from page up)
                                listener.setCommandLine("");
                                IDocument d = getDocument();
                                //Pass them all at once (let the document listener separate the command in lines).
                                d.replace(d.getLength(), 0, StringUtils.join("\n", commandsToExecute) + "\n");
                            }
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * Marks if a history request just started.
     */
    volatile int inHistoryRequests = 0;
    volatile boolean changedAfterLastHistoryRequest = false;

    private final boolean focusOnStart;
    /**
     * Handles a backspace (should guarantee that it does not delete things that are not in the last
     * line -- nor in the prompt)
     */
    private AbstractHandleBackspaceAction handleBackspaceAction;

    private boolean showInitialCommands;

    /**
     * This is the text widget that's used to edit the console. It has some treatments to handle
     * commands that should act differently (special handling for when the caret is on the last line
     * to execute custom commands, such using the history, etc).
     */
    private class ScriptConsoleStyledText extends StyledText {

        /**
         * Handles a delete previous word (should guarantee that it does not delete things that are not in the last
         * line -- nor in the prompt)
         */
        private HandleDeletePreviousWord handleDeletePreviousWord;

        /**
         * Handles a line start action (home) stays within the same line changing from the
         * 1st char of text, beginning of prompt, beginning of line.
         */
        private HandleLineStartAction handleLineStartAction;

        /**
         * Contains the caret offset that has been set from the console API.
         */
        private volatile int internalCaretSet = -1;

        /**
         * Set to true when drag source/target are the same console
         */
        private boolean thisConsoleInitiatedDrag = false;

        /**
         * Constructor.
         *
         * @param parent parent for the styled text
         * @param style style to be used
         */
        public ScriptConsoleStyledText(Composite parent, int style) {
            super(parent, style);

            /**
             * The StyledText will change the caretOffset that we've updated during the modifications,
             * so, the verify and the extended modify listener will keep track if it actually does
             * that and will reset the caret to the position we actually added it.
             *
             * Feels like a hack but I couldn't find a better way to do it.
             */
            addVerifyListener(new VerifyListener() {

                public void verifyText(VerifyEvent e) {
                    internalCaretSet = -1;
                }
            });

            /**
             * Set it to the location we've set it to be.
             */
            addExtendedModifyListener(new ExtendedModifyListener() {

                public void modifyText(ExtendedModifyEvent event) {
                    if (internalCaretSet != -1) {
                        if (internalCaretSet != getCaretOffset()) {
                            setCaretOffset(internalCaretSet);
                        }
                        internalCaretSet = -1;
                    }
                }
            });

            initDragDrop();

            handleDeletePreviousWord = new HandleDeletePreviousWord();
            handleLineStartAction = new HandleLineStartAction();
        }

        private void initDragDrop() {
            DragSource dragSource = new DragSource(this, DND.DROP_COPY | DND.DROP_MOVE);
            dragSource.addDragListener(new DragSourceAdapter());
            dragSource.setTransfer(new Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });

            DropTarget dropTarget = new DropTarget(this, DND.DROP_COPY | DND.DROP_MOVE);
            dropTarget.setTransfer(new Transfer[] { LocalSelectionTransfer.getTransfer(),
                    org.eclipse.swt.dnd.TextTransfer.getInstance() });
            dropTarget.addDropListener(new DragTargetAdapter());
        }

        private final class DragSourceAdapter implements DragSourceListener {
            private Point selection;
            private String selectionText = null;
            private boolean selectionIsEditable;

            public void dragStart(DragSourceEvent event) {
                thisConsoleInitiatedDrag = false;
                selectionText = null;
                event.doit = false;
                if (getSelectedRange().y > 0) {
                    String temp_selection = new ClipboardHandler().getPlainText(getDocument(), getSelectedRange());
                    if (temp_selection != null && temp_selection.length() > 0) {
                        event.doit = true;
                        selectionText = temp_selection;
                        selection = getSelection();
                        selectionIsEditable = isSelectedRangeEditable();
                    }
                }
            }

            public void dragSetData(DragSourceEvent event) {
                if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                    event.data = selectionText;
                    thisConsoleInitiatedDrag = true;
                }
            }

            public void dragFinished(DragSourceEvent event) {
                try {
                    if (event.detail == DND.DROP_MOVE && selectionIsEditable) {
                        Point newSelection = getSelection();
                        int length = selection.y - selection.x;
                        int delta = 0;
                        if (newSelection.x < selection.x) {
                            delta = length;
                        }
                        replaceTextRange(selection.x + delta, length, "");
                    }
                } finally {
                    thisConsoleInitiatedDrag = false;
                }
            }
        }

        private final class DragTargetAdapter implements DropTargetListener {

            private SafeScriptConsoleCodeGenerator getSafeGenerator() {
                ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
                IScriptConsoleCodeGenerator codeGenerator = PythonSnippetUtils
                        .getScriptConsoleCodeGeneratorAdapter(selection);
                return new SafeScriptConsoleCodeGenerator(codeGenerator);
            }

            /**
             * We cancel the drop if we don't have anything to drop
             */
            private boolean forceDropNone(DropTargetEvent event) {
                if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
                    IScriptConsoleCodeGenerator codeGenerator = getSafeGenerator();
                    if (codeGenerator == null || codeGenerator.hasPyCode() == false) {
                        return true;
                    }
                }
                return false;
            }

            private void adjustEventDetail(DropTargetEvent event) {
                if (forceDropNone(event)) {
                    event.detail = DND.DROP_NONE;
                } else if (!thisConsoleInitiatedDrag && (event.operations & DND.DROP_COPY) != 0) {
                    event.detail = DND.DROP_COPY;
                } else if ((event.operations & DND.DROP_MOVE) != 0) {
                    event.detail = DND.DROP_MOVE;
                } else if ((event.operations & DND.DROP_COPY) != 0) {
                    event.detail = DND.DROP_COPY;
                } else {
                    event.detail = DND.DROP_NONE;
                }
            }

            public void dragEnter(DropTargetEvent event) {
                thisConsoleInitiatedDrag = false;
                adjustEventDetail(event);
            }

            public void dragOver(DropTargetEvent event) {
                event.feedback |= DND.FEEDBACK_SCROLL;
            }

            public void dragOperationChanged(DropTargetEvent event) {
                adjustEventDetail(event);
            }

            public void dropAccept(DropTargetEvent event) {
                adjustEventDetail(event);
            }

            public void drop(DropTargetEvent event) {
                if (event.operations == DND.DROP_NONE) {
                    // nothing to do
                    return;
                }

                String text = null;
                if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
                    text = (String) event.data;

                } else if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
                    IScriptConsoleCodeGenerator codeGenerator = getSafeGenerator();
                    if (codeGenerator != null) {
                        text = codeGenerator.getPyCode();
                    }
                }

                if (text != null && text.length() > 0) {
                    Point selectedRange = getSelectedRange();
                    if (selectedRange.x < getLastLineOffset()) {
                        changeSelectionToEditableRange();
                    } else {
                        int commandLineOffset = getCommandLineOffset();
                        if (selectedRange.x < commandLineOffset) {
                            setSelectedRange(commandLineOffset, 0);
                        }
                    }
                    // else, is in range

                    Point newSelection = getSelection();
                    try {
                        getDocument().replace(newSelection.x, 0, text);
                    } catch (BadLocationException e) {
                        return;
                    }
                    setSelectionRange(newSelection.x, text.length());
                    changeSelectionToEditableRange();
                }

            }

            public void dragLeave(DropTargetEvent event) {
            }
        }

        /**
         * Overridden to keep track of changes in the caret.
         */
        @Override
        public void setCaretOffset(int offset) {
            internalCaretSet = offset;
            super.setCaretOffset(offset);
        }

        /**
         * Execute some action.
         */
        @Override
        public void invokeAction(int action) {
            //some actions have a different scope (not in selected range / out of selected range)
            switch (action) {
                case ST.LINE_START:
                    if (handleLineStartAction.execute(getDocument(), getCaretOffset(), getCommandLineOffset(),
                            ScriptConsoleViewer.this)) {
                        return;
                    } else {
                        super.invokeAction(action);
                    }

            }

            if (isSelectedRangeEditable()) {
                try {
                    int historyChange = 0;
                    switch (action) {
                        case ST.LINE_UP:
                            historyChange = 1;
                            break;

                        case ST.LINE_DOWN:
                            historyChange = 2;
                            break;

                        case ST.DELETE_PREVIOUS:
                            handleBackspaceAction.execute(getDocument(),
                                    (ITextSelection) ScriptConsoleViewer.this.getSelection(), getCommandLineOffset());
                            return;

                        case ST.DELETE_WORD_PREVIOUS:
                            handleDeletePreviousWord.execute(getDocument(), getCaretOffset(), getCommandLineOffset());
                            return;
                    }

                    if (historyChange != 0) {
                        if (changedAfterLastHistoryRequest) {
                            //only set a new match if it didn't change since the last time we did an UP/DOWN
                            history.setMatchStart(getCommandLine());
                        }
                        boolean didChange;
                        if (historyChange == 1) {
                            didChange = history.prev();
                        } else {
                            didChange = history.next();
                        }

                        if (didChange) {
                            inHistoryRequests += 1;
                            try {
                                listener.setCommandLine(history.get());
                                setCaretOffset(getDocument().getLength());
                            } finally {
                                inHistoryRequests -= 1;
                            }
                        }
                        changedAfterLastHistoryRequest = false;
                        return;
                    }

                } catch (BadLocationException e) {
                    Log.log(e);
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
        public void copy() {
            copy(DND.CLIPBOARD);
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
            int maxOffset = range.x + range.y;
            boolean changed = false;
            boolean goToEnd = false;

            if (minOffset < commandLineOffset) {
                minOffset = commandLineOffset;
                changed = true;
            }

            if (maxOffset < commandLineOffset) {
                maxOffset = commandLineOffset;
                changed = true;
                // Only go to the end of the buffer if  the max offset isn't in range
                goToEnd = true;
            }

            if (changed) {
                setSelectedRange(minOffset, maxOffset - minOffset);
            }

            if (goToEnd) {
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
     * @return the number of characters visible on a line
     */
    public int getConsoleWidthInCharacters() {
        return getTextWidget().getSize().x / getWidthInPixels("a");
    }

    /**
     * @return the caret offset (based on the document)
     */
    public int getCaretOffset() {
        return getTextWidget().getCaretOffset();
    }

    public Object getInterpreterInfo() {
        return this.console.getInterpreterInfo();
    }

    /**
     * Sets the new caret position in the console.
     *
     * TODO: async should not be allowed (only clearing the shell at the constructor still uses that)
     */
    public void setCaretOffset(final int offset, boolean async) {
        final StyledText textWidget = getTextWidget();
        if (textWidget != null) {
            if (async) {
                Display display = textWidget.getDisplay();
                if (display != null) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            textWidget.setCaretOffset(offset);
                        }
                    });
                }
            } else {
                textWidget.setCaretOffset(offset);
            }
        }
    }

    /**
     * @return true if the currently selected range is editable (all chars must be editable)
     */
    protected boolean isSelectedRangeEditable() {
        Point range = getSelectedRange();
        int commandLineOffset = getCommandLineOffset();

        if (range.x < commandLineOffset) {
            return false;
        }

        if ((range.x + range.y) < commandLineOffset) {
            return false;
        }

        return true;
    }

    /**
     * @return true if the caret is currently in a position that can be edited.
     * @throws BadLocationException
     */
    protected boolean isCaretInLastLine() throws BadLocationException {
        return getTextWidget().getCaretOffset() >= listener.getLastLineOffset();
    }

    /**
     * @return true if the caret is currently in a position that can be edited.
     */
    protected boolean isCaretInEditableRange() {
        return getTextWidget().getCaretOffset() >= getCommandLineOffset();
    }

    /**
     * Creates the styled text for the console
     */
    @Override
    protected StyledText createTextWidget(Composite parent, int styles) {
        return new ScriptConsoleStyledText(parent, styles);
    }

    @Override
    public IScriptConsoleSession getConsoleSession() {
        return this.console.getSession();
    }

    /**
     * Constructor
     *
     * @param parent parent for this viewer
     * @param console the console that this viewer is showing
     * @param contentHandler
     */
    public ScriptConsoleViewer(Composite parent, ScriptConsole console,
            final IScriptConsoleContentHandler contentHandler, IConsoleStyleProvider styleProvider,
            String initialCommands, boolean focusOnStart, AbstractHandleBackspaceAction handleBackspaceAction,
            IHandleScriptAutoEditStrategy strategy, boolean tabCompletionEnabled, boolean showInitialCommands) {
        super(parent, console);
        this.showInitialCommands = showInitialCommands;
        this.handleBackspaceAction = handleBackspaceAction;
        this.focusOnStart = focusOnStart;
        this.tabCompletionEnabled = tabCompletionEnabled;

        this.console = console;
        this.getTextWidget().setBackground(console.getPydevConsoleBackground());

        ScriptConsoleViewer existingViewer = this.console.getViewer();

        if (existingViewer == null) {
            this.isMainViewer = true;
            this.console.setViewer(this);
            this.styleProvider = styleProvider;
            this.history = console.getHistory();

            this.listener = new ScriptConsoleDocumentListener(this, console, console.getPrompt(), console.getHistory(),
                    console.createLineTrackers(console), initialCommands, strategy);

            this.listener.setDocument(getDocument());
        } else {
            this.isMainViewer = false;
            this.styleProvider = existingViewer.styleProvider;
            this.history = existingViewer.history;
            this.listener = existingViewer.listener;
            this.listener.addViewer(this);
        }

        final StyledText styledText = getTextWidget();

        //Added because we don't want the console to close when the user presses ESC
        //(as it would when it's on a floating window)
        //we do that because ESC is meant to clear the current line (and as such,
        //should do that action and not close the console).
        styledText.addTraverseListener(new TraverseListener() {

            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    e.doit = false;
                }
            }
        });

        getDocument().addDocumentListener(new IDocumentListener() {

            public void documentAboutToBeChanged(DocumentEvent event) {
            }

            public void documentChanged(DocumentEvent event) {
                if (inHistoryRequests == 0) {
                    changedAfterLastHistoryRequest = true;
                }
            }
        });

        styledText.addFocusListener(new FocusListener() {

            /**
             * When the initial focus is gained, set the caret position to the last position (just after the prompt)
             */
            public void focusGained(FocusEvent e) {
                setCaretOffset(getDocument().getLength(), true);
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
        styledText.addVerifyKeyListener(new VerifyKeyListener() {
            public void verifyKey(VerifyEvent event) {
                if (KeyBindingHelper.matchesContentAssistKeybinding(event)
                        || KeyBindingHelper.matchesQuickAssistKeybinding(event)) {
                    event.doit = false;
                    return;
                }
            }
        });

        // IPython tab completion
        styledText.addVerifyKeyListener(new VerifyKeyListener() {
            public void verifyKey(VerifyEvent event) {
                if (!ScriptConsoleViewer.this.tabCompletionEnabled ||
                        inCompletion // if we're already doing a code-completion with Ctrl+Space, we shouldn't do the tab completion.
                ) {
                    return;
                }
                // Don't auto-complete if the tab is the first character on the line
                if (event.character == SWT.TAB && !listener.getCommandLine().trim().isEmpty()) {
                    // Show IPython completions when the user tabs in the console
                    listener.handleConsoleTabCompletions();
                    // And eat the tab
                    event.doit = false;
                }
            }
        });

        //execute the content assist
        styledText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (getCaretOffset() >= getCommandLineOffset()) {
                    if (KeyBindingHelper.matchesContentAssistKeybinding(e)) {
                        contentHandler.contentAssistRequired();

                    } else if (KeyBindingHelper.matchesQuickAssistKeybinding(e)) {
                        contentHandler.quickAssistRequired();
                    }
                }
            }

            public void keyReleased(KeyEvent e) {
            }
        });

    }

    public ScriptConsole getConsole() {
        return console;
    }

    /**
     * Listen to the completions because we've to know when we're doing a completion or not.
     */
    @Override
    public void configure(SourceViewerConfiguration configuration) {
        super.configure(configuration);
        ICompletionListener completionListener = new ICompletionListener() {

            public void assistSessionStarted(ContentAssistEvent event) {
                inCompletion = true;
            }

            public void assistSessionEnded(ContentAssistEvent event) {
                inCompletion = false;
            }

            public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
            }
        };

        if (fContentAssistant != null) {
            ((IContentAssistantExtension2) fContentAssistant).addCompletionListener(completionListener);
        }

        if (fQuickAssistAssistant != null) {
            fQuickAssistAssistant.addCompletionListener(completionListener);
        }

        if (isMainViewer) {
            clear(showInitialCommands);
        }
        if (focusOnStart) {
            this.getTextWidget().setFocus();
        }
    }

    public IContentAssistant getContentAssist() {
        return fContentAssistant;
    }

    public IQuickAssistAssistant getQuickFixContentAssist() {
        return fQuickAssistAssistant;
    }

    /**
     * @return the contents of the current buffer (text edited still not passed to the shell)
     */
    public String getCommandLine() {
        return listener.getCommandLine();
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
     * @return the offset where the line containing the current buffer starts (editable area of the document)
     */
    public int getLastLineOffset() {
        try {
            return listener.getLastLineOffset();
        } catch (BadLocationException e) {
            return -1;
        }
    }

    /**
     * Used to clear the contents of the document
     */
    public void clear(boolean addInitialCommands) {
        listener.clear(addInitialCommands);
    }

    /**
     * @return the last time the document shown in this viewer was edited.
     */
    public long getLastChangeMillis() {
        return listener.getLastChangeMillis();
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

    public void discardCommandLine() {
        listener.discardCommandLine();
    }
}
