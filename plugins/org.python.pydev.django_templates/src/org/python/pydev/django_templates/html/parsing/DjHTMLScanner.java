package org.python.pydev.django_templates.html.parsing;

import com.aptana.editor.common.parsing.IScannerSwitchStrategy;
import com.aptana.editor.common.parsing.ScannerSwitchStrategy;
import org.python.pydev.django_templates.parsing.lexer.DjangoTemplatesTokens;
import com.aptana.editor.html.parsing.HTMLScanner;

public class DjHTMLScanner extends HTMLScanner {

    private static final String[] DJHTML_ENTER_TOKENS = new String[] { DjangoTemplatesTokens.getTokenName(DjangoTemplatesTokens.DJHTML_START) };
    private static final String[] DJHTML_EXIT_TOKENS = new String[] { DjangoTemplatesTokens.getTokenName(DjangoTemplatesTokens.DJHTML_END) };

    private static final IScannerSwitchStrategy DJHTML_STRATEGY = new ScannerSwitchStrategy(DJHTML_ENTER_TOKENS, DJHTML_EXIT_TOKENS);

    private boolean isInDjHtml;

    public DjHTMLScanner() {
        super(new DjHTMLTokenScanner(), new IScannerSwitchStrategy[] { DJHTML_STRATEGY });
    }

    public short getTokenType(Object data) {
        IScannerSwitchStrategy strategy = getCurrentSwitchStrategy();
        if (strategy == DJHTML_STRATEGY) {
            if (!isInDjHtml) {
                isInDjHtml = true;
            }
            return DjangoTemplatesTokens.DJHTML_START;
        }
        if (strategy == null && isInDjHtml) {
            isInDjHtml = false;
            return DjangoTemplatesTokens.DJHTML_END;
        }
        return super.getTokenType(data);
    }
}
