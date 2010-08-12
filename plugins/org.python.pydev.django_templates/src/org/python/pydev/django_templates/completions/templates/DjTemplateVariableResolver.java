package org.python.pydev.django_templates.completions.templates;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

public class DjTemplateVariableResolver extends TemplateVariableResolver {

	private String[] options;


    /*
	 * @see TemplateVariableResolver#TemplateVariableResolver(String, String)
	 */
	protected DjTemplateVariableResolver(String type, String description, String[] options) {
		super(type, description);
		this.options = options;
	}
	
	protected String[] resolveAll(TemplateContext context) {
	    return options;
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