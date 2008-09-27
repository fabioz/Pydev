/*
 * Author: atotic
 * Created: Jul 29, 2003
 */
package org.python.pydev.parser.jython;

/**
 * callbacks from the parser to the compiler
 *
 * org.totic.pydev needed to use org.python.parser outside of the the jython tree
 * and this class abstracts all calls from parser to the compiler
 */

public interface ICompilerAPI {

    public Object newLong(String s);

    public Object newLong(java.math.BigInteger i);

    public Object newFloat(double v);
    
    public Object newImaginary(double v);
    
    public Object newInteger(int i);
    
    public String decode_UnicodeEscape(String str, int start, int end,
        String errors, boolean unicode);
}
