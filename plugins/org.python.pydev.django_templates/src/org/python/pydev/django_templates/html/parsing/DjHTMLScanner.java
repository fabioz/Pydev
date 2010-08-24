package org.python.pydev.django_templates.html.parsing;

import org.python.pydev.django_templates.comon.parsing.DjScanner;

import com.aptana.editor.html.parsing.HTMLScanner;

public class DjHTMLScanner extends HTMLScanner {

    private DjScanner djScanner = new DjScanner();
    
    public DjHTMLScanner() {
        super(new DjHtmlTokenScanner(), DjScanner.SWITCH_STRATEGY);
    }

    public short getTokenType(Object data) {
        Short tokenType = djScanner.getTokenType(this, data);
        if(tokenType != null){
            return tokenType;
        }
        return super.getTokenType(data);
    }
}
