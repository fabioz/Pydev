package com.python.pydev.refactoring.visitors;

public class WriterEraser implements IWriterEraser{

	StringBuffer buf = new StringBuffer();
	
	public void write(String o) {
		buf.append(o);
	}

	public void erase(String o) {
		int len = o.length();
		int bufLen = buf.length();
		buf.delete(bufLen-len, bufLen);
	}

	public StringBuffer getBuffer() {
		return buf;
	}

}
