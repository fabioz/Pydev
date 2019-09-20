package org.python.pydev.editor;

import java.util.HashSet;
import java.util.Set;

import org.python.pydev.shared_core.partitioner.ICharacterScanner;
import org.python.pydev.shared_core.partitioner.IToken;
import org.python.pydev.shared_core.partitioner.IWordDetector;

public class PyIdentifierRule extends PyWordRule {

    private String lastTokenStr;
    private Set<String> potentialVariables;

    private IToken variableToken;
    private IToken propertyToken;

    public PyIdentifierRule(IWordDetector detector, IToken defaultToken, IToken classNameToken, IToken funcNameToken,
            IToken parensToken, IToken operatorsToken, IToken variableToken, IToken propertyToken) {
        super(detector, defaultToken, classNameToken, funcNameToken, parensToken, operatorsToken);

        this.lastTokenStr = "";
        this.potentialVariables = new HashSet<String>();

        this.variableToken = variableToken;
        this.propertyToken = propertyToken;
    }

    @Override
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();

        switch (c) {
            case '=':
                if (lastTokenStr.length() > 0 && !lastTokenStr.equals(".")) {
                    potentialVariables.add(lastTokenStr);
                }
                break;
            case '.':
                lastTokenStr = ".";
                break;
        }
        scanner.unread();

        IToken result = super.evaluate(scanner);
        String currentTokenStr = fBuffer.toString();

        if (result == this.fDefaultToken && !this.fDefaultToken.isUndefined()) {
            if (lastTokenStr.equals(".") &&
                    !(lastFound.equals("import") || lastFound.equals("from"))) {
                result = propertyToken;
            } else if (lastTokenStr.equals("import")) {
                potentialVariables.add(currentTokenStr);
            } else if (potentialVariables.contains(currentTokenStr)) {
                result = variableToken;
            }
            lastTokenStr = currentTokenStr;
        } else if (currentTokenStr.equals("import")) {
            lastTokenStr = currentTokenStr;
        }

        return result;
    }
}
