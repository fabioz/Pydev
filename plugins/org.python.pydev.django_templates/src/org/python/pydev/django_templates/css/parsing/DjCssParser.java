package org.python.pydev.django_templates.css.parsing;

import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.comon.parsing.DjParser;

import com.aptana.editor.css.parsing.ICSSParserConstants;

public class DjCssParser extends DjParser{

    public DjCssParser() {
        super(new DjCssParserScanner(), ICSSParserConstants.LANGUAGE, IDjConstants.LANGUAGE_DJANGO_TEMPLATES_CSS);
    }

}
