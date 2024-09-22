/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.templates;

import java.io.File;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPySourceViewer;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISourceViewerForTemplates;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.interactive_console.IScriptConsoleViewer;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.ICoreTextSelection;

/**
 * Makes a custom evaluation of the template buffer to be created (to put it in the correct indentation and
 * change tabs to spaces -- if needed).
 *
 * @author Fabio
 */
public final class PyDocumentTemplateContext extends DocumentTemplateContextWithIndent {

    public ISourceViewerForTemplates edit; //May be null

    /**
     * This constructor is meant for tests!
     */
    public PyDocumentTemplateContext(TemplateContextType type, IDocument document, int offset, int length,
            String indentTo, IIndentPrefs indentPrefs) {
        super(type, document, offset, length, indentTo, indentPrefs);
    }

    public PyDocumentTemplateContext(TemplateContextType type, IDocument document, int offset, int length,
            String indentTo, ISourceViewerForTemplates viewer) {
        this(type, document, offset, length, indentTo, viewer.getIndentPrefs());
        this.edit = viewer;
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
        if (this.edit != null) {
            return edit.isCythonFile();
        }
        return false;
    }

    public File getEditorFile() {
        if (this.edit != null) {
            return this.edit.getEditorFile();
        }
        return new File("");
    }

    public int getGrammarVersion() {
        //Other possibilities
        //org.eclipse.jface.text.source.SourceViewer (in compare)

        if (this.edit instanceof IPySourceViewer) {
            try {
                IPythonNature nature = ((IPySourceViewer) this.edit).getEdit().getPythonNature();
                if (nature != null) {
                    return nature.getGrammarVersion();
                }
            } catch (MisconfigurationException e) {
            }
        }

        if (this.edit instanceof IScriptConsoleViewer) {
            //interactive console
            IScriptConsoleViewer v = (IScriptConsoleViewer) this.edit;
            IInterpreterInfo interpreterInfo = (IInterpreterInfo) v.getInterpreterInfo();
            if (interpreterInfo != null) {
                return interpreterInfo.getGrammarVersion();
            }

        }
        return IGrammarVersionProvider.LATEST_GRAMMAR_PY3_VERSION;
    }

    public String getModuleName() {
        if (this.edit instanceof IPySourceViewer) {
            try {
                IPySourceViewer pyViewer = (IPySourceViewer) this.edit;
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
     * @param edit the viewer for which the context is created
     * @param region the region into <code>document</code> for which the context is created
     * @return a template context that can handle template insertion at the given location, or <code>null</code>
     */
    public static PyDocumentTemplateContext createContext(final TemplateContextType contextType,
            final ISourceViewerForTemplates edit, final IRegion region, String indentTo) {
        if (contextType != null) {
            IDocument document = edit.getDocument();
            return new PyDocumentTemplateContext(contextType, document, region.getOffset(), region.getLength(),
                    indentTo, edit);
        }
        return null;
    }

    public static PyDocumentTemplateContext createContext(final TemplateContextType contextType,
            final ISourceViewerForTemplates edit, final IRegion region) {
        if (contextType != null) {
            IDocument document = edit.getDocument();
            ICoreTextSelection textSelection = edit.getTextSelection();
            PySelection selection = new PySelection(document, textSelection);
            String indent = selection.getIndentationFromLine();
            return PyDocumentTemplateContext.createContext(contextType, edit, region, indent);
        }
        return null;
    }

    public static PyDocumentTemplateContext createContextWithCursor(ISourceViewerForTemplates targetEditor,
            IRegion region, String indent) {
        TemplateContextType contextType = new TemplateContextType();
        contextType.addResolver(new GlobalTemplateVariables.Cursor()); //We do want the cursor thought.
        return createContext(contextType, targetEditor, region, indent);
    }

    public static PyDocumentTemplateContext createContextWithDefaultResolvers(ISourceViewerForTemplates edit,
            IRegion region, String indentTo) {
        TemplateContextType contextType = new TemplateContextType();
        PyAddTemplateResolvers.addDefaultResolvers(contextType);
        return createContext(contextType, edit, region, indentTo);
    }

}