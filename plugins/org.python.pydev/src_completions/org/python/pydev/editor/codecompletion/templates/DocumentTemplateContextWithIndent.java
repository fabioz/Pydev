/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.templates;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.shared_ui.templates.AbstractDocumentTemplateContextWithIndent;

public class DocumentTemplateContextWithIndent extends AbstractDocumentTemplateContextWithIndent {

    private final IIndentPrefs indentPrefs;

    /**
     * Note that it's in the default context because it should be used on subclasses.
     */
    protected DocumentTemplateContextWithIndent(TemplateContextType type, IDocument document, int offset,
            int length, String indentTo, IIndentPrefs indentPrefs) {
        super(type, document, offset, length, indentTo);
        this.indentPrefs = indentPrefs;
    }

    @Override
    protected int getTabWidth() {
        return indentPrefs.getTabWidth();
    }

    @Override
    protected boolean getUseSpaces() {
        return indentPrefs.getUseSpaces(true);
    }

}
