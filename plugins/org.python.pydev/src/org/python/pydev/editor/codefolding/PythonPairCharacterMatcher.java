package org.python.pydev.editor.codefolding;

import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.python.pydev.core.docutils.PythonPairMatcher;

public class PythonPairCharacterMatcher extends PythonPairMatcher implements ICharacterPairMatcher {

    public PythonPairCharacterMatcher(char[] brackets) {
        super(brackets);
    }

}
