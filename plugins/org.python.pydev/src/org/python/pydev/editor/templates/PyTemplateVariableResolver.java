package org.python.pydev.editor.templates;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

public abstract class PyTemplateVariableResolver extends TemplateVariableResolver {

	/*
	 * @see TemplateVariableResolver#TemplateVariableResolver(String, String)
	 */
	protected PyTemplateVariableResolver(String type, String description) {
		super(type, description);
	}

	/*
	 * @see TemplateVariableResolver#evaluate(TemplateContext)
	 */
	protected String resolve(TemplateContext context) {
		return evaluateContext(context);
	}

	/**
	 * Created to be overridden in Jython (we should override only resolve, but as there are
	 * 2 variations of resolve, the overridden class doesn't properly know which one should 
	 * be called, so, this is to disambiguate that).
	 */
	public abstract String evaluateContext(TemplateContext context);

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