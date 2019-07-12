/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: Scott Schlesier
 * Created: March 5, 2005
 */
package org.python.pydev.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.TextAttribute;
import org.python.pydev.plugin.preferences.PyDevEditorPreferences;
import org.python.pydev.shared_core.partitioner.RuleBasedScanner;
import org.python.pydev.shared_core.partitioner.Token;
import org.python.pydev.ui.ColorAndStyleCache;

/**
 *
 * PyColoredScanner is a simple modification to RuleBasedScanner
 * that supports updating the defaultToken color based on a named
 * color in the colorCache
 */
public class PyColoredScanner extends RuleBasedScanner {
    private ColorAndStyleCache colorCache;
    private String name;

    public PyColoredScanner(ColorAndStyleCache colorCache, String name) {
        super();
        Assert.isNotNull(colorCache);
        this.colorCache = colorCache;
        this.name = name;
        updateColorAndStyle();
    }

    public void updateColorAndStyle() {
        TextAttribute attr;
        if (PyDevEditorPreferences.COMMENT_COLOR.equals(name)) {
            attr = colorCache.getCommentTextAttribute();

        } else if (PyDevEditorPreferences.BACKQUOTES_COLOR.equals(name)) {
            attr = colorCache.getBackquotesTextAttribute();

        } else if (PyDevEditorPreferences.STRING_COLOR.equals(name)) {
            attr = colorCache.getStringTextAttribute();

        } else if (PyDevEditorPreferences.UNICODE_COLOR.equals(name)) {
            attr = colorCache.getUnicodeTextAttribute();

        } else {
            throw new RuntimeException("Unexpected: " + name);
        }
        setDefaultReturnToken(new Token(attr));
    }

}
