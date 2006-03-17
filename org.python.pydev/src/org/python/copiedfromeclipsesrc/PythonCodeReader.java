package org.python.copiedfromeclipsesrc;

import java.io.IOException;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;

/**
 * The reader works well as long as we are not inside a string at the current offset (this is not enforced here, so,
 * use at your own risk.
 * 
 * @author Fabio Zadrozny
 */
public class PythonCodeReader {
	
	/** The EOF character */
	public static final int EOF= -1;
	
	private boolean fSkipComments= false;
	private boolean fSkipStrings= false;
	private boolean fForward= false;
	
	private IDocument fDocument;
	private int fOffset;
	
	private int fEnd= -1;
	
	
	public PythonCodeReader() {
	}
	
	/**
	 * Returns the offset of the last read character. Should only be called after read has been called.
	 */
	public int getOffset() {
		return fForward ? fOffset -1 : fOffset;
	}
	
	public void configureForwardReader(IDocument document, int offset, int length, boolean skipComments, boolean skipStrings) throws IOException {
	    //currently not implemented without skip, so, that's the reason the asserts are here...
        Assert.isTrue(skipComments);
        Assert.isTrue(skipStrings);

        fDocument= document;
		fOffset= offset;
		fSkipComments= skipComments;
		fSkipStrings= skipStrings;
		
		fForward= true;
		fEnd= Math.min(fDocument.getLength(), fOffset + length);		
	}
	
	public void configureBackwardReader(IDocument document, int offset, boolean skipComments, boolean skipStrings) throws IOException {
        //currently not implemented without skip, so, that's the reason the asserts are here...
	    Assert.isTrue(skipComments);
	    Assert.isTrue(skipStrings);
		
        fDocument= document;
		fOffset= offset;
		fSkipComments= skipComments;
		fSkipStrings= skipStrings;
		
		fForward= false;
	}
	
	/*
	 * @see Reader#close()
	 */
	public void close() throws IOException {
		fDocument= null;
	}
	
	/*
	 * @see SingleCharReader#read()
	 */
	public int read() throws IOException {
		try {
			return fForward ? readForwards() : readBackwards();
		} catch (BadLocationException x) {
			throw new RuntimeException(x);
		}
	}
	
    private void gotoStringStart(char delimiter) throws BadLocationException {
        boolean isMulti = false;
        
        if(fOffset >= 2){
            if(fDocument.getChar(fOffset) == delimiter && fDocument.getChar(fOffset -1) == delimiter){
                isMulti = true;
                fOffset--;
                fOffset--;
            }
        }

        while (0 < fOffset) {
            char current= fDocument.getChar(fOffset);
            if (current == delimiter) {
                if( !(0 <= fOffset && fDocument.getChar(fOffset -1) == '\\')){
                    if(isMulti){
                        if(fDocument.getChar(fOffset) == delimiter && fDocument.getChar(fOffset -1) == delimiter){
                            return;
                        }
                    }else{
                        return;
                    }
                }
            }
            -- fOffset;
        }
    }

    
	private int readForwards() throws BadLocationException {
		while (fOffset < fEnd) {
			char current= fDocument.getChar(fOffset++);
			
			switch (current) {
				case '#':
                    fOffset = ParsingUtils.eatComments(fDocument, new StringBuffer(), fOffset);
					return current;
					
				case '"':
				case '\'':
				    fOffset = ParsingUtils.eatLiterals(fDocument, new StringBuffer(), fOffset-1)+1;
					continue;
			}
			
			return current;
		}
		
		return EOF;
	}
	
		
	private int readBackwards() throws BadLocationException {
		
		while (0 < fOffset) {
			-- fOffset;
			
			handleComment();
			char current= fDocument.getChar(fOffset);
			switch (current) {
            
    			case '"':
                case '\'':
                    -- fOffset;
                    gotoStringStart(current);
                    continue;
            
                default:
				return current;
			}
			
		}
		
		return EOF;
	}

    //works as a cache so that we don't have to handle some line over and over again for comments
    private int handledLine = -1;
    private void handleComment() throws BadLocationException {
        int lineOfOffset = fDocument.getLineOfOffset(fOffset);
        if(handledLine == lineOfOffset){
            return;
        }
        handledLine = lineOfOffset;
        String line = PySelection.getLine(fDocument, lineOfOffset);
        int i;
        if( (i = line.indexOf('#')) != -1){
            IRegion lineInformation = fDocument.getLineInformation(lineOfOffset);
            int offset = lineInformation.getOffset() + i;
            if(offset < fOffset){
            	fOffset = offset;
            }
        }
    }
}

