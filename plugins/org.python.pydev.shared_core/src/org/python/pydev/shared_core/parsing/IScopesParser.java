package org.python.pydev.shared_core.parsing;

import org.eclipse.jface.text.IDocument;

public interface IScopesParser {

    public Scopes createScopes(IDocument doc);
}
