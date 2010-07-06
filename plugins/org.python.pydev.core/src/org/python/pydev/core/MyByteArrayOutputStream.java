package org.python.pydev.core;

import java.io.ByteArrayOutputStream;

/**
 * Byte array with methods to delete a part of its contents.
 * 
 * Note that it's not thread-safe!
 */
public class MyByteArrayOutputStream extends ByteArrayOutputStream{

    public MyByteArrayOutputStream() {
        super();
    }
    
    public MyByteArrayOutputStream(int i) {
        super(i);
    }

    public int deleteFirst(){
        byte ret = this.buf[0];
        System.arraycopy(this.buf, 1, this.buf, 0, this.buf.length-1);
        this.count--;
        return ret;
    }

    public int delete(byte[] b, int off, int len){
        if(this.size() < len){
            len = this.size();
        }
        if(len == 0){
            return 0;
        }
        System.arraycopy(this.buf, 0, b, off, len);
        int diff = this.count - len;
        
        System.arraycopy(this.buf, len, this.buf, 0, diff);
        this.count -= len;
        return len;
    }


}
