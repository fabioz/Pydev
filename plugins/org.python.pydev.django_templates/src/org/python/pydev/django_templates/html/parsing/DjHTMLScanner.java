package org.python.pydev.django_templates.html.parsing;

import com.aptana.editor.common.parsing.IScannerSwitchStrategy;
import com.aptana.editor.common.parsing.ScannerSwitchStrategy;
import org.python.pydev.django_templates.parsing.lexer.DjangoTemplatesTokens;
import com.aptana.editor.html.parsing.HTMLScanner;

public class DjHTMLScanner extends HTMLScanner {

    private static final String[] RUBY_ENTER_TOKENS = new String[] { DjangoTemplatesTokens.getTokenName(DjangoTemplatesTokens.RUBY) };
    private static final String[] RUBY_EXIT_TOKENS = new String[] { DjangoTemplatesTokens.getTokenName(DjangoTemplatesTokens.RUBY_END) };

    private static final IScannerSwitchStrategy RUBY_STRATEGY = new ScannerSwitchStrategy(RUBY_ENTER_TOKENS, RUBY_EXIT_TOKENS);

    private boolean isInRuby;

    public DjHTMLScanner() {
        super(new DjHTMLTokenScanner(), new IScannerSwitchStrategy[] { RUBY_STRATEGY });
    }

    public short getTokenType(Object data) {
        IScannerSwitchStrategy strategy = getCurrentSwitchStrategy();
        if (strategy == RUBY_STRATEGY) {
            if (!isInRuby) {
                isInRuby = true;
            }
            return DjangoTemplatesTokens.RUBY;
        }
        if (strategy == null && isInRuby) {
            isInRuby = false;
            return DjangoTemplatesTokens.RUBY_END;
        }
        return super.getTokenType(data);
    }
}
