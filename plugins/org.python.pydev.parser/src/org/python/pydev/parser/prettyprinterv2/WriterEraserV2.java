package org.python.pydev.parser.prettyprinterv2;

import org.python.pydev.core.structure.FastStack;
import org.python.pydev.core.structure.FastStringBuffer;

public class WriterEraserV2 implements IWriterEraser{

    FastStack<FastStringBuffer> buf = new FastStack<FastStringBuffer>();
    
    public WriterEraserV2(){
        pushTempBuffer(); //this is the initial buffer (should never be removed)
    }
    
    public void write(String o) {
        buf.peek().append(o);
    }

    public void erase(String o) {
        FastStringBuffer buffer = buf.peek();
        if(buffer.toString().endsWith(o)){
            //only delete if it ends with what was passed
            int len = o.length();
            int bufLen = buffer.length();
            buffer.delete(bufLen-len, bufLen);
        }
    }

    @Override
    public boolean endsWithSpace() {
        FastStringBuffer current = buf.peek();
        if(current.length() == 0){
            return false;
        }
        return current.lastChar() == ' ';
    }
    
    public FastStringBuffer getBuffer() {
        return buf.peek();
    }

    public void pushTempBuffer() {
        buf.push(new FastStringBuffer());
    }

    public String popTempBuffer() {
        return buf.pop().toString();
    }

    @Override
    public String toString() {
        return "WriterEraser<"+buf.peek().toString()+">";
    }
}
