/*
 * Author: atotic
 * Created on Apr 14, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.python.pydev.core.docutils.DocUtils;

/**
 * Our double-click implementation. 
 * 
 * Copied org.eclipse.jdt.internal.ui.text.java.JavaDoubleClickStrategy.
 */
public class PyDoubleClickStrategy implements ITextDoubleClickStrategy {

	public class JavaCodeReader extends Reader {
	
		/** The EOF character */
		public static final int EOF= -1;
	
		private boolean fSkipComments= false;
		private boolean fSkipStrings= false;
		private boolean fForward= false;
	
		private IDocument fDocument;
		private int fOffset;
	
		private int fEnd= -1;
		private int fCachedLineNumber= -1;
		private int fCachedLineOffset= -1;
	
	
		public int read(char cbuf[], int off, int len) throws IOException {
			int end= off + len;
			for (int i= off; i < end; i++) {
				int ch= read();
				if (ch == -1) {
					if (i == off) {
						return -1;
					} else {
						return i - off;
					}
				}
				cbuf[i]= (char)ch;
			}
			return len;
		}		
	
		/**
		 * @see Reader#ready()
		 */		
		public boolean ready() throws IOException {
			return true;
		}
	
		/**
		 * Gets the content as a String
		 */
		public String getString() throws IOException {
			StringBuffer buf= new StringBuffer();
			int ch;
			while ((ch= read()) != -1) {
				buf.append((char)ch);
			}
			return buf.toString();
		}
		public JavaCodeReader() {
		}
	
		/**
		 * Returns the offset of the last read character. Should only be called after read has been called.
		 */
		public int getOffset() {
			return fForward ? fOffset -1 : fOffset;
		}
	
		public void configureForwardReader(IDocument document, int offset, int length, boolean skipComments, boolean skipStrings) throws IOException {
			fDocument= document;
			fOffset= offset;
			fSkipComments= skipComments;
			fSkipStrings= skipStrings;
		
			fForward= true;
			fEnd= Math.min(fDocument.getLength(), fOffset + length);		
		}
	
		public void configureBackwardReader(IDocument document, int offset, boolean skipComments, boolean skipStrings) throws IOException {
			fDocument= document;
			fOffset= offset;
			fSkipComments= skipComments;
			fSkipStrings= skipStrings;
		
			fForward= false;
			try {
				fCachedLineNumber= fDocument.getLineOfOffset(fOffset);
			} catch (BadLocationException x) {
				throw new IOException(x.getMessage());
			}
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
				throw new IOException(x.getMessage());
			}
		}
	
		private void gotoCommentEnd() throws BadLocationException {
			while (fOffset < fEnd) {
				char current= fDocument.getChar(fOffset++);
				if (current == '*') {
					if (fOffset < fEnd && fDocument.getChar(fOffset) == '/') {
						++ fOffset;
						return;
					}
				}
			}
		}
	
		private void gotoStringEnd(char delimiter) throws BadLocationException {
			while (fOffset < fEnd) {
				char current= fDocument.getChar(fOffset++);
				if (current == '\\') {
					// ignore escaped characters
					++ fOffset;
				} else if (current == delimiter) {
					return;
				}
			}
		}
	
		private void gotoLineEnd() throws BadLocationException {
			int line= fDocument.getLineOfOffset(fOffset);
			fOffset= fDocument.getLineOffset(line + 1);
		}
	
		private int readForwards() throws BadLocationException {
			while (fOffset < fEnd) {
				char current= fDocument.getChar(fOffset++);
			
				switch (current) {
					case '/':
					
						if (fSkipComments && fOffset < fEnd) {
							char next= fDocument.getChar(fOffset);
							if (next == '*') {
								// a comment starts, advance to the comment end
								++ fOffset;
								gotoCommentEnd();
								continue;
							} else if (next == '/') {
								// '//'-comment starts, advance to the line end
								gotoLineEnd();
								continue;
							}
						}
					
						return current;
					
					case '"':
					case '\'':
				
						if (fSkipStrings) {
							gotoStringEnd(current);
							continue;
						}
					
						return current;
				}
			
				return current;
			}
		
			return EOF;
		}
	
		private void handleSingleLineComment() throws BadLocationException {
			int line= fDocument.getLineOfOffset(fOffset);
			if (line < fCachedLineNumber) {
				fCachedLineNumber= line;
				fCachedLineOffset= fDocument.getLineOffset(line);
				int offset= fOffset;
				while (fCachedLineOffset < offset) {
					char current= fDocument.getChar(offset--);
					if (current == '/' && fCachedLineOffset <= offset && fDocument.getChar(offset) == '/') {
						fOffset= offset;
						return;
					}
				}
			}
		}
	
		private void gotoCommentStart() throws BadLocationException {
			while (0 < fOffset) {
				char current= fDocument.getChar(fOffset--);
				if (current == '*' && 0 <= fOffset && fDocument.getChar(fOffset) == '/')
					return;
			}
		}
	
		private void gotoStringStart(char delimiter) throws BadLocationException {
			while (0 < fOffset) {
				char current= fDocument.getChar(fOffset);
				if (current == delimiter) {
					if ( !(0 <= fOffset && fDocument.getChar(fOffset -1) == '\\'))
						return;
				}
				-- fOffset;
			}
		}
		
		private int readBackwards() throws BadLocationException {
		
			while (0 < fOffset) {
				-- fOffset;
			
				handleSingleLineComment();
			
				char current= fDocument.getChar(fOffset);
				switch (current) {
					case '/':
					
						if (fSkipComments && fOffset > 1) {
							char next= fDocument.getChar(fOffset - 1);
							if (next == '*') {
								// a comment ends, advance to the comment start
								fOffset -= 2;
								gotoCommentStart();
								continue;
							}
						}
					
						return current;
					
					case '"':
					case '\'':
				
						if (fSkipStrings) {
							-- fOffset;
							gotoStringStart(current);
							continue;
						}
					
						return current;
				}
			
				return current;
			}
		
			return EOF;
		}
	}

	
	/**
	 * swiped an internal class straight out of Java code
	 * it was originaly org.eclipse.jdt.internal.ui.text.JavaPairMatcher
	 */
	public class JavaPairMatcher implements ICharacterPairMatcher {
	
		protected char[] fPairs;
		protected IDocument fDocument;
		protected int fOffset;
	
		protected int fStartPos;
		protected int fEndPos;
		protected int fAnchor;
	
		protected JavaCodeReader fReader= new JavaCodeReader();
	
	
		public JavaPairMatcher(char[] pairs) {
			fPairs= pairs;
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.source.ICharacterPairMatcher#match(org.eclipse.jface.text.IDocument, int)
		 */
		public IRegion match(IDocument document, int offset) {

			fOffset= offset;

			if (fOffset < 0)
				return null;

			fDocument= document;

			if (fDocument != null && matchPairsAt() && fStartPos != fEndPos)
				return new Region(fStartPos, fEndPos - fStartPos + 1);
			
			return null;
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.source.ICharacterPairMatcher#getAnchor()
		 */
		public int getAnchor() {
			return fAnchor;
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.text.source.ICharacterPairMatcher#dispose()
		 */
		public void dispose() {
			clear();
			fDocument= null;
			fReader= null;
		}
	
		/*
		 * @see org.eclipse.jface.text.source.ICharacterPairMatcher#clear()
		 */
		public void clear() {
			if (fReader != null) {
				try {
					fReader.close();
				} catch (IOException x) {
					// ignore
				}
			}
		}
	
		protected boolean matchPairsAt() {

			int i;
			int pairIndex1= fPairs.length;
			int pairIndex2= fPairs.length;

			fStartPos= -1;
			fEndPos= -1;

			// get the chars preceding and following the start position
			try {

				char prevChar= fDocument.getChar(Math.max(fOffset - 1, 0));
//	   modified behavior for http://dev.eclipse.org/bugs/show_bug.cgi?id=16879			
//				char nextChar= fDocument.getChar(fOffset);

				// search for opening peer character next to the activation point
				for (i= 0; i < fPairs.length; i= i + 2) {
//					if (nextChar == fPairs[i]) {
//						fStartPos= fOffset;
//						pairIndex1= i;
//					} else 
					if (prevChar == fPairs[i]) {
						fStartPos= fOffset - 1;
						pairIndex1= i;
					}
				}
			
				// search for closing peer character next to the activation point
				for (i= 1; i < fPairs.length; i= i + 2) {
					if (prevChar == fPairs[i]) {
						fEndPos= fOffset - 1;
						pairIndex2= i;
					} 
//					else if (nextChar == fPairs[i]) {
//						fEndPos= fOffset;
//						pairIndex2= i;
//					}
				}

				if (fEndPos > -1) {
					fAnchor= RIGHT;
					fStartPos= searchForOpeningPeer(fEndPos, fPairs[pairIndex2 - 1], fPairs[pairIndex2], fDocument);
					if (fStartPos > -1)
						return true;
					else
						fEndPos= -1;
				}	else if (fStartPos > -1) {
					fAnchor= LEFT;
					fEndPos= searchForClosingPeer(fStartPos, fPairs[pairIndex1], fPairs[pairIndex1 + 1], fDocument);
					if (fEndPos > -1)
						return true;
					else
						fStartPos= -1;
				}

			} catch (BadLocationException x) {
			} catch (IOException x) {
			}

			return false;
		}
	
		protected int searchForClosingPeer(int offset, int openingPeer, int closingPeer, IDocument document) throws IOException {
		
			fReader.configureForwardReader(document, offset + 1, document.getLength(), true, true);
		
			int stack= 1;
			int c= fReader.read();
			while (c != JavaCodeReader.EOF) {
				if (c == openingPeer && c != closingPeer)
					stack++;
				else if (c == closingPeer)
					stack--;
				
				if (stack == 0)
					return fReader.getOffset();
				
				c= fReader.read();
			}
		
			return  -1;
		}
	
		protected int searchForOpeningPeer(int offset, int openingPeer, int closingPeer, IDocument document) throws IOException {
		
			fReader.configureBackwardReader(document, offset, true, true);
		
			int stack= 1;
			int c= fReader.read();
			while (c != JavaCodeReader.EOF) {
				if (c == closingPeer && c != openingPeer)
					stack++;
				else if (c == openingPeer)
					stack--;
				
				if (stack == 0)
					return fReader.getOffset();
				
				c= fReader.read();
			}
		
			return -1;
		}
	}
	protected JavaPairMatcher fPairMatcher = new JavaPairMatcher(DocUtils.BRACKETS);

	/**
	 * @see ITextDoubleClickStrategy#doubleClicked
	 */
	public void doubleClicked(ITextViewer textViewer) {

		int offset = textViewer.getSelectedRange().x;

		if (offset < 0)
			return;

		IDocument document = textViewer.getDocument();

		IRegion region = fPairMatcher.match(document, offset);
		if (region != null && region.getLength() >= 2)
			textViewer.setSelectedRange(
				region.getOffset() + 1,
				region.getLength() - 2);
		else
			selectWord(textViewer, document, offset);
	}

	protected void selectWord(
		ITextViewer textViewer,
		IDocument document,
		int anchor) {

		try {

			int offset = anchor;
			char c;

			while (offset >= 0) {
				c = document.getChar(offset);
				if (!Character.isJavaIdentifierPart(c))
					break;
				--offset;
			}

			int start = offset;

			offset = anchor;
			int length = document.getLength();

			while (offset < length) {
				c = document.getChar(offset);
				if (!Character.isJavaIdentifierPart(c))
					break;
				++offset;
			}

			int end = offset;

			if (start == end)
				textViewer.setSelectedRange(start, 0);
			else
				textViewer.setSelectedRange(start + 1, end - start - 1);

		} catch (BadLocationException x) {
		}
	}
}
