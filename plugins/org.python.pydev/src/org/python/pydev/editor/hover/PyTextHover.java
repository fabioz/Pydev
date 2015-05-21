/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 02/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.hover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.python.pydev.core.ExtensionHelper;
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
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * Gets the default hover information and asks for clients to gather more info.
 * 
 * @author Fabio
 */
public class PyTextHover implements ITextHover, ITextHoverExtension {

    private final class PyInformationControl extends DefaultInformationControl
            implements IInformationControlExtension3 {
        private PyInformationControl(Shell parent, int textStyles, IInformationPresenter presenter,
                String statusFieldText) {
            super(parent, textStyles, presenter, statusFieldText);
        }

    }

    /**
     * Whether we're in a comment or multiline string.
     */
    private final boolean pythonCommentOrMultiline;

    /**
     * The text selected
     */
    private ITextSelection textSelection;

    /**
     * Constructor
     * 
     * @param sourceViewer the viewer for which the hover info should be gathered
     * @param contentType the type of the current content we're hovering over.
     */
    public PyTextHover(ISourceViewer sourceViewer, String contentType) {
        boolean pythonCommentOrMultiline = false;

        for (String type : IPythonPartitions.types) {
            if (type.equals(contentType)) {
                pythonCommentOrMultiline = true;
                break;
            }
        }
        this.pythonCommentOrMultiline = pythonCommentOrMultiline;
    }

    @SuppressWarnings("unchecked")
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        FastStringBuffer buf = new FastStringBuffer();

        if (!pythonCommentOrMultiline) {
            if (textViewer instanceof PySourceViewer) {
                PySourceViewer s = (PySourceViewer) textViewer;
                PySelection ps = new PySelection(s.getDocument(), hoverRegion.getOffset() + hoverRegion.getLength());

                List<IPyHoverParticipant> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_HOVER);
                for (IPyHoverParticipant pyHoverParticipant : participants) {
                    try {
                        String hoverText = pyHoverParticipant.getHoverText(hoverRegion, s, ps, textSelection);
                        if (hoverText != null && hoverText.trim().length() > 0) {
                            if (buf.length() > 0) {
                                buf.append(PyInformationPresenter.LINE_DELIM);
                            }
                            buf.append(hoverText);
                        }
                    } catch (Exception e) {
                        //clients should not make the hover fail!
                        Log.log(e);
                    }
                }

                getMarkerHover(hoverRegion, s, buf);
                if (PyHoverPreferencesPage.getShowDocstringOnHover()) {
                    getDocstringHover(hoverRegion, s, ps, buf);
                }

            }
        }
        return buf.toString();
    }

    /**
     * Fills the buffer with the text for markers we're hovering over.
     */
    private void getMarkerHover(IRegion hoverRegion, PySourceViewer s, FastStringBuffer buf) {
        for (Iterator<MarkerAnnotationAndPosition> it = s.getMarkerIterator(); it.hasNext();) {
            MarkerAnnotationAndPosition marker = it.next();
            try {
                if (marker.position == null) {
                    continue;
                }
                int cStart = marker.position.offset;
                int cEnd = cStart + marker.position.length;
                int offset = hoverRegion.getOffset();
                if (cStart <= offset && cEnd >= offset) {
                    if (buf.length() > 0) {
                        buf.append(PyInformationPresenter.LINE_DELIM);
                    }
                    Object msg = marker.markerAnnotation.getMarker().getAttribute(IMarker.MESSAGE);
                    if (!"PyDev breakpoint".equals(msg)) {
                        buf.appendObject(msg);
                    }
                }
            } catch (CoreException e) {
                //ignore marker does not exist anymore
            }
        }
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
     * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
     */
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        //we have to set it here (otherwise we don't have thread access to the UI)
        this.textSelection = (ITextSelection) textViewer.getSelectionProvider().getSelection();
        return new Region(offset, 0);
    }

    /*
     * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
     */
    public IInformationControlCreator getHoverControlCreator() {
        return new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                String tooltipAffordanceString = null;
                try {
                    tooltipAffordanceString = EditorsUI.getTooltipAffordanceString();
                } catch (Throwable e) {
                    //Not available on Eclipse 3.2
                }
                DefaultInformationControl ret = new PyInformationControl(parent, SWT.NONE,
                        new PyInformationPresenter(), tooltipAffordanceString);
                return ret;
            }
        };
    }
}
