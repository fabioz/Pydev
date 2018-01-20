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
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleViewer;

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
    /*default*/PyDocumentTemplateContext(TemplateContextType type, IDocument document, int offset, int length,
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
        if (this.viewer instanceof PySourceViewer) {
            return ((PySourceViewer) this.viewer).getEdit().isCythonFile();
        }
        return false;
    }

    public File getEditorFile() {
        if (this.viewer instanceof PySourceViewer) {
            return ((PySourceViewer) this.viewer).getEdit().getEditorFile();
        }
        return new File("");
    }

    public int getGrammarVersion() {
        //Other possibilities
        //org.eclipse.jface.text.source.SourceViewer (in compare)

        if (this.viewer instanceof PySourceViewer) {
            try {
                IPythonNature nature = ((PySourceViewer) this.viewer).getEdit().getPythonNature();
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
        return IGrammarVersionProvider.LATEST_GRAMMAR_VERSION;
    }

    public String getModuleName() {
        if (this.viewer instanceof PySourceViewer) {
            try {
                PySourceViewer pyViewer = (PySourceViewer) this.viewer;
                PyEdit edit = pyViewer.getEdit();
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
     * @return the indent preferences to be used.
     */
    private static IIndentPrefs getIndentPrefs(ITextViewer viewer) {
        if (viewer instanceof PySourceViewer) {
            PySourceViewer pyViewer = (PySourceViewer) viewer;
            return pyViewer.getEdit().getIndentPrefs();
        } else {
            return DefaultIndentPrefs.get(null);
        }
    }

}