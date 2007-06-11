package org.python.pydev.refactoring.tests.core;

import org.python.pydev.refactoring.ast.rewriter.RewriterVisitor;

public abstract class AbstractRewriterTestCase extends AbstractIOTestCase {

	public AbstractRewriterTestCase(String name) {
		this(name, false);
	}

	public AbstractRewriterTestCase(String name, boolean ignoreEmptyLines) {
		super(name, ignoreEmptyLines);
	}

	protected void runRewriter() throws Throwable {
		setTestGenerated(RewriterVisitor.reparsed(getSource(), "\n"));
	}

}
