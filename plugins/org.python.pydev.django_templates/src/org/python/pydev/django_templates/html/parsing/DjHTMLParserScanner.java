package org.python.pydev.django_templates.html.parsing;

import com.aptana.editor.html.parsing.HTMLParserScanner;

public class DjHTMLParserScanner extends HTMLParserScanner
{

	public DjHTMLParserScanner()
	{
		super(new DjHTMLScanner());
	}
}
