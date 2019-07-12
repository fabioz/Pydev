/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.templates;

import java.io.File;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPySourceViewer;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.interactive_console.IScriptConsoleViewer;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * Makes a custom evaluation of the template buffer to be created (to put it in the correct indentation and
 * change tabs to spaces -- if needed).
 *
 * @author Fabio
 */
public final class PyDocumentTemplateContext extends DocumentTemplateContextWithIndent {

    public ITextViewer viewer; //May be null

    /**
     * Note that it's in the default context because it should be used on subclasses.
     */
    /*default*/ PyDocumentTemplateContext(TemplateContextType type, IDocument document, int offset, int length,
            String indentTo, IIndentPrefs indentPrefs) {
        super(type, document, offset, length, indentTo, indentPrefs);
    }

    public PyDocumentTemplateContext(TemplateContextType type, IDocument document, int offset, int length,
            String indentTo, ITextViewer viewer) {
        this(type, document, offset, length, indentTo, getIndentPrefs(viewer));
        this.viewer = viewer;
    }

    // Methods below are Used in scripting

    public PySelection createPySelection() {
        return new PySelection(getDocument(), getStart());
    }

    public Class<FastParser> getFastParserClass() {
        return FastParser.class;
    }

    public Class<NodeUtils> getNodeUtilsClass() {
        return NodeUtils.class;
    }

    public Class<FunctionDef> getFunctionDefClass() {
        return FunctionDef.class;
    }

    public Class<ClassDef> getClassDefClass() {
        return ClassDef.class;
    }

    public Class<BadLocationException> getBadLocationExceptionClass() {
        return BadLocationException.class;
    }

    public boolean isCythonFile() {
        if (this.viewer instanceof IPySourceViewer) {
            return ((IPySourceViewer) this.viewer).getEdit().isCythonFile();
        }
        return false;
    }

    public File getEditorFile() {
        if (this.viewer instanceof IPySourceViewer) {
            return ((IPySourceViewer) this.viewer).getEdit().getEditorFile();
        }
        return new File("");
    }

    public int getGrammarVersion() {
        //Other possibilities
        //org.eclipse.jface.text.source.SourceViewer (in compare)

        if (this.viewer instanceof IPySourceViewer) {
            try {
                IPythonNature nature = ((IPySourceViewer) this.viewer).getEdit().getPythonNature();
                if (nature != null) {
                    return nature.getGrammarVersion();
                }
            } catch (MisconfigurationException e) {
            }
        }

        if (this.viewer instanceof IScriptConsoleViewer) {
            //interactive console
            IScriptConsoleViewer v = (IScriptConsoleViewer) this.viewer;
            IInterpreterInfo interpreterInfo = (IInterpreterInfo) v.getInterpreterInfo();
            if (interpreterInfo != null) {
                return interpreterInfo.getGrammarVersion();
            }

        }
        return IGrammarVersionProvider.LATEST_GRAMMAR_PY3_VERSION;
    }

    public String getModuleName() {
        if (this.viewer instanceof IPySourceViewer) {
            try {
                IPySourceViewer pyViewer = (IPySourceViewer) this.viewer;
                IPyEdit edit = pyViewer.getEdit();
                IPythonNature nature = edit.getPythonNature();
                if (nature != null) {
                    return nature.resolveModule(edit.getEditorFile());
                }
            } catch (MisconfigurationException e) {
            }
        }
        return "";
    }

    /**
     * Creates a concrete template context for the given region in the document. This involves finding out which
     * context type is valid at the given location, and then creating a context of this type. The default implementation
     * returns a <code>DocumentTemplateContext</code> for the context type at the given location.
     *
     * @param contextType the context type for the template.
     * @param viewer the viewer for which the context is created
     * @param region the region into <code>document</code> for which the context is created
     * @return a template context that can handle template insertion at the given location, or <code>null</code>
     */
    public static PyDocumentTemplateContext createContext(final TemplateContextType contextType,
            final ITextViewer viewer, final IRegion region, String indent) {
        if (contextType != null) {
            IDocument document = viewer.getDocument();
            final String indentTo = indent;
            return new PyDocumentTemplateContext(contextType, document, region.getOffset(), region.getLength(),
                    indentTo, viewer);
        }
        return null;
    }

    public static PyDocumentTemplateContext createContext(final TemplateContextType contextType,
            final ITextViewer viewer, final IRegion region) {
        if (contextType != null) {
            IDocument document = viewer.getDocument();
            PySelection selection = new PySelection(document,
                    ((ITextSelection) viewer.getSelectionProvider().getSelection()).getOffset());
            String indent = selection.getIndentationFromLine();
            return PyDocumentTemplateContext.createContext(contextType, viewer, region, indent);
        }
        return null;
    }

    /**
     * @return the indent preferences to be used.
     */
    private static IIndentPrefs getIndentPrefs(ITextViewer viewer) {
        if (viewer instanceof IPySourceViewer) {
            IPySourceViewer pyViewer = (IPySourceViewer) viewer;
            return pyViewer.getEdit().getIndentPrefs();
        } else {
            return DefaultIndentPrefs.get(null);
        }
    }

}