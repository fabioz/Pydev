package org.python.pydev.django_templates.common;

import org.python.copiedfromeclipsesrc.PythonPairMatcher;
import org.python.pydev.editor.PyDoubleClickStrategy;

public class DjDoubleClickStrategy extends PyDoubleClickStrategy{

    public static final char[] BRACKETS = { '{', '}', '(', ')', '[', ']', '<', '>' };
    
    public DjDoubleClickStrategy(String contentType) {
        super(contentType);
        fPairMatcher = new PythonPairMatcher(BRACKETS);
    }

}
