/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.django_templates.html.editor;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.django_templates.DjPlugin;
import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.editor.DjEditor;
import org.python.pydev.django_templates.html.outline.DjHTMLOutlineContentProvider;
import org.python.pydev.django_templates.html.outline.DjHTMLOutlineLabelProvider;
import org.python.pydev.utils.ICallback;

import com.aptana.editor.common.outline.CommonOutlinePage;
import com.aptana.editor.common.parsing.FileService;
import com.aptana.editor.common.validator.IValidationManager;
import com.aptana.editor.css.ICSSConstants;
import com.aptana.editor.html.HTMLEditor;
import com.aptana.editor.html.IHTMLConstants;
import com.aptana.editor.html.parsing.HTMLParseState;
import com.aptana.editor.js.IJSConstants;

/**
 * @author Fabio Zadrozny
 */
public class DjHTMLEditor extends HTMLEditor {

    private DjEditor djEditor;
    
    /*
     * (non-Javadoc)
     * 
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
    protected FileService createFileService()
    {
        FileService fileService = new FileService(IHTMLConstants.CONTENT_TYPE_HTML, new HTMLParseState());
        IValidationManager validationManager = fileService.getValidationManager();
        validationManager.addNestedLanguage(ICSSConstants.CONTENT_TYPE_CSS);
        validationManager.addNestedLanguage(IJSConstants.CONTENT_TYPE_JS);
        validationManager.addNestedLanguage(IDjConstants.CONTENT_TYPE_DJANGO_HTML);
        return fileService;
    }

    @Override
    protected CommonOutlinePage createOutlinePage() {
        CommonOutlinePage outline = super.createOutlinePage();
        outline.setContentProvider(new DjHTMLOutlineContentProvider());
        outline.setLabelProvider(new DjHTMLOutlineLabelProvider(getFileService().getParseState()));

        return outline;
    }

    @Override
    protected char[] getPairMatchingCharacters() {
        return this.djEditor.getPairMatchingCharacters(super.getPairMatchingCharacters());
    }
}
