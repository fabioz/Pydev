package com.python.pydev.refactoring.visitors;

import org.python.pydev.core.structure.FastStack;

public class WriterEraser implements IWriterEraser{

	FastStack<StringBuffer> buf = new FastStack<StringBuffer>();
	
    public WriterEraser(){
        pushTempBuffer(); //this is the initial buffer (should never be removed)
    }
    
	public void write(String o) {
		buf.peek().append(o);
	}

	public void erase(String o) {
        StringBuffer buffer = buf.peek();
        if(buffer.toString().endsWith(o)){
            //only delete if it ends with what was passed
    		int len = o.length();
    		int bufLen = buffer.length();
            buffer.delete(bufLen-len, bufLen);
        }
	}

	public StringBuffer getBuffer() {
		return buf.peek();
	}

    public void pushTempBuffer() {
        buf.push(new StringBuffer());
    }

    public String popTempBuffer() {
        return buf.pop().toString();
    }

    @Override
    public String toString() {
        return "WriterEraser<"+buf.peek().toString()+">";
    }
}
