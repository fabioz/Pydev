/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.autoedit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractIndentPrefs implements IIndentPrefs {

    private boolean forceTabs = false;

    @Override
    public boolean getForceTabs() {
        return forceTabs;
    }

    @Override
    public void setForceTabs(boolean forceTabs) {
        this.forceTabs = forceTabs;
    }

    /**
     * Naive implementation. Always redoes the indentation string based in the
     * spaces and tabs settings. 
     * 
     * @see org.python.pydev.core.IIndentPrefs#getIndentationString()
     */
    @Override
    public String getIndentationString() {
        if (getUseSpaces(true)) {
            return StringUtils.createSpaceString(getTabWidth());
        } else {
            return "\t";
        }
    }

    /**
     * Converts spaces to tabs or vice-versa depending on the user preferences
     */
    @Override
    public void convertToStd(IDocument document, DocumentCommand command) {
        try {
            if (getUseSpaces(true)) {
                command.text = convertTabsToSpaces(document, command.text, command.offset, getIndentationString());
            }

            else {
                command.text = convertSpacesToTabs(document, command.text, command.offset, getIndentationString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    //------------------------------------------------------------- UTILS

    /**
     * Replaces tabs if needed by indent string or just a space depending of the
     * tab location
     * 
     */
    private String convertTabsToSpaces(IDocument document, String text, int offset, String indentString)
            throws BadLocationException {
        // only interesting if it contains a tab (also if it is a tab only)
        if (text.indexOf("\t") != -1) {
            // get some text infos

            if (text.equals("\t")) {
                //only a single tab?
                deleteWhitespaceAfter(document, offset);
                text = indentString;

            } else {
                // contains a char (pasted text)
                char[] chars = text.toCharArray();
                FastStringBuffer newText = new FastStringBuffer();
                for (int count = 0; count < chars.length; count++) {
                    if (chars[count] == '\t') {
                        newText.append(indentString);

                    } else { // if it is not a tab add the char
                        newText.append(chars[count]);
                    }
                }
                text = newText.toString();
            }
        }
        return text;
    }

    /**
     * Converts spaces to strings. Useful when pasting
     */
    private String convertSpacesToTabs(IDocument document, String text, int offset, String indentString)
            throws BadLocationException {
        String spaceStr = StringUtils.createSpaceString(getTabWidth());
        while (text.startsWith(spaceStr)) {
            text = text.replaceAll(spaceStr, "\t");
        }
        return text;
    }

    /**
     * When hitting TAB, delete the whitespace after the cursor in the line
     */
    private void deleteWhitespaceAfter(IDocument document, int offset) throws BadLocationException {
        if (offset < document.getLength() && !TextSelectionUtils.endsWithNewline(document, document.get(offset, 1))) {

            int lineLength = document.getLineInformationOfOffset(offset).getLength();
            int lineStart = document.getLineInformationOfOffset(offset).getOffset();
            String textAfter = document.get(offset, (lineStart + lineLength) - offset);

            if (textAfter.length() > 0 && isWhitespace(textAfter)) {
                document.replace(offset, textAfter.length(), "");
            }
        }
    }

    /**
     * Checks if the string is solely composed of spaces 
     * 
     * @param s the string analyzed
     * @return true if it's only composed of spaces and false otherwise.
     */
    private boolean isWhitespace(String s) {
        int len = s.length();

        //it's done backwards because the chance of finding a non-whitespace char is higher at the end of the string
        //than at the beggining
        for (int i = len - 1; i > -1; i--) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
