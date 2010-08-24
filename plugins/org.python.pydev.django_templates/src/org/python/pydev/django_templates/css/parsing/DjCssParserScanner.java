package org.python.pydev.django_templates.css.parsing;

import com.aptana.editor.common.parsing.CompositeParserScanner;

public class DjCssParserScanner extends CompositeParserScanner{

    public DjCssParserScanner() {
        super(new DjCssScanner());
    }


}
