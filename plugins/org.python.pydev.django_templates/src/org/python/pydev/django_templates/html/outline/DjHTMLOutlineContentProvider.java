package org.python.pydev.django_templates.html.outline;

import com.aptana.editor.html.outline.HTMLOutlineContentProvider;
import com.aptana.editor.ruby.outline.RubyOutlineContentProvider;
import com.aptana.editor.ruby.parsing.IRubyParserConstants;

public class DjHTMLOutlineContentProvider extends HTMLOutlineContentProvider
{
	public DjHTMLOutlineContentProvider()
	{
		addSubLanguage(IRubyParserConstants.LANGUAGE, new RubyOutlineContentProvider());
	}
}
