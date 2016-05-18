/**
 * Copyright (c) 2016 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.hover;

import java.io.IOException;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PyInformationPresenter;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.parser.visitors.NodeUtils;

public abstract class AbstractPyEditorTextHover implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

    /**
     * The text selected
     */
    protected ITextSelection textSelection;
    protected PyInformationPresenter informationPresenter;
    protected PyInformationControl informationControl;
    protected Integer hoverControlPreferredWidth;
    protected Integer hoverControlWidth = null;

    public final class PyInformationControl extends DefaultInformationControl
            implements IInformationControlExtension3 {
        private PyInformationControl(Shell parent, String statusFieldText,
                IInformationPresenter presenter) {
            super(parent, statusFieldText, presenter);
        }

    }

    public AbstractPyEditorTextHover() {
        informationPresenter = new PyInformationPresenter();
    }

    /**
     * Specifies whether a given content type is supported for this Hover
     * @param contentType the content type
     * @return whether hover info should be rendered for this content type
     */
    public abstract boolean isContentTypeSupported(String contentType);

    /*
     * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
     */
    @Override
    public IInformationControlCreator getHoverControlCreator() {
        return new IInformationControlCreator() {

            @Override
            public IInformationControl createInformationControl(Shell parent) {
                String tooltipAffordanceString = null;
                try {
                    tooltipAffordanceString = EditorsUI.getTooltipAffordanceString();
                } catch (Throwable e) {
                    //Not available on Eclipse 3.2
                }
                informationControl = new PyInformationControl(parent, tooltipAffordanceString,
                        informationPresenter);
                return informationControl;
            }
        };
    }

    /*
     * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
     */
    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        //we have to set it here (otherwise we don't have thread access to the UI)
        this.textSelection = (ITextSelection) textViewer.getSelectionProvider().getSelection();
        return new Region(offset, 0);
    }

    /**
     * Copied from {@link PyTextHover} when that class was deprecated.
     */
    public static String printAst(PyEdit edit, SimpleNode astToPrint) {
        String str = null;
        if (astToPrint != null) {
            IIndentPrefs indentPrefs;
            if (edit != null) {
                indentPrefs = edit.getIndentPrefs();
            } else {
                indentPrefs = DefaultIndentPrefs.get(null);
            }

            Str docStr = NodeUtils.getNodeDocStringNode(astToPrint);
            if (docStr != null) {
                docStr.s = PyStringUtils.fixWhitespaceColumnsToLeftFromDocstring(docStr.s,
                        indentPrefs.getIndentationString());
            }

            PrettyPrinterPrefsV2 prefsV2 = PrettyPrinterV2.createDefaultPrefs(edit, indentPrefs,
                    PyInformationPresenter.LINE_DELIM);

            PrettyPrinterV2 prettyPrinterV2 = new PrettyPrinterV2(prefsV2);
            try {

                str = prettyPrinterV2.print(astToPrint);
            } catch (IOException e) {
                Log.log(e);
            }
        }
        return str;
    }

    /*
     * @see org.eclipse.jface.text.ITextHoverExtension2#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
     * @since 3.4
     */
    @Override
    @SuppressWarnings("deprecation")
    public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
        return getHoverInfo(textViewer, hoverRegion);
    }

    /**
     * Add a listener to resize events on the information presented control.
     * @param listener the resize listener
     */
    public void addInformationPresenterControlListener(ControlListener listener) {
        if (informationPresenter != null) {
            informationPresenter.addResizeCallback(listener);
        }
    }

    /**
     * Set the preferred width of the Hover control.
     * @param width the preferred width
     */
    public void setHoverControlPreferredWidth(int width) {
        this.hoverControlPreferredWidth = width;
    }

    /**
     * Get the preferred width of the Hover control.
     * @return the preferred width
     */
    public Integer getHoverControlPreferredWidth() {
        return this.hoverControlPreferredWidth;
    }

}
