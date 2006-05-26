package org.python.pydev.editor.partitioner;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.core.IPythonPartitions;

/**
 * Based on org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner
 * 
 * Could become a replacement
 * 
 * @author fabioz
 */
public class FastPythonPartitionScanner implements IPartitionTokenScanner, IPythonPartitions{
	

	// states
	private static final int PYTHON= 0;
	private static final int COMMENT= 1;
	private static final int SINGLE_LINE_STRING1= 2; //'
	private static final int SINGLE_LINE_STRING2= 3; //"
	private static final int MULTI_LINE_STRING1= 4;  //'
	private static final int MULTI_LINE_STRING2= 5;  //""
	private static final int BACKQUOTES= 6;  
	
	
	/** The scanner. */
	private final BufferedDocumentScanner fScanner= new BufferedDocumentScanner(1000);	// faster implementation

	private final IToken[] fTokens= new IToken[] {
			new Token(null),
			new Token(PY_COMMENT),
			new Token(PY_SINGLELINE_STRING1),
			new Token(PY_SINGLELINE_STRING2),
			new Token(PY_MULTILINE_STRING1),
			new Token(PY_MULTILINE_STRING2),
			new Token(PY_BACKQUOTES),
		};
	private int fTokenOffset;
	private int fTokenLength;
	
	
	private static int getState(String contentType) {

		if (contentType == null)
			return PYTHON;

		else if (contentType.equals(PY_COMMENT))
			return COMMENT;

		else if (contentType.equals(PY_SINGLELINE_STRING1))
			return SINGLE_LINE_STRING1;
		
		else if (contentType.equals(PY_SINGLELINE_STRING2))
			return SINGLE_LINE_STRING2;

		else if (contentType.equals(PY_MULTILINE_STRING1))
			return MULTI_LINE_STRING1;
		
		else if (contentType.equals(PY_MULTILINE_STRING2))
			return MULTI_LINE_STRING2;
		
		else if (contentType.equals(PY_BACKQUOTES))
			return BACKQUOTES;

		else //DEFAULT
			return PYTHON;
	}

	public void setPartialRange(IDocument document, int offset, int length, String contentType, int partitionOffset) {
		fScanner.setRange(document, offset, length);
		fTokenOffset= offset;
		fTokenLength= 0;
	}

	public void setRange(IDocument document, int offset, int length) {
		fScanner.setRange(document, offset, length);
		fTokenOffset= offset;
		fTokenLength= 0;
	}


	public int getTokenOffset() {
		return fTokenOffset;
	}

	public int getTokenLength() {
		return fTokenLength;
	}

	/*
	 * @see org.eclipse.jface.text.rules.ITokenScanner#nextToken()
	 */
	public IToken nextToken() {
		fTokenOffset += fTokenLength;
		fTokenLength= 0;

		while (true) {
			final int ch= fScanner.read();

			// characters
	 		switch (ch) {
	 		
	 		case ICharacterScanner.EOF:
	 			fTokenLength++;
	 			return Token.EOF;
	 			
	 		case '#':
	 			int offsetEnd = fTokenOffset;
	 			int ch2 = fScanner.read();
	 			offsetEnd++;
	 			
	 	        while(ch2!= '\n' && ch2 != '\r' && ch != ICharacterScanner.EOF){
	 	        	offsetEnd++;
	 	        	ch2 = fScanner.read();
	 	        }
	 	        fTokenLength = offsetEnd-fTokenOffset;
	 	        return fTokens[COMMENT];
	 	        
	 		case '\'':
		 		offsetEnd = fTokenOffset;
		 		ch2 = fScanner.read();
		 		offsetEnd++;
		 		
		 		while(ch2!= '\n' && ch2 != '\r' && ch != ICharacterScanner.EOF){
		 			offsetEnd++;
		 			ch2 = fScanner.read();
		 		}
		 		fTokenLength = offsetEnd-fTokenOffset;
		 		return fTokens[COMMENT];
	 	        
	 		default:
	 			fTokenLength++;
	 			return fTokens[PYTHON];
	 		}
		}
 	}

}
