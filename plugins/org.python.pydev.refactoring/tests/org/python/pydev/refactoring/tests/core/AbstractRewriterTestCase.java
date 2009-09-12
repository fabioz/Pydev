/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.core;

import org.python.pydev.refactoring.ast.visitors.rewriter.RewriterVisitor;

public abstract class AbstractRewriterTestCase extends AbstractIOTestCase {

	public AbstractRewriterTestCase(String name) {
		this(name, false);
	}

	public AbstractRewriterTestCase(String name, boolean ignoreEmptyLines) {
		super(name, ignoreEmptyLines);
	}

	protected void runRewriter() throws Throwable {
		setTestGenerated(RewriterVisitor.reparsed(data.source, "\n"));
	}

}
