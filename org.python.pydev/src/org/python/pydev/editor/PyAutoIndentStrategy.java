/*
 * Created on Dec 10, 2003
 * Author: atotic
 * License: Common Public License 1.0
 */

package org.python.pydev.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.plugin.PydevPrefs;

/**
 * Implements indenting behavior.
 *
 * <p>Tabs vs. spaces indentation
 * <p>Borrows heavily from {@link org.eclipse.jface.text.DefaultAutoIndentStrategy},
 * and the pyeclipse (PythonAutoIndentStrategy).
 */
public class PyAutoIndentStrategy extends DefaultAutoIndentStrategy {

	/** indentation string */
	String identString = null;
	//	should tab be converted to spaces?
	boolean useSpaces = PydevPrefs.getPreferences().getBoolean(PydevPrefs.SUBSTITUTE_TABS);	
	int tabWidth = PydevPrefs.getPreferences().getInt(PydevPrefs.TAB_WIDTH);
	boolean forceTabs = false;
	
	public void setForceTabs(boolean forceTabs) {
		this.forceTabs = forceTabs;
	}
	
	private String createSpaceString(int width) {
		StringBuffer b = new StringBuffer(width);
						while (tabWidth-- > 0)
							b.append(" ");
		return b.toString();
	}

	/** returns correct single-step indentation */
	private String getIndentationString() {
		if (identString == null ||
			tabWidth != PydevPrefs.getPreferences().getInt(PydevPrefs.TAB_WIDTH) ||
			useSpaces != PydevPrefs.getPreferences().getBoolean(PydevPrefs.SUBSTITUTE_TABS))
		{
			tabWidth = PydevPrefs.getPreferences().getInt(PydevPrefs.TAB_WIDTH);
			useSpaces = PydevPrefs.getPreferences().getBoolean(PydevPrefs.SUBSTITUTE_TABS);
			if (useSpaces && !forceTabs)
				identString = createSpaceString(tabWidth);
			else
				identString = "\t";
		}
		return identString;
	}
	
	private boolean isWhitespace(String s) {
		for (int i = s.length() - 1; i > -1 ; i--)
			if (!Character.isWhitespace(s.charAt(i)))
				return false;
		return true;
	}


	/**
	 * Replaces tabs if needed by ident string or just a space depending of the
	 * tab location
	 * 
	 */
	protected String convertTabs(
		IDocument document, int length, String text, int offset, 
		String indentString) throws BadLocationException 
	{
		// only interresting if it contains a tab (also if it is a tab only)
		if (text.indexOf("\t") >= 0) {
			// get some text infos
			int lineStart = 
				document.getLineInformationOfOffset(offset).getOffset();
			String line = document.get(lineStart, offset - lineStart);
			// only a single tab?
			if (text.equals("\t")) {
				deleteWhitespaceAfter(document, offset);
				if (isWhitespace(line))
					text = indentString;
				else
					text = " ";
				// contains a char (pasted text)
			} else {
				byte[] byteLine = text.getBytes();
				StringBuffer newText = new StringBuffer();
				for (int count = 0; count < byteLine.length; count++) {
					if (byteLine[count] == '\t')
						newText.append(indentString);
						// if it is not a tab add the char
					else
						newText.append((char) byteLine[count]);
				}
				text = newText.toString();
			}
		}
		return text;
	}
	
	/**
	 * Converts spaces to strings. Useful when pasting
	 */
	protected String convertSpaces(
		IDocument document, int length, String text, int offset, 
		String indentString) throws BadLocationException 
	{
		if (text.length() > 2)
			return text;
		return text.replaceAll(createSpaceString(tabWidth), "\t");
	}

	/**
	 * When hitting TAB, delete the whitespace after the cursor in the line
	 */
	protected void deleteWhitespaceAfter(IDocument document, int offset)
		throws BadLocationException {
		if (offset < document.getLength()
			&& !endsWithNewline(document, document.get(offset, 1))) {
			int lineLength =
				document.getLineInformationOfOffset(offset).getLength();
			int lineStart =
				document.getLineInformationOfOffset(offset).getOffset();
			String textAfter =
				document.get(offset, (lineStart + lineLength) - offset);
			if (textAfter.length() > 0
				&& isWhitespace(textAfter)) {
				document.replace(offset, textAfter.length(), "");
			}
		}
	}

	/**
	 * 
	 * @param document
	 * @param length
	 * @param text
	 * @param offset
	 * @return String
	 * @throws BadLocationException
	 */
	protected String autoIndentNewline(
						IDocument document, int length, String text, int offset)
					throws BadLocationException 
	{
		if (length == 0 && text != null && endsWithNewline(document, text)) {
			if (offset > 0 && document.getChar(offset - 1) == ':')
				text = text + getIndentationString();
		}
		return text;
	}

	/**
	 * True if text ends with a newline delimiter
	 */
	private boolean endsWithNewline(IDocument document, String text) {
		String[] newlines = document.getLegalLineDelimiters();
		boolean ends = false;
		for (int i = 0; i < newlines.length; i++) {
			String delimiter = newlines[i];
			if (text.indexOf(delimiter) != -1)
				ends = true;
		}
		return ends;
	}
	
	/**
	 * 
	 * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(IDocument, DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		// super idents newlines the same amount as the previous line
		super.customizeDocumentCommand(document, command);
		try {
			command.text = autoIndentNewline(
					document, command.length, command.text, command.offset);
			if (PydevPrefs.getPreferences().getBoolean(PydevPrefs.SUBSTITUTE_TABS))
				command.text = convertTabs(
					document, command.length, command.text, command.offset,
					getIndentationString());
			else command.text = convertSpaces(
				document, command.length, command.text, command.offset,
				getIndentationString());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

}
