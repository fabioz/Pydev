/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

/**
 * This class exists only for documentation purposes (empty class).
 * 
 * 
 * The classes that are available for parsing code or dealing with strings in Pydev are:
 * 
 * org.python.pydev.parser.PyParser
 *     Used to create a complete AST from Python (code-completion/code analysis use this 
 *     representation as it's pretty complete, but one of the slowest to generate -- other classes
 *     can be used when the complete info is not needed -- and those are also more syntax-error 
 *     friendly).
 *     
 * org.python.pydev.parser.fastparser.FastDefinitionsParser
 *     Provides only a part of the AST, containing classes, functions, class attributes, instance 
 *     attributes -- basically the tokens that provide a definition that can be 'globally' accessed.
 * 
 * org.python.pydev.core.docutils.PyDocIterator
 *     Traverses Python code skipping comments, strings, slash continues next line, ...
 * 
 * org.python.pydev.core.docutils.ParsingUtils
 *     Helper to parse python code (can skip comments, strings, ...)
 * 
 * org.python.pydev.core.docutils.PySelection
 *     Deals with the document given an offset. Preferred class to use when a selection is available.
 * 
 * org.python.pydev.parser.fastparser.FastParser
 *     Can create a structure only with the class and function declarations (for current scope,
 *     full document, only first, ...) 
 * 
 * org.python.pydev.core.docutils.StringUtils
 *     Dealing with raw strings: left trim, replace chars, split, replace new lines, change coding style, ... 
 * 
 * org.python.pydev.core.docutils.WrapAndCaseUtils
 *     Gotten from Apache. Utilities for wrapping text, capitalize, uncapitalize, swap case, ...
 * 
 * org.python.pydev.core.docutils.PyImportsHandling
 *     Dealing with imports
 *     
 */
public class HowToParseStringsAndPythonCode {

}
