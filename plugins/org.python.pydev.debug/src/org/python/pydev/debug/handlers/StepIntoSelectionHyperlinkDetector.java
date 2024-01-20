package org.python.pydev.debug.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.SmartStepIntoVariant;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.visitors.PythonLanguageUtils;
import org.python.pydev.shared_core.string.ICoreTextSelection;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;

/**
 * This is a specialization of a hyperlink detector for the step into selection command
 *
 * @since 3.3
 */
public class StepIntoSelectionHyperlinkDetector extends AbstractHyperlinkDetector {

    /**
     * Specific implementation of a hyperlink for step into command
     */
    class StepIntoSelectionHyperlink implements IHyperlink {

        private final IRegion fSelection;
        private final PyEdit pyEdit;
        private final String selectedWord;
        private final PyStackFrame debugContext;
        private final int line;
        private final SmartStepIntoVariant target;

        /**
         * Constructor
         * @param debugContext
         * @param pyEdit
         * @param selectedWord
         * @param region
         */
        public StepIntoSelectionHyperlink(PyStackFrame debugContext, PyEdit pyEdit, IRegion selection, int line,
                String selectedWord, SmartStepIntoVariant target) {
            this.debugContext = debugContext;
            this.pyEdit = pyEdit;
            this.line = line;
            this.selectedWord = selectedWord;
            this.fSelection = selection;
            this.target = target;
        }

        /**
         * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
         */
        @Override
        public IRegion getHyperlinkRegion() {
            return new Region(fSelection.getOffset(), fSelection.getLength());
        }

        /**
         * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
         */
        @Override
        public String getHyperlinkText() {
            return "Step Into Selection: " + this.selectedWord;
        }

        /**
         * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
         */
        @Override
        public String getTypeLabel() {
            return null;
        }

        /**
         * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
         */
        @Override
        public void open() {
            try {
                debugContext.stepIntoTarget(pyEdit, line, selectedWord, target);
            } catch (DebugException e) {
                Log.log(e);
            }
        }

    }

    /**
     * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
     */
    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        ITextEditor editor = getAdapter(ITextEditor.class);
        if (editor instanceof PyEdit) {
            PyEdit pyEdit = (PyEdit) editor;
            // should only enable step into selection when the current debug context
            // is an instance of IJavaStackFrame
            IAdaptable debugContext = DebugUITools.getDebugContext();
            if (debugContext == null) {
                return null;
            }
            if (!(debugContext instanceof PyStackFrame)) {
                return null;
            }
            PyStackFrame pyStackFrame = (PyStackFrame) debugContext;
            SmartStepIntoVariant[] stepIntoTargets = pyStackFrame.getStepIntoTargets();
            if (stepIntoTargets == null) {
                Log.log("Unable to get Smart Step Into Targets.");
                return null;
            }
            if (stepIntoTargets.length == 0) {
                Log.log("No Smart Step Into Targets found.");
                return null;
            }

            ICoreTextSelection textSelection = pyEdit.getTextSelection();
            final int line = textSelection.getStartLine();
            final int offset = region.getOffset();

            boolean smartStepIntoHasOffset = false;
            List<SmartStepIntoVariant> foundAtLine = new ArrayList<>();
            for (SmartStepIntoVariant smartStepIntoVariant : stepIntoTargets) {
                if (smartStepIntoVariant.endlineno >= 0 && smartStepIntoVariant.startcol >= 0
                        && smartStepIntoVariant.endcol >= 0) {
                    // Ok, new approach: use the offsets given by the backend when available (Python 3.11 onwards).
                    smartStepIntoHasOffset = true;
                    if (smartStepIntoVariant.line >= line) {
                        foundAtLine.add(smartStepIntoVariant);
                    }
                }
            }

            List<SmartStepIntoVariant> found = new ArrayList<>();
            IDocument document = pyEdit.getDocument();

            if (smartStepIntoHasOffset) {
                TextSelectionUtils ts = new TextSelectionUtils(document, offset);

                List<StepIntoSelectionHyperlink> hyperlinks = new ArrayList<>();
                for (SmartStepIntoVariant smartStepIntoVariant : stepIntoTargets) {
                    try {
                        int startOffset = ts.getAbsoluteCursorOffset(smartStepIntoVariant.line,
                                smartStepIntoVariant.startcol);
                        int endOffset = ts.getAbsoluteCursorOffset(smartStepIntoVariant.endlineno,
                                smartStepIntoVariant.endcol);
                        if (startOffset <= offset && endOffset >= offset) {
                            IRegion wordRegion = new Region(startOffset, endOffset - startOffset);
                            final StepIntoSelectionHyperlink link = new StepIntoSelectionHyperlink(
                                    (PyStackFrame) debugContext, pyEdit,
                                    wordRegion, line,
                                    smartStepIntoVariant.name, smartStepIntoVariant);
                            if (hyperlinks.size() == 0 || canShowMultipleHyperlinks) {
                                hyperlinks.add(link);
                            } else {
                                // Just add it if its size is smaller than the existing one.
                                StepIntoSelectionHyperlink curr = hyperlinks.get(0);
                                if (curr.getHyperlinkRegion().getLength() > link.getHyperlinkRegion().getLength()) {
                                    hyperlinks.set(0, link);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
                if (hyperlinks.size() == 0) {
                    // We can't return empty as it gives an IllegalArgumentException.
                    return null;
                }
                return hyperlinks.toArray(new StepIntoSelectionHyperlink[0]);

            } else {
                try {
                    //see if we can find a word there
                    IRegion wordRegion = TextSelectionUtils.findWord(document, offset);
                    if (wordRegion == null || wordRegion.getLength() == 0) {
                        return null;
                    }

                    String selectedWord;
                    //don't highlight keywords
                    try {
                        selectedWord = document.get(wordRegion.getOffset(), wordRegion.getLength());
                        if (PythonLanguageUtils.isKeyword(selectedWord)) {
                            return null;
                        }
                    } catch (BadLocationException e) {
                        Log.log(e);
                        return null;
                    }

                    for (SmartStepIntoVariant smartStepIntoVariant : stepIntoTargets) {
                        if (line == smartStepIntoVariant.line && selectedWord.equals(smartStepIntoVariant.name)) {
                            found.add(smartStepIntoVariant);
                        }
                    }
                    if (found.size() == 0) {
                        Log.log("Unable to find step into target with name: " + selectedWord + " at line: " + line
                                + "\nAvailable: "
                                + StringUtils.join("; ", stepIntoTargets));
                        return null;
                    }
                    SmartStepIntoVariant target;
                    if (found.size() > 1) {
                        Collections.reverse(found); // i.e.: the target is backwards.
                        Iterator<SmartStepIntoVariant> iterator = found.iterator();
                        target = iterator.next();

                        TextSelectionUtils ts = new TextSelectionUtils(document, textSelection);

                        // Let's check if there's more than one occurrence of the same word in the given line.
                        String lineContents = ts.getLine(line);
                        List<Integer> wordOffsets = StringUtils.findWordOffsets(lineContents, selectedWord);
                        if (wordOffsets.size() > 0) {
                            int offsetInLine = wordRegion.getOffset() - ts.getStartLineOffset();
                            for (Integer i : wordOffsets) {
                                if (i >= offsetInLine) {
                                    break;
                                }
                                target = iterator.next();
                                if (!iterator.hasNext()) {
                                    break;
                                }
                            }
                        }
                    } else {
                        // size == 1
                        target = found.get(0);
                    }

                    //return a hyperlink even without trying to find the definition (which may be costly)
                    return new IHyperlink[] {
                            new StepIntoSelectionHyperlink((PyStackFrame) debugContext, pyEdit, wordRegion, line,
                                    selectedWord, target) };
                } catch (Exception e) {
                    Log.log(e);
                    return null;
                }
            }

        }
        return null;
    }
}
