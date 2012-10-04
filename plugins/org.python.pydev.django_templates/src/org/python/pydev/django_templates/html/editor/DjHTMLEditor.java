/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.django_templates.html.editor;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.django_templates.DjPlugin;
import org.python.pydev.django_templates.editor.DjEditor;
import org.python.pydev.django_templates.html.outline.DjHTMLOutlineContentProvider;
import org.python.pydev.django_templates.html.outline.DjHTMLOutlineLabelProvider;
import org.python.pydev.utils.ICallback;

import com.aptana.editor.html.HTMLEditor;
import com.aptana.editor.html.IHTMLConstants;

/**
 * @author Fabio Zadrozny
 */
public class DjHTMLEditor extends HTMLEditor {

    private DjEditor djEditor;

    /*
     * (non-Javadoc)
     * @see com.aptana.editor.common.AbstractThemeableEditor#initializeEditor()
     */
    @Override
    protected void initializeEditor() {
        super.initializeEditor();
        djEditor = new DjEditor();
        this.djEditor.registerPrefChangeListener(new ICallback() {

            public Object call(Object args) throws Exception {
                return getISourceViewer();
            }
        });

        setSourceViewerConfiguration(new DjHTMLSourceViewerConfiguration(this.djEditor.getChainedPrefStore(), this));
        setDocumentProvider(DjPlugin.getDefault().getDjHTMLDocumentProvider());
    }

    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        ISourceViewer viewer = super.createSourceViewer(parent, ruler, styles);
        djEditor.onCreateSourceViewer(viewer);
        return viewer;
    }

    @Override
    public void dispose() {
        super.dispose();
        djEditor.dispose();
    }

    @Override
    public String getContentType() {
        return IHTMLConstants.CONTENT_TYPE_HTML;
    }

    @Override
    public ITreeContentProvider getOutlineContentProvider() {
        return new DjHTMLOutlineContentProvider(this);
    }

    @Override
    public ILabelProvider getOutlineLabelProvider() {
        return new DjHTMLOutlineLabelProvider(getDocument());
    }

    @Override
    public char[] getPairMatchingCharacters() {
        return this.djEditor.getPairMatchingCharacters(super.getPairMatchingCharacters());
    }
}
