/**
 * Copyright (c) 2016 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 *
 * A re-factor of <code>PyTextHover</code> to use the extension point <code>org.python.pydev.pyTextHover</code>
 */
package org.python.pydev.editor.hover;

import java.util.ArrayList;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.docutils.StringEscapeUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PyInformationPresenter;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;

public class PyDocstringTextHover extends AbstractPyEditorTextHover {

    public static String ID = "org.python.pydev.editor.hover.pyDocstringTextHover";

    @Override
    public String getHoverInfo(final ITextViewer textViewer, IRegion hoverRegion) {
        FastStringBuffer buf = new FastStringBuffer();

        if (textViewer instanceof PySourceViewer) {
            PySourceViewer s = (PySourceViewer) textViewer;
            PySelection ps = new PySelection(s.getDocument(), hoverRegion.getOffset() + hoverRegion.getLength());
            getDocstringHover(hoverRegion, s, ps, buf);
        }
        return buf.toString();
    }

    public PyDocstringTextHover() {
        super();
    }

    /*
     */
    @Override
    public boolean isContentTypeSupported(String contentType) {
        boolean pythonCommentOrMultiline = IPythonPartitions.NON_DEFAULT_TYPES_AS_SET.contains(contentType);

        if (!pythonCommentOrMultiline) {
            return true;
        }

        return false;
    }

    /**
     * Fills the buffer with the text for docstrings of the selected element.
     */
    @SuppressWarnings("unchecked")
    private void getDocstringHover(IRegion hoverRegion, PySourceViewer s, PySelection ps, FastStringBuffer buf) {
        //Now, aside from the marker, let's check if there's some definition we should show the user about.
        CompletionCache completionCache = new CompletionCache();
        ArrayList<IDefinition> selected = new ArrayList<IDefinition>();

        PyEdit edit = s.getEdit();
        RefactoringRequest request;
        IPythonNature nature = null;
        try {
            nature = edit.getPythonNature();
            request = new RefactoringRequest(edit.getEditorFile(), ps, new NullProgressMonitor(), nature, edit);
        } catch (MisconfigurationException e) {
            return;
        }
        String[] tokenAndQual = null;
        try {
            tokenAndQual = PyRefactoringFindDefinition.findActualDefinition(request, completionCache, selected);
        } catch (CompletionRecursionException | BadLocationException e1) {
            Log.log(e1);
            buf.append("Unable to compute hover. Details: " + e1.getMessage());
            return;
        }

        FastStringBuffer temp = new FastStringBuffer();

        if (tokenAndQual != null && selected.size() > 0) {
            for (IDefinition d : selected) {
                Definition def = (Definition) d;

                SimpleNode astToPrint = null;
                if (def.ast != null) {
                    astToPrint = def.ast;
                    if ((astToPrint instanceof Name || astToPrint instanceof NameTok) && def.scope != null) {
                        //There's no real point in just printing the name, let's see if we're able to actually find
                        //the scope where it's in and print that scope.
                        FastStack<SimpleNode> scopeStack = def.scope.getScopeStack();
                        if (scopeStack != null && scopeStack.size() > 0) {
                            SimpleNode peek = scopeStack.peek();
                            if (peek != null) {
                                stmtType stmt = NodeUtils.findStmtForNode(peek, astToPrint);
                                if (stmt != null) {
                                    astToPrint = stmt;
                                }
                            }
                        }
                    }
                    try {
                        astToPrint = astToPrint.createCopy();
                        MakeAstValidForPrettyPrintingVisitor.makeValid(astToPrint);
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }

                temp = temp.clear();
                if (def.value != null) {
                    if (astToPrint instanceof FunctionDef) {
                        temp.append("def ");

                    } else if (astToPrint instanceof ClassDef) {
                        temp.append("class ");

                    }
                    temp.append("<pydev_hint_bold>");
                    temp.append(def.value);
                    temp.append("</pydev_hint_bold>");
                    temp.append(' ');
                }

                if (def.module != null) {
                    temp.append("Found at: ");
                    temp.append("<pydev_hint_bold>");
                    temp.append(def.module.getName());
                    temp.append("</pydev_hint_bold>");
                    temp.append(PyInformationPresenter.LINE_DELIM);
                }

                if (def.module != null && def.value != null) {
                    ItemPointer pointer = PyRefactoringFindDefinition.createItemPointer(def);
                    String asPortableString = pointer.asPortableString();
                    if (asPortableString != null) {
                        //may happen if file is not in the pythonpath
                        temp.replaceAll(
                                "<pydev_hint_bold>",
                                StringUtils.format("<pydev_link pointer=\"%s\">",
                                        StringEscapeUtils.escapeXml(asPortableString)));
                        temp.replaceAll("</pydev_hint_bold>", "</pydev_link>");
                    }
                }

                String str = printAst(edit, astToPrint);

                if (str != null && str.trim().length() > 0) {
                    temp.append(PyInformationPresenter.LINE_DELIM);
                    temp.append(str);

                } else {
                    String docstring = d.getDocstring(nature, completionCache);
                    if (docstring != null && docstring.trim().length() > 0) {
                        IIndentPrefs indentPrefs = edit.getIndentPrefs();
                        temp.append(PyStringUtils.fixWhitespaceColumnsToLeftFromDocstring(docstring,
                                indentPrefs.getIndentationString()));
                    }
                }

                if (temp.length() > 0) {
                    if (buf.length() > 0) {
                        buf.append(PyInformationPresenter.LINE_DELIM);
                    }
                    buf.append(temp);
                }
            }
        }
    }

}
