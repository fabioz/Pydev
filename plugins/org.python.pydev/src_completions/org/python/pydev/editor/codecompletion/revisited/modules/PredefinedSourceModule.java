package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;

import org.python.pydev.parser.jython.SimpleNode;

public class PredefinedSourceModule extends SourceModule{

	public PredefinedSourceModule(String name, File f, SimpleNode n, Throwable parseError) {
		super(name, f, n, parseError);
	}

}
