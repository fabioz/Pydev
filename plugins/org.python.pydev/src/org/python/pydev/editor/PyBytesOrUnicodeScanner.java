/******************************************************************************
* Copyright (C) 2015  Brainwy Software Ltda.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.editor;

import java.lang.ref.WeakReference;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.ui.ColorAndStyleCache;

public class PyBytesOrUnicodeScanner extends AbstractStringScanner {

    private WeakReference<PyPresentationReconciler> reconciler;

    public PyBytesOrUnicodeScanner(ColorAndStyleCache colorCache, IGrammarVersionProvider grammarVersionProvider,
            PyPresentationReconciler reconciler) {
        super(colorCache);
        this.setGrammarVersionProvider(grammarVersionProvider);
        this.reconciler = new WeakReference<>(reconciler);
    }

    @Override
    public void updateColorAndStyle() {
        if (defaultIsUnicode) {
            fStringReturnToken = new Token(colorCache.getUnicodeTextAttribute());
        } else {
            fStringReturnToken = new Token(colorCache.getStringTextAttribute());
        }
        fDocStringMarkupTextReturnToken = new Token(colorCache.getDocstringMarkupTextAttribute());
    }

    private IGrammarVersionProvider grammarVersionProvider;
    private boolean hasFromFutureImportUnicode = false;
    private boolean defaultIsUnicode = false;
    private IDocument fDocument;

    private boolean setDefaultIsUnicode(boolean defaultIsUnicode) {
        if (defaultIsUnicode != this.defaultIsUnicode) {
            this.defaultIsUnicode = defaultIsUnicode;
            this.updateColorAndStyle();
            PyPresentationReconciler r = this.reconciler.get();
            if (r != null) {
                r.invalidateTextPresentation();
            }
            return true;
        }
        return false;
    }

    public void setGrammarVersionProvider(IGrammarVersionProvider grammarVersionProvider) {
        if (grammarVersionProvider != null) {
            this.grammarVersionProvider = grammarVersionProvider;
        }
    }

    /**
     * Returns whether the setting changed.
     */
    private void setFromFutureImportUnicode(boolean hasFromFutureImportUnicode) {
        if (this.hasFromFutureImportUnicode != hasFromFutureImportUnicode) {
            this.hasFromFutureImportUnicode = hasFromFutureImportUnicode;
        }
    }

    private boolean updateDefaultIsBytesOrUnicode() {
        if (hasFromFutureImportUnicode) {
            return setDefaultIsUnicode(true);
        }
        int grammarVersion = IGrammarVersionProvider.LATEST_GRAMMAR_VERSION;
        IGrammarVersionProvider g = grammarVersionProvider;
        if (g != null) {
            try {
                grammarVersion = g.getGrammarVersion();
            } catch (MisconfigurationException e) {
            }
        }
        if (grammarVersion >= IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0) {
            return setDefaultIsUnicode(true);
        } else {
            return setDefaultIsUnicode(false);
        }
    }

    private long last = 0;

    @Override
    public void setRange(IDocument document, int offset, int length) {
        if (this.fDocument != document) {
            this.fDocument = document;
            last = System.currentTimeMillis();
            this.updateFutureUnicodeFromDocument();
        } else {
            long curr = System.currentTimeMillis();
            if (curr - last > 1000) { //Check at most one time/second
                last = curr;
                updateFutureUnicodeFromDocument();
            }
        }
        super.setRange(document, offset, length);
    }

    private void updateFutureUnicodeFromDocument() {
        if (this.fDocument != null) {
            this.setFromFutureImportUnicode(PySelection.hasFromFutureImportUnicode(this.fDocument));
        }
        //The grammar version or unicode literals could've changed...
        updateDefaultIsBytesOrUnicode();
    }
}
