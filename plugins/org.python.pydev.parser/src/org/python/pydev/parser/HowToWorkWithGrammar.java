/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

/**
 * Working with the Pydev Grammar 
 * ==============================
 * 
 * This page shows the steps needed for modifying and creating a new grammar.
 * 
 * 
 * Where to start? 
 * -----------------
 * 
 * The org.python.pyev.parser plugin contains all the grammars supported by pydev. JavaCC knowledge
 * is needed to edit those files (most notably the python.jjt_template files -- python.jjt files
 * should not be directly edited).
 * 
 * The most relevant packages are:
 * 
 * org.python.pydev.parser.jython.ast 
 * ----------------------------------
 * 
 * This package contains the AST (Abstract Syntax Tree) structure used by Pydev. It implements a
 * visitor pattern so that clients can traverse the structure.
 * 
 * All the grammars generate the same AST. This means it must remain compatible with all versions of
 * Python -- that isn't much a problem as new features arrive, as it'd be just a matter of creating
 * new AST nodes which will only exist on certain versions and not others or simply creating new
 * fields in the nodes.
 * 
 * E.g.: The 'with' construct was introduced on Python 2.5, so, there is a 'With' AST node but the
 * Python 2.4 grammar doesn't generate it.
 * 
 * To change this structure, the file to be changed is Python.asdl and adsl_java.py should be run
 * after it's changed (with the Python.asdl file as a parameter) so that the nodes are regenerated
 * (and the asdl_java.py is also the place to be edited if more features are needed in the nodes
 * structure).
 * 
 * 
 * org.python.pydev.parser.grammarXX 
 * ---------------------------------
 * 
 * Each of the grammar packages provides the specific implementation for a grammar. Note that the
 * PythonGrammarXXXXX classes are all automatically generated.
 * 
 * The TreeBuilder class is responsible for actually generating the nodes and there's a base class
 * to reuse code among many tree builders.
 * 
 * The python.jjt file is also automatically generated from the python.jjt_template file. To
 * generate it, run the make_replace.py file at the org.python.pydev.parser.grammarcommon package.
 * 
 * If you want to generate the files only for this grammar (usually while testing), the ant
 * build.xml can be used. To regenerate for all the grammars at the same time, use the ant
 * build_all.xml (at the org.python.pydev.parser.grammarcommon package).
 * 
 * 
 * org.python.pydev.parser.grammarcommon 
 * -------------------------------------
 * 
 * This package contains the classes that are common among all the grammars and provides a
 * make_replace.py to generate the python.jjt files and an ant build_all.xml to regenerate all the
 * PythonGrammar classes (note that ant build_all.xml doesn't call the make_replace.py)
 * 
 * The make_replace.py can be edited to provide constructs that are common among more than 1
 * grammar.
 * 
 * 
 * Important 
 * ---------
 * 
 * One thing essential for code to get into pydev is that it has to be properly tested. For examples
 * on tests for the grammar see the PyParserXXTest classes under tests/org.python.pydev.parser.
 * 
 * Notes 
 * ------
 * 
 * Note that the grammar is a fork of the Jython structure, but it has a number of changes to
 * support the features needed in Pydev:
 * 
 * Major:
 * 
 * - Provides a way to get comments and other tokens (specialsBefore and specialsAfter in
 * SimpleNode, containing comments and other syntax tokens such as 'if', 'with', etc... which are
 * needed for a more accurate pretty-printing process)
 * 
 * - Faster (there were lots of optimizations in this area, such as a faster char stream, use of
 * FastStringBuffer instead of StringBuffer, declaration of some classes as final, no creation of
 * intermediary nodes, less memory use using a pool of strings during the parse, etc).
 * 
 * - Structure changed so that multiple grammars can reuse the code that creates the tree structure
 * and the grammar itself
 * 
 * - Error handling is done in the grammar so that an AST can be properly generated even in the face
 * of errors (although it's not 100% fail-proof).
 */
public class HowToWorkWithGrammar {

}
