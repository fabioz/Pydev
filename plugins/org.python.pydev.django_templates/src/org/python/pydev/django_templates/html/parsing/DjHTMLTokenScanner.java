package org.python.pydev.django_templates.html.parsing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

import org.python.pydev.django_templates.parsing.lexer.DjangoTemplatesTokens;
import com.aptana.editor.html.parsing.HTMLTokenScanner;

public class DjHTMLTokenScanner extends HTMLTokenScanner {

    @SuppressWarnings("nls")
    private static final String[] DJHTML_START = { "{%", "{{" };
    
    @SuppressWarnings("nls")
    private static final String[] DJHTML_END = new String[] { "%}", "}}" };

    public DjHTMLTokenScanner() {
        List<IRule> rules = new ArrayList<IRule>();
        // adds rules for finding the django templates start and end sequences
        WordRule wordRule = new WordRule(new DjStartDetector(), Token.UNDEFINED);
        IToken token = createToken(getTokenName(DjangoTemplatesTokens.DJHTML_START));
        for (String word : DJHTML_START) {
            wordRule.addWord(word, token);
        }
        rules.add(wordRule);
        wordRule = new WordRule(new DjEndDetector(), Token.UNDEFINED);
        token = createToken(getTokenName(DjangoTemplatesTokens.DJHTML_END));
        for (String word : DJHTML_END) {
            wordRule.addWord(word, token);
        }
        rules.add(wordRule);

        for (IRule rule : fRules) {
            rules.add(rule);
        }

        setRules(rules.toArray(new IRule[rules.size()]));
    }

    private static String getTokenName(short token) {
        return DjangoTemplatesTokens.getTokenName(token);
    }

    private static final class DjStartDetector implements IWordDetector {

        @Override
        public boolean isWordPart(char c) {
            switch (c) {
            case '{':
            case '%':
                return true;
            }
            return false;
        }

        @Override
        public boolean isWordStart(char c) {
            return c == '{';
        }
    }

    private static final class DjEndDetector implements IWordDetector {

        @Override
        public boolean isWordPart(char c) {
            switch (c) {
            case '%':
            case '}':
                return true;
            }
            return false;
        }

        @Override
        public boolean isWordStart(char c) {
            return c == '}' || c == '%';
        }
    }
}
