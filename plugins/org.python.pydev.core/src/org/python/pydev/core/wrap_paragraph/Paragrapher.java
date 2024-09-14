package org.python.pydev.core.wrap_paragraph;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.string.TextSelectionUtils;

public class Paragrapher {
    private PySelection selection;
    private IDocument document;

    private int offset;
    public int currentLineNo;

    private String currentLine;

    public String leadingString;
    public String mainText;

    public int offsetOfOriginalParagraph;
    public int lengthOfOriginalParagraph;

    private int numberOfLinesInDocument;
    public final String docDelimiter;
    private int noCols;
    private List<String> paragraph;

    private static Pattern pattern = Pattern
            .compile("(\\s*#\\s*|\\s*\"\"\"\\s*|\\s*'''\\s*|\\s*\"\\s*|\\s*'\\s*|\\s*)");

    public Paragrapher(PySelection selection, int noCols) {
        this.noCols = noCols;
        this.selection = selection;
        this.document = selection.getDoc();

        this.docDelimiter = TextSelectionUtils.getDelimiter(document);
        this.offset = selection.getAbsoluteCursorOffset();
        this.currentLineNo = selection.getLineOfOffset(offset);

        this.currentLine = selection.getLine(currentLineNo);

        Matcher matcher = pattern.matcher(currentLine);
        if (matcher.find()) {
            this.leadingString = currentLine.substring(0, matcher.end());
            this.mainText = currentLine.substring(matcher.end());
        }

        this.offsetOfOriginalParagraph = 0;
        this.lengthOfOriginalParagraph = 0;

        this.numberOfLinesInDocument = document.getNumberOfLines();
    }

    private String[] splitLine(String line) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String leadingString = line.substring(0, matcher.end());
            String mainText = line.substring(matcher.end());
            return new String[] { leadingString, mainText };
        }
        return new String[] { "", line }; // Fallback if no match
    }

    public String getCurrentLine() {
        this.currentLine = selection.getLine(currentLineNo);
        this.mainText = splitLine(currentLine)[1];
        return this.mainText;
    }

    public boolean previousLineIsInParagraph() {
        if (currentLineNo == 0) {
            return false;
        }

        String previousLine = selection.getLine(currentLineNo - 1);
        String[] previousLineParts = splitLine(previousLine);
        String leadingStringOfPreviousLine = previousLineParts[0];
        String mainTextOfPreviousLine = previousLineParts[1];

        if (mainTextOfPreviousLine.trim().isEmpty() || !leadingStringOfPreviousLine.equals(leadingString)) {
            String line = selection.getLine(currentLineNo);
            int lineEndsAt;
            try {
                lineEndsAt = selection.getEndLineOffset(currentLineNo);
            } catch (BadLocationException e) {
                return false;
            }
            offsetOfOriginalParagraph = lineEndsAt - line.length();
            return false;
        } else {
            return true;
        }
    }

    public boolean nextLineIsInParagraph() {
        if (currentLineNo + 1 == numberOfLinesInDocument) {
            return false;
        }

        String nextLine = selection.getLine(currentLineNo + 1);
        String[] nextLineParts = splitLine(nextLine);
        String leadingStringOfNextLine = nextLineParts[0];
        String mainTextOfNextLine = nextLineParts[1];

        if (mainTextOfNextLine.trim().isEmpty() || !leadingStringOfNextLine.equals(leadingString)) {
            try {
                lengthOfOriginalParagraph = selection.getEndLineOffset(currentLineNo) - offsetOfOriginalParagraph;
            } catch (BadLocationException e) {
                return false;
            }
            return false;
        } else {
            return true;
        }
    }

    public String getValidErrorInPos() {
        // Start building a list of lines of text in paragraph
        paragraph = new ArrayList<>();
        paragraph.add(this.getCurrentLine());

        // Check if it's a docstring (""" or ' or ")
        boolean isDocstring = this.leadingString.contains("\"\"\"") ||
                this.leadingString.contains("'") ||
                this.leadingString.contains("\"");

        if (isDocstring) {
            return "Cannot rewrap docstrings";
        }

        if (paragraph.get(0).trim().isEmpty()) {
            return "Currect selection is empty";
        }

        // Don't wrap empty lines or docstrings
        int startingLineNo = this.currentLineNo;

        // Add the lines before the line containing the selection
        while (this.previousLineIsInParagraph()) {
            this.currentLineNo--;
            paragraph.add(0, this.getCurrentLine());
        }

        // Add the lines after the line containing the selection
        this.currentLineNo = startingLineNo;
        while (this.nextLineIsInParagraph()) {
            this.currentLineNo++;
            paragraph.add(this.getCurrentLine());
        }

        if (paragraph.size() == 1 && paragraph.get(0).length() < noCols - this.leadingString.length()) {
            return "Current selection cannot be wrapped";
        }
        return null;
    }

    public ReplaceEdit getReplaceEdit() {
        // Rewrap the paragraph
        List<String> wrappedParagraph = new ArrayList<>();

        for (String line : paragraph) {
            wrappedParagraph.add(line.trim() + " ");
        }

        String fullParagraph = String.join("", wrappedParagraph);
        List<String> rewrappedParagraph = wrapText(fullParagraph, noCols - this.leadingString.length());

        // Add line terminators
        List<String> finalParagraph = new ArrayList<>();
        for (String line : rewrappedParagraph) {
            finalParagraph.add(this.leadingString + line + this.docDelimiter);
        }

        // Adjust the last line to remove the delimiter
        if (!finalParagraph.isEmpty()) {
            String lastLine = finalParagraph.get(finalParagraph.size() - 1);
            lastLine = lastLine.replace(this.docDelimiter, "");
            finalParagraph.set(finalParagraph.size() - 1, lastLine);
        }

        // Replace the original paragraph
        String newText = String.join("", finalParagraph);
        ReplaceEdit replaceEdit = new ReplaceEdit(this.offsetOfOriginalParagraph, this.lengthOfOriginalParagraph,
                newText);
        return replaceEdit;
    }

    public static List<String> wrapText(String text, int width) {
        // Mimic Python's textwrap functionality
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + width);
            if (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                while (end > start && !Character.isWhitespace(text.charAt(end))) {
                    end--;
                }
            }
            lines.add(text.substring(start, end).trim());
            start = end;
        }
        return lines;
    }

}
