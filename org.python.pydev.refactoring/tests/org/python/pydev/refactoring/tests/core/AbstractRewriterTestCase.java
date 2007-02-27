package org.python.pydev.refactoring.tests.core;

import java.io.StringWriter;

import org.python.pydev.refactoring.ast.visitors.VisitorFactory;

public abstract class AbstractRewriterTestCase extends AbstractIOTestCase {

	public AbstractRewriterTestCase(String name) {
		this(name, false);
	}

	public AbstractRewriterTestCase(String name, boolean ignoreEmptyLines) {
		super(name, ignoreEmptyLines);
	}

	protected void runRewriter() throws Throwable {
		StringWriter out = new StringWriter();
		VisitorFactory.createRewriterVisitor(out, getSource());
		setTestGenerated(out.getBuffer().toString());
	}

}
