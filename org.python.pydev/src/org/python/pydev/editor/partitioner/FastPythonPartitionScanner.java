package org.python.pydev.editor.partitioner;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;
import org.python.pydev.core.IPythonPartitions;

/**
 * Based on org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner
 * 
 * @author fabioz
 */
public class FastPythonPartitionScanner implements IPartitionTokenScanner, IPythonPartitions{
	

	// states
	private static final int PYTHON= 0;
	private static final int SINGLE_LINE_COMMENT= 1;
	private static final int STRING= 2;
	private static final int MULTI_LINE_STRING= 3;
	
	// beginning of prefixes and postfixes
	private static final int NONE= 0;
	private static final int BACKSLASH= 1; // postfix for STRING and CHARACTER
	private static final int SLASH= 2; // prefix for SINGLE_LINE or MULTI_LINE or JAVADOC
	private static final int SLASH_STAR= 3; // prefix for MULTI_LINE_COMMENT or JAVADOC
	private static final int SLASH_STAR_STAR= 4; // prefix for MULTI_LINE_COMMENT or JAVADOC
	private static final int STAR= 5; // postfix for MULTI_LINE_COMMENT or JAVADOC
	private static final int CARRIAGE_RETURN=6; // postfix for STRING, CHARACTER and SINGLE_LINE_COMMENT

	
	
	/** The offset of the last returned token. */
	private int fTokenOffset;
	/** The length of the last returned token. */
	private int fTokenLength;

	/** The state of the scanner. */
	private int fState;
	/** The last significant characters read. */
	private int fLast;
	/** The amount of characters already read on first call to nextToken(). */
	private int fPrefixLength;

	
	/** The scanner. */
	private final BufferedDocumentScanner fScanner= new BufferedDocumentScanner(1000);	// faster implementation

	
	private static int getState(String contentType) {

		if (contentType == null)
			return PYTHON;

		else if (contentType.equals(PY_COMMENT))
			return SINGLE_LINE_COMMENT;

		else if (contentType.equals(PY_SINGLELINE_STRING1))
			return STRING;

		else if (contentType.equals(PY_MULTILINE_STRING1))
			return MULTI_LINE_STRING;

		else
			return PYTHON;
	}

	public void setPartialRange(IDocument document, int offset, int length, String contentType, int partitionOffset) {
		fScanner.setRange(document, offset, length);
		fTokenOffset= partitionOffset;
		fTokenLength= 0;
		fPrefixLength= offset - partitionOffset;
		fLast= NONE;

		if (offset == partitionOffset) {
			// restart at beginning of partition
			fState= PYTHON;
		} else {
			fState= getState(contentType);
		}
	}

	public void setRange(IDocument document, int offset, int length) {
		fScanner.setRange(document, offset, length);
		fScanner.setRange(document, offset, length);
		fTokenOffset= offset;
		fTokenLength= 0;
		fPrefixLength= 0;
		fLast= NONE;
		fState= PYTHON;
	}

	public IToken nextToken() {
		return null;
	}

	public int getTokenOffset() {
		return fTokenOffset;
	}

	public int getTokenLength() {
		return fTokenLength;
	}

}
