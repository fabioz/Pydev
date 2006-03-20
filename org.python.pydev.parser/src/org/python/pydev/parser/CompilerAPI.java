/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.parser;

import org.python.pydev.parser.jython.IParserHost;

/**
 * Implement Py methods required by PythonGrammar
 * 
 */
public class CompilerAPI implements IParserHost {
	public Object newInteger(int i) {
		return new java.lang.Integer(i);
	}

	public Object newLong(String s) {
		return new java.lang.Long(s);
	}

	public Object newLong(java.math.BigInteger i) {
		return i;
	}

	public Object newImaginary(double v) {
		return new java.lang.Double(v);
	}

	public static Object newFloat(float v) {
		return new java.lang.Float(v);
	}

	public Object newFloat(double v) {
		return new java.lang.Float(v);
	}

	/**
	 * TODO how do I implement Unicode decoding in Java?
	 */
	public String decode_UnicodeEscape(String str, int start, int end, String errors, boolean unicode) {
		return str.substring(start, end);
	}
}