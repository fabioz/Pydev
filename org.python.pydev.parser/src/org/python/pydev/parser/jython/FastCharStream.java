package org.python.pydev.parser.jython;

import java.io.IOException;

/**
 * An implementation of interface CharStream, where the data is read from
 * a Reader.
 * 
 * Completely recreated so that we can read data directly from a String, as the initial implementation
 * was highly inneficient when working only with a string.
 */

public final class FastCharStream implements CharStream
{

	private char[] buffer;
	private int bufpos;
	private int tokenBegin;

	public FastCharStream(String initialDoc) {
		this.buffer = initialDoc.toCharArray();
	}

	public char readChar() throws IOException {
		try {
			char r = this.buffer[bufpos];
			bufpos++;
			return r;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IOException();
		}
	}

	public int getColumn() {
		return 0;
	}

	public int getLine() {
		return 0;
	}

	public int getEndColumn() {
		return 0;
	}

	public int getEndLine() {
		return 0;
	}

	public int getBeginColumn() {
		return 0;
	}

	public int getBeginLine() {
		return 0;
	}

	public void backup(int amount) {
		bufpos -= amount;
		if(bufpos < 0){
			bufpos = 0;
		}
	}

	public char BeginToken() throws IOException {
		char c = readChar();
		tokenBegin = bufpos;
		return c;
	}

	public String GetImage() {
	     if (bufpos >= tokenBegin)
	         return new String(buffer, tokenBegin, bufpos - tokenBegin + 1);
	      else
	         return new String(buffer, tokenBegin, buffer.length - tokenBegin) + new String(buffer, 0, bufpos + 1);
	}

	public char[] GetSuffix(int len) {

	     if ((bufpos + len) > buffer.length){
	    	 len = buffer.length - bufpos;
	     }
	     
	     char[] ret = new char[len];
	     if(len > 0){
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
