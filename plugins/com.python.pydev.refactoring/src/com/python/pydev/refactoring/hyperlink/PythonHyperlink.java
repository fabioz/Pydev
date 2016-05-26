/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.hyperlink;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.python.pydev.editor.PyEdit;

import com.python.pydev.refactoring.actions.PyGoToDefinition;

/**
 * Hiperlink will try to open the current selected word.
 *
 * @author Fabio
 */
public class PythonHyperlink implements IHyperlink {

    private final IRegion fRegion;
    private PyEdit fEditor;

    public PythonHyperlink(IRegion region, PyEdit editor) {
        Assert.isNotNull(region);
        fRegion = region;
        fEditor = editor;
    }

    @Override
    public IRegion getHyperlinkRegion() {
        return fRegion;
    }

    @Override
    public String getHyperlinkText() {
        return "Go To Definition";
    }

    @Override
    public String getTypeLabel() {
        return null;
    }

    /**
     * Try to find a definition and open it.
     */
    @Override
    public void open() {
        PyGoToDefinition pyGoToDefinition = new PyGoToDefinition();
        pyGoToDefinition.setEditor(this.fEditor);
        pyGoToDefinition.run((IAction) null);
    }

}
