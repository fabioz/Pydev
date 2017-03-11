package org.python.pydev.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class PyDecoratorRule implements IRule {

    private IToken decoratorToken;

    public PyDecoratorRule(IToken decoratorToken) {
        this.decoratorToken = decoratorToken;
    }

    public boolean isWordPart(int c) {
        return c != '\n' && c != '\r' && c != '(' && c != -1;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        if (c != '@') {
            scanner.unread();
            return Token.UNDEFINED;
        }
        PyCodeScanner codeScanner = (PyCodeScanner) scanner;
        String currLineContents;
        try {
            int diffOffset = -1; // we don't want to get the current '@'
            currLineContents = codeScanner.getLineContentsToCursor(diffOffset);
        } catch (BadLocationException e) {
            scanner.unread();
            return Token.UNDEFINED;
        }
        if (currLineContents.trim().length() == 0) {
            // Ok, it's a decorator
            do {
                c = scanner.read();
            } while (isWordPart(c));
            scanner.unread();
            return this.decoratorToken;
        }
        scanner.unread();
        return Token.UNDEFINED;
    }

}
