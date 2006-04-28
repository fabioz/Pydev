package org.python.pydev.parser.jython;

import java.io.IOException;

/**
 * An implementation of interface CharStream, where the data is read from a Reader. Completely recreated so that we can read data directly from a String, as the
 * initial implementation was highly inneficient when working only with a string (actually, if it was small, there would be no noticeable
 * delays, but if it became big, then the improvement would be HUGE).
 * 
 * It keeps the same semantics for line and column stuff (and shares the previous approach of keeping a buffer for this info).
 * If we wanted we could optimize it for also taking less memory, but as there is usually not so many concurrent parses, this 
 * is probably not worth it -- and it would probably be a little slower)
 */

public final class FastCharStream implements CharStream {

	private char[] buffer;

	private int bufline[];

	private int bufcolumn[];

	private boolean prevCharIsCR = false;

	private boolean prevCharIsLF = false;

	private int column = 0;

	private int line = 1;

	private int bufpos = -1;

	private int updatePos;
	
	private int tokenBegin;
	
	private static IOException ioException;

	private static final boolean DEBUG = false;

	private final void UpdateLineColumn(char c) {
		column++;

		if (prevCharIsLF) {
			prevCharIsLF = false;
			line += (column = 1);
		} else if (prevCharIsCR) {
			prevCharIsCR = false;
			if (c == '\n') {
				prevCharIsLF = true;
			} else
				line += (column = 1);
		}

		switch (c) {
		case '\r':
			prevCharIsCR = true;
			break;
		case '\n':
			prevCharIsLF = true;
			break;
		// ok, this was commented out because the position would not reflect correctly the positions found in the ast.
		// this may have other problems, but they have to be analyzed better to see the problems this may bring
		// (files that mix tabs and spaces may suffer, but I could not find out very well the problems -- anyway,
		// restricting the analysis to files that have only tabs or only spaces seems reasonable -- shortcuts are available
		// so that we can convert a file from one type to another, so, what remains is making some lint analysis to be sure of it).
		// case '\t' :
		// column--;
		// column += (8 - (column & 07));
		// break;
		default:
			break;
		}

		bufline[bufpos] = line;
		bufcolumn[bufpos] = column;
	}

	public FastCharStream(String initialDoc) {
		this.buffer = initialDoc.toCharArray();
		this.bufline = new int[initialDoc.length()];
		this.bufcolumn = new int[initialDoc.length()];
	}

	public final char readChar() throws IOException {
		try {
		    bufpos++;
			char r = this.buffer[bufpos];
			if(bufpos >= updatePos){
				updatePos++;
				UpdateLineColumn(r);
			}
			if(DEBUG){
				System.out.println("FastCharStream: readChar >>"+(int)r+"<<");
			}
			return r;
		} catch (ArrayIndexOutOfBoundsException e) {
		    bufpos--;
		    if (ioException == null){
		    	ioException = new IOException();
		    }
			throw ioException;
		}
	}

	/**
	 * @deprecated
	 * @see #getEndColumn
	 */

	public final int getColumn() {
		return bufcolumn[bufpos];
	}

	/**
	 * @deprecated
	 * @see #getEndLine
	 */

	public final int getLine() {
		return bufline[bufpos];
	}

	public final int getEndColumn() {
		return bufcolumn[bufpos];
	}

	public final int getEndLine() {
		return bufline[bufpos];
	}

	public final int getBeginColumn() {
		return bufcolumn[tokenBegin];
	}

	public final int getBeginLine() {
		return bufline[tokenBegin];
	}

	public final void backup(int amount) {
		if(DEBUG){
			System.out.println("FastCharStream: backup >>"+amount+"<<");
		}
		bufpos -= amount;
	}

	public final char BeginToken() throws IOException {
		char c = readChar();
		tokenBegin = bufpos;
		if(DEBUG){
			System.out.println("FastCharStream: BeginToken >>"+(int)c+"<<");
		}
		return c;
	}

	public final String GetImage() {
		String s = null;
		if (bufpos >= tokenBegin) {
			s = new String(buffer, tokenBegin, bufpos - tokenBegin+1);
		} else {
			s = new String(buffer, tokenBegin, buffer.length - tokenBegin+1);
		}
		if(DEBUG){
			System.out.println("FastCharStream: GetImage >>"+s+"<<");
		}
		return s;
	}

	public final char[] GetSuffix(int len) {

		char[] ret = new char[len];
		if (len > 0) {
			try {
                int initial = bufpos - len +1;
                if(initial < 0){
                    int initial0 = initial;
                    len += initial;
                    initial = 0;
                    System.arraycopy(buffer, initial, ret, -initial0, len);
                }else{
                    System.arraycopy(buffer, initial, ret, 0, len);
                }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(DEBUG){
			System.out.println("FastCharStream: GetSuffix:"+len+" >>"+new String(ret)+"<<");
		}
		return ret;
	}

	public final void Done() {
		buffer = null;
		bufline = null;
		bufcolumn = null;
	}

}
