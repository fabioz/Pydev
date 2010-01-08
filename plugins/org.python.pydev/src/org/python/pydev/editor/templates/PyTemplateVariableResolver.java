package org.python.pydev.editor.templates;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

public class PyTemplateVariableResolver extends TemplateVariableResolver {

	/*
	 * @see TemplateVariableResolver#TemplateVariableResolver(String, String)
	 */
	protected PyTemplateVariableResolver(String type, String description) {
		super(type, description);
	}


	/**
	 * Returns always <code>true</code>, since simple variables are normally
	 * unambiguous.
	 *
	 * @param context {@inheritDoc}
	 * @return <code>true</code>
	 */
	protected boolean isUnambiguous(TemplateContext context) {
		return true;
	}
}