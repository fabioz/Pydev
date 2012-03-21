package org.python.pydev.dltk.console.codegen;

import java.io.File;

import org.eclipse.ui.ide.ResourceUtil;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;

/**
 * A collection of utilities to help create snippets of Python Code
 */
public final class PythonSnippetUtils {

	/**
	 * Creates an "execfile" command for the argument file, escaping the name
	 * file as needed.
	 * 
	 * @param file
	 *            file to execfile
	 * @return Python snippet
	 */
	public static String getExecfileCommand(File file) {
		return StringUtils.format("execfile(%s)\n", getSingleQuotedString(file.toString()));
	}

	private static String getQuotedString(char quote, String string) {
		// Implemented using SimpleRunner.getArgumentsAsStr as a starting point
		if (string == null || string.length() == 0)
			return "" + quote + quote;
		FastStringBuffer buf = new FastStringBuffer();

		buf.append(quote);
		char[] characters = string.toCharArray();
		for (int j = 0; j < characters.length; j++) {
			char character = characters[j];
			if (character == quote) {
				buf.append('\\');
			} else if (character == '\\') {
				buf.append('\\');
			}
			buf.append(character);
		}

		buf.append(quote);

		return buf.toString();
	}

	/**
	 * Returns a singly (') quoted string
	 * 
	 * @param string
	 *            to quote
	 * @return singly quote string
	 */
	public static String getSingleQuotedString(String string) {
		return getQuotedString('\'', string);
	}

	/**
	 * Returns a doubly (") quoted string
	 * 
	 * @param string
	 *            to quote
	 * @return doubly quote string
	 */
	public static String getDoubleQuotedString(String string) {
		return getQuotedString('\"', string);
	}

	/**
	 * Get a IScriptConsoleCodeGenerator adapted object for object
	 * @param object to adapt
	 * @return adapted object, or <code>null</code> if not adaptable
	 * @see IScriptConsoleCodeGenerator
	 */
	public static IScriptConsoleCodeGenerator getScriptConsoleCodeGeneratorAdapter(Object object) {
		if (object == null) {
			return null;
		}
		if (object instanceof IScriptConsoleCodeGenerator) {
			return (IScriptConsoleCodeGenerator) object;
		}
		Object adaptedNode = ResourceUtil.getAdapter(object, IScriptConsoleCodeGenerator.class, true);
		if (adaptedNode instanceof IScriptConsoleCodeGenerator) {
			return (IScriptConsoleCodeGenerator) adaptedNode;
		}
		return null;
	}
}
