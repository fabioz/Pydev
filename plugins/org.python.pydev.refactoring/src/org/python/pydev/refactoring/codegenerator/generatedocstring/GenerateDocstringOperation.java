/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

/* 
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

/*
 * @author Robin Stocker, Fabio Zadrozny
 */
package org.python.pydev.refactoring.codegenerator.generatedocstring;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.correctionassist.docstrings.DocstringsPrefPage;

// FIXME: Merge with code in AssistDocString.

/**
 * Generates a docstring for a function or a class. The caret can be anywhere
 * within the body (of the function or class) or on the signature. If there's
 * already a docstring, the caret is placed at the end of the docstring.
 */
public class GenerateDocstringOperation implements IWorkspaceRunnable {

    private ITextEditor editor;

    public GenerateDocstringOperation(ITextEditor editor) {
        this.editor = editor;
    }

    public void run(IProgressMonitor monitor) throws CoreException {
        PySelection selection = new PySelection(editor);

        boolean definitionFound = selectDefinition(selection);
        if(!definitionFound){
            return;
        }

        boolean docstringFound = selectEndOfDocstring(selection);
        if(docstringFound){
            editor.selectAndReveal(selection.getAbsoluteCursorOffset(), 0);
            return;
        }

        Tuple<List<String>, Integer> tuple = selection.getInsideParentesisToks(false);
        if(tuple == null){
            // We didn't find parenthesis, which means the selection isn't on a
            // definition, so be done now.
            return;
        }
        List<String> params = tuple.o1;

        // Needed because the parameter list can be on multiple lines.
        int lineOfLastParam = selection.getLineOfOffset(tuple.o2);

        String inAndIndent = getInAndIndent(selection);

        StringBuilder s = new StringBuilder();
        String docStringMarker = DocstringsPrefPage.getDocstringMarker();

        s.append(inAndIndent + docStringMarker);
        s.append(inAndIndent);

        int relativeSelectionOffset = s.length();

        if(selection.isInFunctionLine()){
            for(String paramName:params){
                if(!PySelection.isIdentifier(paramName)){
                    continue;
                }
                s.append(inAndIndent + "@param " + paramName + ":");
                if(DocstringsPrefPage.getTypeTagShouldBeGenerated(paramName)){
                    s.append(inAndIndent + "@type " + paramName + ":");
                }
            }
        }

        s.append(inAndIndent + docStringMarker);

        try{
            int insertOffset = selection.getEndLineOffset(lineOfLastParam);
            IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            document.replace(insertOffset, 0, s.toString());
            editor.selectAndReveal(insertOffset + relativeSelectionOffset, 0);
        }catch(BadLocationException e){
            return;
        }
    }

    /**
     * Jump to the line where the "def" or "class" is.
     * 
     * @param selection
     * @return whether the definition was found
     */
    private boolean selectDefinition(PySelection selection) {
        if(isInDefinitionLine(selection)){
            return true;
        }

        // Find the next non-blank line to find our current scope indentation.
        boolean nextNonBlankLineFound = selectNextNonBlankLine(selection);
        if(!nextNonBlankLineFound){
            return false;
        }
        String scopeIndentation = selection.getIndentationFromLine();

        // Find the line with the definition, where the indentation is less than
        // the scope indentation.
        int line = selection.getCursorLine();
        while(true){
            String indentation = selection.getIndentationFromLine();
            boolean lessIndentation = indentation.length() < scopeIndentation.length();
            if(lessIndentation && isInDefinitionLine(selection)){
                return true;
            }

            // There was less indentation, so the selection was originally in
            // e.g. an if block.
            boolean blankLine = selection.getCursorLineContents().trim().equals("");
            if(lessIndentation && !blankLine){
                scopeIndentation = indentation;
            }

            line -= 1;
            if(line == -1){
                // Arrived at the top, return now.
                return false;
            }
            int offset = selection.getLineOffset(line);
            selection.setSelection(offset, offset);
        }
    }

    /**
     * @param selection
     * @return whether we're in a definition line ("class" or "def")
     */
    private boolean isInDefinitionLine(PySelection selection) {
        if(selection.getCursorLineContents().trim().equals("")){
            return false;
        }
        return selection.isInFunctionLine() || selection.isInClassLine();
    }

    /**
     * Find the next non-blank line.
     * @param selection to change
     * @return whether there was a next non-blank line
     */
    private boolean selectNextNonBlankLine(PySelection selection) {
        int line = selection.getCursorLine();
        while(selection.getLine(line).trim().equals("")){
            // Note: We use the line as a counter instead of the selection
            // directly, because there is an infinite loop otherwise (probably
            // because of some corner case).
            line += 1;
            if(selection.getLineOffset(line) == 0){
                // Arrived at the bottom (0 means not found), return now.
                return false;
            }
        }

        int offset = selection.getLineOffset(line);
        selection.setSelection(offset, offset);
        return true;
    }

    /**
     * Jump to the end of the docstring if there is one.
     * 
     * @param selection
     * @return whether there was a docstring
     */
    private boolean selectEndOfDocstring(PySelection selection) {
        int charsToColon = selection.getToColon().length();
        int offset = selection.getLineOffset();
        offset += charsToColon;

        String re = "\\G" + // Start at offset with find(offset)
                "\\s*(u|r)?" + // Also match Unicode or raw docstrings
                "(\"{3}|'{3}|\"|')" + // Start of docstring, e.g. """ (this is \2)
                "(.*?" + // Content
                "[^\\\\]?)(\\2)"; // End of docstring
        Pattern p = Pattern.compile(re, Pattern.DOTALL);

        String document = selection.getDoc().get();
        Matcher m = p.matcher(document);
        boolean found = m.find(offset);
        if(!found){
            return false;
        }

        int contentStart = m.start(3);
        String docstringContents = m.group(3);

        // For intelligent selection placement. Go to the end of the last line
        // of the docstring.
        p = Pattern.compile("\n?(\\t| )*\\z");
        m = p.matcher(docstringContents);
        m.find();
        int offsetToEnd = m.start();
        selection.setSelection(contentStart + offsetToEnd, contentStart + offsetToEnd);
        return true;
    }

    private String getInAndIndent(PySelection selection) {
        String initial = PySelection.getIndentationFromLine(selection.getCursorLineContents());
        String delimiter = PyAction.getDelimiter(selection.getDoc());
        String indentation;
        // This check is made so we can use a normal ITextEditor, which makes it
        // possible to mock the editor in tests.
        if(editor instanceof PyEdit){
            indentation = PyAction.getStaticIndentationString((PyEdit) editor);
        }else{
            indentation = "    ";
        }
        String inAndIndent = delimiter + initial + indentation;
        return inAndIndent;
    }
}
