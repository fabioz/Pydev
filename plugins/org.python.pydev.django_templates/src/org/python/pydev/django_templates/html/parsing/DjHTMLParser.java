package org.python.pydev.django_templates.html.parsing;

import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.comon.parsing.DjParser;

import com.aptana.editor.html.parsing.IHTMLParserConstants;

public class DjHTMLParser extends DjParser {

    public DjHTMLParser() {
        super(new DjHTMLParserScanner(), IHTMLParserConstants.LANGUAGE, IDjConstants.LANGUAGE_DJANGO_TEMPLATES_HTML);
    }

}
