package org.python.pydev.parser.jython;

import java.io.IOException;

/**
 * An implementation of interface CharStream, where the data is read from a Reader. Completely recreated so that we can read data directly from a String, as the
 * initial implementation was highly inneficient when working only with a string.
 */

public final class FastCharStream implements CharStream {

	private char[] buffer;

	private int bufline[];

	private int bufcolumn[];

	private boolean prevCharIsCR = false;

	private boolean prevCharIsLF = false;

	private int column = 0;

	private int line = 1;

	private int bufpos;

	private int updatePos;
	
	private int tokenBegin;

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

	public char readChar() throws IOException {
		try {
			char r = this.buffer[bufpos];
			bufpos++;
			if(bufpos >= updatePos){
				updatePos++;
				UpdateLineColumn(r);
			}
			return r;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IOException();
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
		return bufcolumn[bufpos-1];
	}

	public final int getEndLine() {
		return bufline[bufpos-1];
	}

	public final int getBeginColumn() {
		return bufcolumn[tokenBegin]+1;
	}

	public final int getBeginLine() {
		return bufline[tokenBegin]-1;
	}

	public void backup(int amount) {
		bufpos -= amount;
		if (bufpos < 0) {
			bufpos = 0;
		}
	}

	public char BeginToken() throws IOException {
		tokenBegin = bufpos;
		char c = readChar();
		return c;
	}

	public String GetImage() {
		String s = null;
		if (bufpos >= tokenBegin) {
			s = new String(buffer, tokenBegin, bufpos - tokenBegin);
		} else {
			s = new String(buffer, tokenBegin, buffer.length - tokenBegin);
		}
		return s;
	}

	public char[] GetSuffix(int len) {

		if ((bufpos + len) > buffer.length) {
			len = buffer.length - bufpos;
		}

		char[] ret = new char[len];
		if (len > 0) {
			try {
				System.arraycopy(buffer, bufpos - len, ret, 0, len);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	public void Done() {
		buffer = null;
	}

}
