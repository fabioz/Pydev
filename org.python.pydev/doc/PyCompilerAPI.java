/*
 * Author: atotic
 * Created: Jul 29, 2003
 * License: Common Public License v1.0
 */
package org.python.parser;

import org.python.core.Py;
import org.python.core.PyString;
import java.math.BigInteger;

/**
 * 
 * TODO
 */
public class PyCompilerAPI implements ICompilerAPI {

	public Object newLong(String s) {
		return CompilerAPI.newLong(s);
	}

	public Object newLong(BigInteger i) {
		return CompilerAPI.newLong(i);
	}

	public Object newFloat(double v) {
		return CompilerAPI.newFloat(v);
	}

	public Object newImaginary(double v) {
		return CompilerAPI.newImaginary(v);
	}

	public Object newInteger(int i) {
		return CompilerAPI.newInteger(i);
	}

	public String decode_UnicodeEscape(
		String str, int start, int end, String errors, boolean unicode) {
			return PyString.decode_UnicodeEscape(str, start, end, errors, unicode);
	}

}
